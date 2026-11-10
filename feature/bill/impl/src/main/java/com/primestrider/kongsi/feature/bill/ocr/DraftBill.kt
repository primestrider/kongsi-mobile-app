package com.primestrider.kongsi.feature.bill.ocr

import com.primestrider.kongsi.feature.bill.model.Bill
import com.primestrider.kongsi.feature.bill.model.BillCharge
import com.primestrider.kongsi.feature.bill.model.BillItem

/**
 * A new, unsaved bill from what was read on a receipt, for the Review step: the items (shared by no one yet), the
 * charges with the rates printed next to them, and the receipt's total, so Review can tell whether the lines add up.
 */
fun ParsedReceipt.toDraftBill(): Bill =
    Bill(
        people = emptyList(),
        items = items.map { BillItem(id = 0, name = it.name, totalMinor = it.totalMinor, sharedBy = emptySet(), quantity = it.quantity) },
        service = service?.let { BillCharge(it.amountMinor, it.rateBps) } ?: BillCharge(),
        tax = tax?.let { BillCharge(it.amountMinor, it.rateBps) } ?: BillCharge(),
        discountMinor = discountMinor,
        receiptTotalMinor = totalMinor,
    )
