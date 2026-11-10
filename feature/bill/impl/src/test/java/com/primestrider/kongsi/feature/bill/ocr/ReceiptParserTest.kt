package com.primestrider.kongsi.feature.bill.ocr

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReceiptParserTest {
    /** The expected result of a fixture, as written in its `.json` file. */
    @Serializable
    private data class Expected(
        val items: List<Item>,
        val subtotalMinor: Long?,
        val service: Charge?,
        val tax: Charge?,
        val discountMinor: Long,
        val totalMinor: Long?,
    ) {
        @Serializable
        data class Item(
            val name: String,
            val quantity: Int,
            val totalMinor: Long,
        )

        @Serializable
        data class Charge(
            val amountMinor: Long,
            val rateBps: Int?,
        )

        fun toReceipt() =
            ParsedReceipt(
                items = items.map { ParsedItem(it.name, it.quantity, it.totalMinor) },
                subtotalMinor = subtotalMinor,
                service = service?.let { ParsedCharge(it.amountMinor, it.rateBps) },
                tax = tax?.let { ParsedCharge(it.amountMinor, it.rateBps) },
                discountMinor = discountMinor,
                totalMinor = totalMinor,
            )
    }

    private fun resource(name: String): String =
        requireNotNull(javaClass.getResource("/receipts/$name")) { "Missing fixture $name" }.readText()

    /**
     * A fixture as ML Kit would see it: each text line is one row of the receipt, and text set apart by two or more
     * spaces (the name and the price columns) is a separate line on that row, placed by its column.
     */
    private fun linesOf(text: String): List<ReceiptLine> =
        text.lines().flatMapIndexed { row, line ->
            Regex("""\S+(?: \S+)*""")
                .findAll(line)
                .map { match ->
                    val top = row * ROW_HEIGHT
                    ReceiptLine(
                        match.value,
                        left = match.range.first * CHAR_WIDTH,
                        top = top,
                        right = (match.range.last + 1) * CHAR_WIDTH,
                        bottom =
                            top + LINE_HEIGHT,
                    )
                }.toList()
        }

    private fun check(fixture: String) {
        val expected = Json.decodeFromString<Expected>(resource("$fixture.json")).toReceipt()
        assertEquals(fixture, expected, ReceiptParser.parse(linesOf(resource("$fixture.txt"))))
    }

    @Test fun restaurant_with_service_and_pb1() = check("restoran_pb1_service")

    @Test fun coffee_shop_with_quantity_prefixes_and_ppn() = check("kedai_kopi")

    @Test fun minimarket_with_quantities_on_their_own_line_and_tax_after_the_total() = check("minimarket")

    @Test fun item_discount_and_bill_discount() = check("diskon")

    @Test fun quantity_and_unit_price_columns() = check("qty_kolom")

    @Test fun rupiah_prefix_and_decimals() = check("desimal_rp")

    @Test fun comma_thousands_and_quantity_suffix() = check("koma_ribuan")

    @Test fun plain_digits_without_subtotal() = check("warung_tulis")

    @Test
    fun lines_of_one_row_are_joined_even_when_slightly_skewed_or_out_of_order() {
        // The price sits a few pixels lower than its name, and ML Kit returns it first.
        val lines =
            listOf(
                ReceiptLine("30.000", left = 300, top = 106, right = 360, bottom = 130),
                ReceiptLine("Nasi Goreng", left = 10, top = 100, right = 120, bottom = 124),
                ReceiptLine("12.000", left = 300, top = 141, right = 360, bottom = 165),
                ReceiptLine("Ocha", left = 10, top = 138, right = 60, bottom = 162),
            )

        assertEquals(
            listOf(ParsedItem("Nasi Goreng", 1, 30_000), ParsedItem("Ocha", 1, 12_000)),
            ReceiptParser.parse(lines).items,
        )
    }

    @Test
    fun prices_are_read_in_every_common_form() {
        val read = { text: String -> ReceiptParser.priceOf(text) }

        assertEquals(45_000L, read("45.000"))
        assertEquals(45_000L, read("45,000"))
        assertEquals(45_000L, read("45000"))
        assertEquals(45_000L, read("Rp45.000"))
        assertEquals(45_000L, read("45.000,00"))
        assertEquals(45_000L, read("45,000.00"))
        assertEquals(1_250_000L, read("1.250.000"))
        assertEquals(-1_800L, read("-1.800"))
        assertNull(read("12"))
        assertNull(read("2.0.31"))
        assertNull(read("19:32"))
        assertNull(read("022-7301234"))
        assertNull(read("081234567890"))
    }

    @Test
    fun total_bayar_is_the_total_not_a_payment() {
        val receipt =
            ReceiptParser.parse(
                linesOf(listOf("Es Teh      5.000", "Total Bayar      5.000", "Tunai     10.000").joinToString("\n")),
            )

        assertEquals(5_000L, receipt.totalMinor)
        assertEquals(listOf(ParsedItem("Es Teh", 1, 5_000)), receipt.items)
    }

    @Test
    fun nothing_readable_gives_an_empty_receipt() {
        assertEquals(ParsedReceipt(emptyList(), null, null, null, 0, null), ReceiptParser.parse(emptyList()))
    }

    private companion object {
        const val ROW_HEIGHT = 40
        const val LINE_HEIGHT = 30
        const val CHAR_WIDTH = 10
    }
}
