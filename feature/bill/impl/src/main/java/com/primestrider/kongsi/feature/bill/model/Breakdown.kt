package com.primestrider.kongsi.feature.bill.model

import kotlin.math.abs

/**
 * One line of "How was this calculated?" for a person, shared by the Summary screen and the WhatsApp message so both
 * explain a total the same way (UX Principles, points 3 and 9).
 */
sealed interface BreakdownLine {
    val amountMinor: Long

    /** The person's part of an item; [sharedWith] are the ids of the others who had it (payer first), [sharers] everyone. */
    data class Item(
        val name: String,
        val quantity: Int,
        val sharedWith: List<Long>,
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

/** Rounding is only worth a line when it changes the total by more than this (UX Principles, point 3). */
private const val ROUNDING_SHOWN_ABOVE = 1L

/** [share] as breakdown lines: each item they had, then their service, tax, discount and any rounding above Rp1. */
fun breakdownOf(
    bill: Bill,
    share: PersonShare,
): List<BreakdownLine> {
    val items = bill.items.associateBy { it.id }
    val people = bill.people.sortedByDescending { it.id == bill.payerPersonId }.map { it.id }
    val lines: MutableList<BreakdownLine> =
        share.items
            .map { part ->
                val item = items.getValue(part.itemId)
                val others = people.filter { it != share.personId && it in item.sharedBy }
                BreakdownLine.Item(item.name, item.quantity, others, part.sharers, part.amountMinor)
            }.toMutableList()
    if (share.serviceMinor != 0L) lines += BreakdownLine.Service(bill.service.rateBps, share.serviceMinor)
    if (share.taxMinor != 0L) lines += BreakdownLine.Tax(bill.tax.rateBps, onService = share.serviceMinor != 0L, share.taxMinor)
    if (share.discountMinor != 0L) lines += BreakdownLine.Discount(share.discountMinor)
    if (abs(share.roundingMinor) > ROUNDING_SHOWN_ABOVE) lines += BreakdownLine.Rounding(share.roundingMinor)
    return lines
}

/** One person's part of an item shared by [sharers]: "½", "⅓", … "⅒", then "1/11". */
fun fractionText(sharers: Int): String = VULGAR_FRACTIONS.getOrNull(sharers - 2) ?: "1/$sharers"

private val VULGAR_FRACTIONS = listOf("½", "⅓", "¼", "⅕", "⅙", "⅐", "⅛", "⅑", "⅒")
