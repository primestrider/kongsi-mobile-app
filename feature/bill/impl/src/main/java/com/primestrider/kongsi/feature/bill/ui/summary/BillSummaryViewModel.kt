package com.primestrider.kongsi.feature.bill.ui.summary

import android.database.sqlite.SQLiteException
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.primestrider.kongsi.core.common.ApplicationScope
import com.primestrider.kongsi.core.designsystem.component.Messenger
import com.primestrider.kongsi.core.designsystem.component.UiText
import com.primestrider.kongsi.feature.bill.data.BillRepository
import com.primestrider.kongsi.feature.bill.data.FrequentPeopleRepository
import com.primestrider.kongsi.feature.bill.data.PaymentAccountRepository
import com.primestrider.kongsi.feature.bill.impl.R
import com.primestrider.kongsi.feature.bill.model.Bill
import com.primestrider.kongsi.feature.bill.model.BreakdownLine
import com.primestrider.kongsi.feature.bill.model.PaymentAccount
import com.primestrider.kongsi.feature.bill.model.SplitCalculator
import com.primestrider.kongsi.feature.bill.model.breakdownOf
import com.primestrider.kongsi.feature.bill.ui.people.PersonRow
import com.primestrider.kongsi.feature.bill.ui.people.rowsOf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/** A person on the Summary: what they owe and how it was worked out. */
data class SummaryPerson(
    val row: PersonRow,
    val totalMinor: Long,
    val lines: List<BreakdownLine>,
)

data class BillSummaryUiState(
    val loaded: Boolean = false,
    val title: String = "",
    /** The payer first. */
    val people: List<SummaryPerson> = emptyList(),
    val totalMinor: Long = 0,
    val assignedMinor: Long = 0,
    /** Every item has someone and every charge an item to go on, so the shares add up to [totalMinor]. */
    val complete: Boolean = false,
    /** Whose "How was this calculated?" sheet is open. */
    val openPersonId: Long? = null,
    /** Every saved account, in the user's order; the message lists the [selectedAccountIds] ones. */
    val accounts: List<PaymentAccount> = emptyList(),
    val selectedAccountIds: Set<Long> = emptySet(),
)

/**
 * Summary: what each person owes, the payer's own share marked and not requested, and for everyone a breakdown in
 * plain words, and the payment accounts friends transfer to. A bill starts with the default accounts, applied once (as
 * soon as there is an account); after that the choice is the bill's own and saved with it. "Done" remembers the people
 * for the next bill.
 */
@HiltViewModel
class BillSummaryViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val bills: BillRepository,
        private val frequent: FrequentPeopleRepository,
        private val paymentAccounts: PaymentAccountRepository,
        private val messenger: Messenger,
        @ApplicationScope private val appScope: CoroutineScope,
    ) : ViewModel() {
        private val billId = savedStateHandle.get<Long>(BILL_ID) ?: 0L
        private val writes = Mutex()

        val state: StateFlow<BillSummaryUiState> =
            combine(
                bills.observe(billId),
                savedStateHandle.getStateFlow<Long?>(OPEN_PERSON, null),
                paymentAccounts.accounts,
            ) { stored, open, accounts ->
                if (stored == null) return@combine BillSummaryUiState(loaded = true)
                val result = SplitCalculator.split(stored)
                val rows = rowsOf(stored)
                val shares = result.shares.associateBy { it.personId }
                BillSummaryUiState(
                    loaded = true,
                    title = stored.title,
                    people =
                        rows.map { row ->
                            val share = shares.getValue(row.person.id)
                            SummaryPerson(row, share.totalMinor, breakdownOf(stored, share))
                        },
                    totalMinor = result.totalMinor,
                    assignedMinor = result.assignedMinor,
                    complete = result.isComplete,
                    openPersonId = open?.takeIf { id -> rows.any { it.person.id == id } },
                    accounts = accounts,
                    selectedAccountIds = stored.paymentAccountIds ?: accounts.defaultIds(),
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BillSummaryUiState())

        init {
            viewModelScope.launch {
                // Waits for the first account, so one added from here ("Add account") is picked up too.
                val accounts = paymentAccounts.accounts.first { it.isNotEmpty() }
                save { stored -> if (stored.paymentAccountIds == null) stored.copy(paymentAccountIds = accounts.defaultIds()) else null }
            }
        }

        /** Adds the account to this bill's message, or takes it off. */
        fun toggleAccount(accountId: Long) {
            viewModelScope.launch {
                save { stored ->
                    val selected = stored.paymentAccountIds ?: paymentAccounts.accounts.first().defaultIds()
                    stored.copy(paymentAccountIds = if (accountId in selected) selected - accountId else selected + accountId)
                }
            }
        }

        fun onOpenBreakdown(personId: Long) {
            savedStateHandle[OPEN_PERSON] = personId
        }

        fun onCloseBreakdown() {
            savedStateHandle[OPEN_PERSON] = null
        }

        /** The bill is done: remembers everyone but the payer for next time (also after the screen is gone). */
        fun finish() {
            appScope.launch {
                val stored = bills.get(billId) ?: return@launch
                frequent.remember(stored.people.filterNot { it.id == stored.payerPersonId })
            }
        }

        /** Applies [update] to the stored bill and saves it; an update returning null changes nothing. */
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
            private const val OPEN_PERSON = "openPerson"
        }
    }

private fun List<PaymentAccount>.defaultIds(): Set<Long> = filter { it.isDefault }.map { it.id }.toSet()
