package com.primestrider.kongsi.feature.bill.ui.home

import android.database.sqlite.SQLiteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.primestrider.kongsi.core.common.ApplicationScope
import com.primestrider.kongsi.core.designsystem.component.MessageVariant
import com.primestrider.kongsi.core.designsystem.component.Messenger
import com.primestrider.kongsi.core.designsystem.component.UiMessage
import com.primestrider.kongsi.core.designsystem.component.UiText
import com.primestrider.kongsi.feature.bill.data.BillRepository
import com.primestrider.kongsi.feature.bill.data.PaymentAccountRepository
import com.primestrider.kongsi.feature.bill.impl.R
import com.primestrider.kongsi.feature.bill.model.Bill
import com.primestrider.kongsi.feature.bill.model.SplitCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * One line of the bill history. [title] is empty for a bill the user did not name. [owingCount] is everyone but the
 * payer, [paidCount] those of them marked as paid.
 */
data class BillRow(
    val id: Long,
    val title: String,
    val createdAt: Long,
    val totalMinor: Long,
    val peopleCount: Int,
    val owingCount: Int = 0,
    val paidCount: Int = 0,
)

data class BillHomeUiState(
    /** True once the database has answered and no payment account is saved yet. */
    val showAccountHint: Boolean = false,
    /** Newest first. */
    val bills: List<BillRow> = emptyList(),
    /** False until the database has answered once, so the empty state does not flash on open. */
    val loaded: Boolean = false,
)

/**
 * Home: the bill history and the payment-account hint. A bill can be renamed, duplicated (for a bill that repeats) or
 * deleted. Delete happens at once and offers Undo; the undo runs in [appScope], so it still works after the user has
 * left the screen while the message is showing.
 */
@HiltViewModel
class BillHomeViewModel
    @Inject
    constructor(
        accounts: PaymentAccountRepository,
        private val bills: BillRepository,
        private val messenger: Messenger,
        @ApplicationScope private val appScope: CoroutineScope,
    ) : ViewModel() {
        val state: StateFlow<BillHomeUiState> =
            combine(accounts.accounts, bills.bills) { savedAccounts, savedBills ->
                BillHomeUiState(
                    showAccountHint = savedAccounts.isEmpty(),
                    bills = savedBills.map { it.toRow() },
                    loaded = true,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BillHomeUiState())

        fun delete(id: Long) {
            viewModelScope.launch {
                val removed =
                    try {
                        bills.delete(id)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: SQLiteException) {
                        messenger.error(UiText.Resource(R.string.bill_home_delete_failed))
                        null
                    } ?: return@launch
                messenger.show(
                    UiMessage(
                        text =
                            if (removed.title.isBlank()) {
                                UiText.Resource(R.string.bill_home_deleted_untitled)
                            } else {
                                UiText.Resource(R.string.bill_home_deleted, listOf(removed.title))
                            },
                        variant = MessageVariant.INFO,
                        actionLabel = UiText.Resource(R.string.bill_undo),
                        onAction = { appScope.launch { restore(removed) } },
                    ),
                )
            }
        }

        /** Gives bill [id] the name [title] (trimmed; blank leaves it unnamed). */
        fun rename(
            id: Long,
            title: String,
        ) {
            viewModelScope.launch {
                write {
                    val bill = bills.get(id) ?: return@write
                    bills.save(bill.copy(title = title.trim()))
                }
            }
        }

        /** Saves a copy of bill [id] and hands its id to [onCopied], to open it. */
        fun duplicate(
            id: Long,
            onCopied: (Long) -> Unit,
        ) {
            viewModelScope.launch { write { bills.duplicate(id)?.let(onCopied) } }
        }

        private suspend fun write(block: suspend () -> Unit) {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: SQLiteException) {
                messenger.error(UiText.Resource(R.string.bill_save_failed))
            }
        }

        private suspend fun restore(bill: Bill) {
            try {
                bills.save(bill)
            } catch (e: SQLiteException) {
                messenger.error(UiText.Resource(R.string.bill_home_delete_failed))
            }
        }
    }

private fun Bill.toRow() =
    BillRow(
        id = id,
        title = title,
        createdAt = createdAt,
        totalMinor = SplitCalculator.split(this).totalMinor,
        peopleCount = people.size,
        owingCount = people.count { it.id != payerPersonId },
        paidCount = people.count { it.id != payerPersonId && it.paid },
    )
