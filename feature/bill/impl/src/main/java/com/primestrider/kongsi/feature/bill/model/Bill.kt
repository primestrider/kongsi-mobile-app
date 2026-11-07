package com.primestrider.kongsi.feature.bill.model

/** Someone on a bill. The payer is a person too; their share is shown but not requested. */
data class Person(
    val id: Long,
    val name: String,
    val phone: String? = null,
)

/**
 * One receipt line. [totalMinor] is the line total (price × quantity) in minor units. [sharedBy] holds the ids of the
 * people who had it; each pays an equal part. Lines the receipt does not charge service or tax on have [serviceable] or
 * [taxable] turned off.
 */
data class BillItem(
    val id: Long,
    val name: String,
    val totalMinor: Long,
    val sharedBy: Set<Long>,
    val quantity: Int = 1,
    val serviceable: Boolean = true,
    val taxable: Boolean = true,
)

/** A charge printed on the receipt, in minor units; [rateBps] is the rate it was entered with (10% = 1000), if any. */
data class BillCharge(
    val amountMinor: Long = 0,
    val rateBps: Int? = null,
)

/**
 * What [SplitCalculator] needs to split a bill. Money is in minor units of the bill's currency (IDR has none, so
 * Rp45.000 is 45000). [discountMinor] is a bill-level discount; a discount on one item is its lower price instead.
 * [receiptTotalMinor] is the total printed on the receipt, when known, to warn when the lines do not add up to it.
 */
data class Bill(
    val people: List<Person>,
    val items: List<BillItem>,
    val service: BillCharge = BillCharge(),
    val tax: BillCharge = BillCharge(),
    val discountMinor: Long = 0,
    val receiptTotalMinor: Long? = null,
)
