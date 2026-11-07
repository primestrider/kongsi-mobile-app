package com.primestrider.kongsi.feature.bill.ui.people

import android.database.sqlite.SQLiteException
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.primestrider.kongsi.core.common.ApplicationScope
import com.primestrider.kongsi.core.datastore.UserProfileRepository
import com.primestrider.kongsi.core.designsystem.component.Messenger
import com.primestrider.kongsi.core.designsystem.component.UiText
import com.primestrider.kongsi.core.util.IndonesianPhone
import com.primestrider.kongsi.feature.bill.contacts.ContactMatch
import com.primestrider.kongsi.feature.bill.contacts.ContactsRepository
import com.primestrider.kongsi.feature.bill.data.BillRepository
import com.primestrider.kongsi.feature.bill.data.FrequentPeopleRepository
import com.primestrider.kongsi.feature.bill.data.FrequentPerson
import com.primestrider.kongsi.feature.bill.impl.R
import com.primestrider.kongsi.feature.bill.model.Bill
import com.primestrider.kongsi.feature.bill.model.Person
import com.primestrider.kongsi.feature.bill.model.SplitCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/** Someone on the bill, with their tone [index] and a [label] that tells apart people with the same name. */
data class PersonRow(
    val person: Person,
    val index: Int,
    val label: String,
    val isMe: Boolean,
)

/** What the search offers to add. */
sealed interface Suggestion {
    /** Someone from an earlier bill. */
    data class Frequent(
        val person: FrequentPerson,
    ) : Suggestion

    /** A number from the address book. */
    data class Contact(
        val match: ContactMatch,
    ) : Suggestion

    /** The typed text as a name, without a number. */
    data class Typed(
        val name: String,
    ) : Suggestion
}

data class BillPeopleUiState(
    val loaded: Boolean = false,
    val title: String = "",
    /** The payer first. */
    val people: List<PersonRow> = emptyList(),
    /** Remembered people first, then contacts, then the typed name. */
    val suggestions: List<Suggestion> = emptyList(),
    val totalMinor: Long = 0,
    val receiptMinor: Long? = null,
    /** True once someone besides the payer is on the bill. */
    val canContinue: Boolean = false,
)

/**
 * People: who is on the bill. The payer ("me", from the profile) is added once and stays. Others come from the
 * address book (with READ_CONTACTS), the system contact picker (without it), people from earlier bills, or a typed
 * name. A number already on the bill is not added again; the same name twice is allowed and told apart.
 */
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class BillPeopleViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val bills: BillRepository,
        private val contacts: ContactsRepository,
        private val frequent: FrequentPeopleRepository,
        private val profiles: UserProfileRepository,
        private val messenger: Messenger,
        @ApplicationScope private val appScope: CoroutineScope,
    ) : ViewModel() {
        private val billId = savedStateHandle.get<Long>(BILL_ID) ?: 0L

        /** The search text, as Compose state so the field never gets a stale value while typing. */
        var query by mutableStateOf("")
            private set
        private val queryFlow = MutableStateFlow("")
        private val contactsAllowed = MutableStateFlow(false)
        private val writes = Mutex()

        private val bill = bills.observe(billId)

        private val suggestions =
            combine(queryFlow, contactsAllowed, bill) { text, allowed, stored -> Triple(text, allowed, stored?.people.orEmpty()) }
                .mapLatest { (text, allowed, people) -> suggestionsFor(text, allowed, people) }

        val state: StateFlow<BillPeopleUiState> =
            combine(bill, suggestions) { stored, offered ->
                if (stored == null) return@combine BillPeopleUiState(loaded = true)
                val result = SplitCalculator.split(stored)
                BillPeopleUiState(
                    loaded = true,
                    title = stored.title,
                    people = rowsOf(stored),
                    suggestions = offered,
                    totalMinor = result.totalMinor,
                    receiptMinor = stored.receiptTotalMinor,
                    canContinue = stored.people.size >= 2,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BillPeopleUiState())

        init {
            viewModelScope.launch { addPayerOnce() }
        }

        fun onQueryChange(text: String) {
            query = text
            queryFlow.value = text
        }

        /** Whether the address book may be searched; the screen asks for the permission and reports the answer. */
        fun onContactsAllowed(allowed: Boolean) {
            contactsAllowed.value = allowed
        }

        fun add(suggestion: Suggestion) {
            val person =
                when (suggestion) {
                    is Suggestion.Frequent -> Person(0, suggestion.person.name, suggestion.person.phone)
                    is Suggestion.Contact -> Person(0, suggestion.match.name, suggestion.match.international ?: suggestion.match.phone)
                    is Suggestion.Typed -> Person(0, suggestion.name.trim(), null)
                }
            if (person.name.isBlank()) return
            onQueryChange("")
            change { stored ->
                val number = person.phone?.let(IndonesianPhone::international)
                val existing = number?.let { n -> stored.people.firstOrNull { it.phone?.let(IndonesianPhone::international) == n } }
                if (existing != null) {
                    val name = if (existing.id == stored.payerPersonId) existing.name.ifBlank { person.name } else existing.name
                    messenger.info(UiText.Resource(R.string.bill_people_already_added, listOf(name)))
                    null
                } else {
                    stored.copy(people = stored.people + person)
                }
            }
        }

        /** The number the system contact picker returned; null when the user backed out. */
        fun onPickerResult(uri: Uri?) {
            uri ?: return
            viewModelScope.launch { contacts.fromPickerResult(uri)?.let { add(Suggestion.Contact(it)) } }
        }

        /** Removes someone and their part in the items they shared. The payer stays. */
        fun remove(personId: Long) {
            change { stored ->
                if (personId == stored.payerPersonId) return@change null
                stored.copy(
                    people = stored.people.filterNot { it.id == personId },
                    items = stored.items.map { it.copy(sharedBy = it.sharedBy - personId) },
                )
            }
        }

        /** Leaving for the next step: remembers everyone but the payer for next time (also after the screen is gone). */
        fun next() {
            appScope.launch {
                val stored = bills.get(billId) ?: return@launch
                frequent.remember(stored.people.filterNot { it.id == stored.payerPersonId })
            }
        }

        private suspend fun addPayerOnce() {
            val profile = profiles.profile.first()
            save { stored ->
                if (stored.payerPersonId != null) return@save null
                val me = Person(0, profile.name.orEmpty(), profile.phone?.let(IndonesianPhone::international))
                stored.copy(people = listOf(me) + stored.people)
            }
            // The new person's id is only known once saved.
            save { stored -> if (stored.payerPersonId == null) stored.copy(payerPersonId = stored.people.firstOrNull()?.id) else null }
        }

        private suspend fun suggestionsFor(
            text: String,
            allowed: Boolean,
            people: List<Person>,
        ): List<Suggestion> {
            val numbers = people.mapNotNull { it.phone?.let(IndonesianPhone::international) }.toSet()
            val names = people.filter { it.phone == null }.map { it.name.trim().lowercase() }.toSet()
            val remembered =
                frequent.suggestions(text).filterNot { f ->
                    if (f.phone != null) f.phone in numbers else f.name.trim().lowercase() in names
                }
            val rememberedNumbers = remembered.mapNotNull { it.phone }.toSet()
            val found =
                if (allowed && text.isNotBlank()) {
                    contacts.search(text).filterNot {
                        it.international != null &&
                            (it.international in numbers || it.international in rememberedNumbers)
                    }
                } else {
                    emptyList()
                }
            val typed =
                text
                    .trim()
                    .takeIf { it.isNotEmpty() }
                    ?.let { listOf(Suggestion.Typed(it)) }
                    .orEmpty()
            return remembered.map { Suggestion.Frequent(it) } + found.map { Suggestion.Contact(it) } + typed
        }

        /** Applies [update] to the stored bill and saves it; an update returning null changes nothing. */
        private fun change(update: suspend (Bill) -> Bill?) {
            viewModelScope.launch { save(update) }
        }

        private suspend fun save(update: suspend (Bill) -> Bill?) {
            writes.withLock {
                try {
                    val stored = bills.get(billId) ?: return@withLock
                    val updated = update(stored) ?: return@withLock
                    bills.save(updated)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: SQLiteException) {
                    messenger.error(UiText.Resource(R.string.bill_save_failed))
                }
            }
        }

        companion object {
            const val BILL_ID = "billId"
        }
    }

/** The people of [bill], payer first, with labels that tell apart people with the same name. */
internal fun rowsOf(bill: Bill): List<PersonRow> {
    val ordered = bill.people.sortedByDescending { it.id == bill.payerPersonId }
    val labels = personLabels(ordered)
    return ordered.mapIndexed { index, person ->
        PersonRow(person, index = bill.people.indexOf(person), label = labels[index], isMe = person.id == bill.payerPersonId)
    }
}

/**
 * Names as shown: the first person with a name keeps it plain; later ones with the same name (ignoring case) get the
 * last four digits of their number ("Budi · 7890"), or a count when they have none ("Budi 2").
 */
internal fun personLabels(people: List<Person>): List<String> {
    val seen = mutableMapOf<String, MutableList<Person>>()
    return people.map { person ->
        val key = person.name.trim().lowercase()
        val group = seen.getOrPut(key) { mutableListOf() }
        val label =
            when {
                group.isEmpty() -> person.name
                person.phone != null -> "${person.name} · ${person.phone.filter(Char::isDigit).takeLast(4)}"
                else -> {
                    val withoutNumber = group.count { it.phone == null } + 1
                    "${person.name} ${withoutNumber + if (group.first().phone != null) 1 else 0}"
                }
            }
        group += person
        label
    }
}
