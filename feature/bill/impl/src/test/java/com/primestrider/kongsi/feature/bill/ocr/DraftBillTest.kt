package com.primestrider.kongsi.feature.bill.ocr

import com.primestrider.kongsi.feature.bill.model.Bill
import com.primestrider.kongsi.feature.bill.model.BillCharge
import com.primestrider.kongsi.feature.bill.model.BillItem
import org.junit.Assert.assertEquals
import org.junit.Test

class DraftBillTest {
    @Test
    fun a_read_receipt_becomes_a_draft_bill_to_review() {
        val receipt =
            ParsedReceipt(
                items = listOf(ParsedItem("Nasi Goreng", 1, 30_000), ParsedItem("Ocha", 2, 12_000)),
                subtotalMinor = 42_000,
                service = ParsedCharge(2_100, rateBps = 500),
                tax = ParsedCharge(4_410),
                discountMinor = 1_000,
                totalMinor = 47_510,
            )

        assertEquals(
            Bill(
                people = emptyList(),
                items =
                    listOf(
                        BillItem(0, "Nasi Goreng", 30_000, sharedBy = emptySet()),
                        BillItem(0, "Ocha", 12_000, sharedBy = emptySet(), quantity = 2),
                    ),
                service = BillCharge(2_100, 500),
                tax = BillCharge(4_410),
                discountMinor = 1_000,
                receiptTotalMinor = 47_510,
            ),
            receipt.toDraftBill(),
        )
    }

    @Test
    fun charges_not_found_are_zero_and_an_unknown_total_is_left_open() {
        val bill = ParsedReceipt(listOf(ParsedItem("Es Teh", 1, 5_000)), null, null, null, 0, null).toDraftBill()

        assertEquals(BillCharge(), bill.service)
        assertEquals(BillCharge(), bill.tax)
        assertEquals(null, bill.receiptTotalMinor)
    }
}
