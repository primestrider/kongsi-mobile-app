package com.primestrider.kongsi.feature.bill.ui.assign

import android.database.sqlite.SQLiteException
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.primestrider.kongsi.core.common.ApplicationScope
import com.primestrider.kongsi.core.designsystem.component.MessageVariant
import com.primestrider.kongsi.core.designsystem.component.Messenger
import com.primestrider.kongsi.core.designsystem.component.UiMessage
import com.primestrider.kongsi.core.designsystem.component.UiText
import com.primestrider.kongsi.feature.bill.data.BillRepository
import com.primestrider.kongsi.feature.bill.impl.R
import com.primestrider.kongsi.feature.bill.model.Bill
import com.primestrider.kongsi.feature.bill.model.BillItem
import com.primestrider.kongsi.feature.bill.model.SplitCalculator
import com.primestrider.kongsi.feature.bill.model.SplitIssue
import com.primestrider.kongsi.feature.bill.ui.people.PersonRow
import com.primestrider.kongsi.feature.bill.ui.people.rowsOf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/** Item-first (tap an item, tick people) or person-first (pick a person, tick their items). */
enum class AssignMode { BY_ITEM, BY_PERSON }

/** An item with the people who had it, in the order of [BillAssignUiState.people]. */
data class AssignItemRow(
    val item: BillItem,
    val sharers: List<PersonRow>,
)

data class BillAssignUiState(
    val loaded: Boolean = false,
    val title: String = "",
    /** The payer first. */
    val people: List<PersonRow> = emptyList(),
    val items: List<AssignItemRow> = emptyList(),
    val mode: AssignMode = AssignMode.BY_ITEM,
    /** The person whose items are ticked in [AssignMode.BY_PERSON]. */
    val selectedPersonId: Long? = null,
    val assignedMinor: Long = 0,
    val totalMinor: Long = 0,
    /** Items nobody had yet, in receipt order; they keep the user off the Summary step. */
    val unassignedItemIds: List<Long> = emptyList(),
    val canContinue: Boolean = false,
    /** Set by tapping "not assigned yet"; the screen scrolls to the item, highlights it, then calls `onFocusShown`. */
    val focusItemId: Long? = null,
)

/**
 * Assign: who had which item. Both modes change the same thing, the people sharing an item, and every change is saved
 * at once. "Split whole bill equally" gives every item to everyone and can be undone.
 */
@HiltViewModel
class BillAssignViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val bills: BillRepository,
        private val messenger: Messenger,
        @ApplicationScope private val appScope: CoroutineScope,
    ) : ViewModel() {
        private val billId = savedStateHandle.get<Long>(BILL_ID) ?: 0L
        private val mode = savedStateHandle.getStateFlow(MODE, AssignMode.BY_ITEM)
        private val selectedPerson = savedStateHandle.getStateFlow<Long?>(SELECTED_PERSON, null)
        private val focus = MutableStateFlow<Long?>(null)
        private val writes = Mutex()

        val state: StateFlow<BillAssignUiState> =
            combine(bills.observe(billId), mode, selectedPerson, focus) { stored, mode, selected, focused ->
                if (stored == null) return@combine BillAssignUiState(loaded = true)
                val result = SplitCalculator.split(stored)
                val people = rowsOf(stored)
                val unassigned =
                    result.issues
                        .filterIsInstance<SplitIssue.UnassignedItems>()
                        .firstOrNull()
                        ?.itemIds
                        .orEmpty()
                BillAssignUiState(
                    loaded = true,
                    title = stored.title,
                    people = people,
                    items = stored.items.map { item -> AssignItemRow(item, people.filter { it.person.id in item.sharedBy }) },
                    mode = mode,
                    selectedPersonId = selected?.takeIf { id -> people.any { it.person.id == id } } ?: people.firstOrNull()?.person?.id,
                    assignedMinor = result.assignedMinor,
                    totalMinor = result.totalMinor,
                    unassignedItemIds = unassigned,
                    canContinue = stored.items.isNotEmpty() && result.isComplete,
                    focusItemId = focused,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BillAssignUiState())

        fun onModeChange(mode: AssignMode) {
            savedStateHandle[MODE] = mode
        }

        fun onSelectPerson(personId: Long) {
            savedStateHandle[SELECTED_PERSON] = personId
        }

        /** Adds [personId] to the people who had [itemId], or takes them off it. */
        fun toggle(
            itemId: Long,
            personId: Long,
        ) = change { stored ->
            if (stored.people.none { it.id == personId }) return@change null
            stored.withSharers(itemId) { sharers -> if (personId in sharers) sharers - personId else sharers + personId }
        }

        /** Gives [itemId] to everyone; when everyone already had it, to no one, so the shortcut also undoes itself. */
        fun toggleEveryone(itemId: Long) =
            change { stored ->
                val everyone = stored.people.map { it.id }.toSet()
                stored.withSharers(itemId) { sharers -> if (sharers.containsAll(everyone)) emptySet() else everyone }
            }

        /** Every item shared by everyone, with Undo to bring back what was ticked before. */
        fun splitEqually() {
            viewModelScope.launch {
                var before: Map<Long, Set<Long>> = emptyMap()
                val changed =
                    save { stored ->
                        val everyone = stored.people.map { it.id }.toSet()
                        if (stored.items.all { it.sharedBy == everyone }) return@save null
                        before = stored.items.associate { it.id to it.sharedBy }
                        stored.copy(items = stored.items.map { it.copy(sharedBy = everyone) })
                    }
                if (!changed) return@launch
                messenger.show(
                    UiMessage(
                        text = UiText.Resource(R.string.bill_assign_split_done),
                        variant = MessageVariant.INFO,
                        actionLabel = UiText.Resource(R.string.bill_undo),
                        // Undo also works after leaving the screen.
                        onAction = {
                            appScope.launch {
                                save { stored ->
                                    // People removed since then are left out.
                                    val known = stored.people.map { it.id }.toSet()
                                    stored.copy(
                                        items = stored.items.map { it.copy(sharedBy = (before[it.id] ?: it.sharedBy) intersect known) },
                                    )
                                }
                            }
                        },
                    ),
                )
            }
        }

        /** "2 items not assigned yet. Tap to assign": shows the first such item in the item-first list. */
        fun onUnassignedClick() {
            val first = state.value.unassignedItemIds.firstOrNull() ?: return
            onModeChange(AssignMode.BY_ITEM)
            focus.value = first
        }

        fun onFocusShown() {
            focus.value = null
        }

        private fun Bill.withSharers(
            itemId: Long,
            update: (Set<Long>) -> Set<Long>,
        ): Bill? {
            if (items.none { it.id == itemId }) return null
            return copy(items = items.map { if (it.id == itemId) it.copy(sharedBy = update(it.sharedBy)) else it })
        }

        /** Applies [update] to the stored bill and saves it; an update returning null changes nothing. */
        private fun change(update: (Bill) -> Bill?) {
            viewModelScope.launch { save(update) }
        }

        /** Returns whether something was saved. */
        private suspend fun save(update: (Bill) -> Bill?): Boolean =
            writes.withLock {
                try {
                    val stored = bills.get(billId) ?: return@withLock false
                    val updated = update(stored) ?: return@withLock false
                    bills.save(updated)
                    true
                } catch (e: CancellationException) {
                    throw e
                } catch (e: SQLiteException) {
                    messenger.error(UiText.Resource(R.string.bill_save_failed))
                    false
                }
            }

        companion object {
            const val BILL_ID = "billId"
            private const val MODE = "mode"
            private const val SELECTED_PERSON = "selectedPerson"
        }
    }
