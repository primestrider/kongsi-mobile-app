package com.primestrider.kongsi.feature.bill.ui.summary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.primestrider.kongsi.core.common.ApplicationScope
import com.primestrider.kongsi.feature.bill.data.BillRepository
import com.primestrider.kongsi.feature.bill.data.FrequentPeopleRepository
import com.primestrider.kongsi.feature.bill.model.Bill
import com.primestrider.kongsi.feature.bill.model.PersonShare
import com.primestrider.kongsi.feature.bill.model.SplitCalculator
import com.primestrider.kongsi.feature.bill.ui.people.PersonRow
import com.primestrider.kongsi.feature.bill.ui.people.rowsOf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

/** One line of "How was this calculated?", in plain words on the screen. */
sealed interface BreakdownLine {
    val amountMinor: Long

    /** The person's part of an item; [sharedWith] are the others who had it, [sharers] everyone who did. */
    data class Item(
        val name: String,
        val quantity: Int,
        val sharedWith: List<PersonRow>,
        val sharers: Int,
        override val amountMinor: Long,
    ) : BreakdownLine

    /** Service on the person's items; [rateBps] when the charge was entered as a rate. */
    data class Service(
        val rateBps: Int?,
        override val amountMinor: Long,
    ) : BreakdownLine

    /** Tax on the person's items, and on their service when they had some ([onService]). */
    data class Tax(
        val rateBps: Int?,
        val onService: Boolean,
        override val amountMinor: Long,
    ) : BreakdownLine

    /** The person's part of a bill-level discount, as a positive amount taken off. */
    data class Discount(
        override val amountMinor: Long,
    ) : BreakdownLine

    /** Rounding added (positive) or taken off (negative), only when it is more than Rp1. */
    data class Rounding(
        override val amountMinor: Long,
    ) : BreakdownLine
}

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
)

/**
 * Summary: what each person owes, the payer's own share marked and not requested, and for everyone a breakdown in
 * plain words. "Done" remembers the people for the next bill.
 */
@HiltViewModel
class BillSummaryViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val bills: BillRepository,
        private val frequent: FrequentPeopleRepository,
        @ApplicationScope private val appScope: CoroutineScope,
    ) : ViewModel() {
        private val billId = savedStateHandle.get<Long>(BILL_ID) ?: 0L

        val state: StateFlow<BillSummaryUiState> =
            combine(bills.observe(billId), savedStateHandle.getStateFlow<Long?>(OPEN_PERSON, null)) { stored, open ->
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
                            SummaryPerson(row, share.totalMinor, breakdownOf(stored, share, rows))
                        },
                    totalMinor = result.totalMinor,
                    assignedMinor = result.assignedMinor,
                    complete = result.isComplete,
                    openPersonId = open?.takeIf { id -> rows.any { it.person.id == id } },
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BillSummaryUiState())

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

        companion object {
            const val BILL_ID = "billId"
            private const val OPEN_PERSON = "openPerson"
        }
    }

/** Rounding is only worth a line when it changes the total by more than this (UX Principles, point 3). */
private const val ROUNDING_SHOWN_ABOVE = 1L

/** [share] as breakdown lines: each item they had, then their service, tax, discount and any rounding above Rp1. */
internal fun breakdownOf(
    bill: Bill,
    share: PersonShare,
    rows: List<PersonRow>,
): List<BreakdownLine> {
    val items = bill.items.associateBy { it.id }
    val lines: MutableList<BreakdownLine> =
        share.items
            .map { part ->
                val item = items.getValue(part.itemId)
                val others = rows.filter { it.person.id != share.personId && it.person.id in item.sharedBy }
                BreakdownLine.Item(item.name, item.quantity, others, part.sharers, part.amountMinor)
            }.toMutableList()
    if (share.serviceMinor != 0L) lines += BreakdownLine.Service(bill.service.rateBps, share.serviceMinor)
    if (share.taxMinor != 0L) lines += BreakdownLine.Tax(bill.tax.rateBps, onService = share.serviceMinor != 0L, share.taxMinor)
    if (share.discountMinor != 0L) lines += BreakdownLine.Discount(share.discountMinor)
    if (abs(share.roundingMinor) > ROUNDING_SHOWN_ABOVE) lines += BreakdownLine.Rounding(share.roundingMinor)
    return lines
}
