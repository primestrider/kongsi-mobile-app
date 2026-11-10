package com.primestrider.kongsi.feature.bill.ocr

import kotlin.math.abs
import kotlin.math.roundToInt

/** One line of text recognised on a receipt photo, with its box in pixels (as ML Kit gives it). */
data class ReceiptLine(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val centerY: Int get() = (top + bottom) / 2
}

/** A receipt line read as an item: [totalMinor] is the line total (after a discount printed under it). */
data class ParsedItem(
    val name: String,
    val quantity: Int,
    val totalMinor: Long,
)

/** A charge printed on the receipt, with the rate printed next to it ("PB1 10%"), if any. */
data class ParsedCharge(
    val amountMinor: Long,
    val rateBps: Int? = null,
)

/**
 * What a receipt says, as far as it could be read. Everything is a starting point for the Review step, where the user
 * checks and fixes it; a part not found is null (or zero for the discount).
 */
data class ParsedReceipt(
    val items: List<ParsedItem>,
    val subtotalMinor: Long?,
    val service: ParsedCharge?,
    val tax: ParsedCharge?,
    val discountMinor: Long,
    val totalMinor: Long?,
)

/**
 * Reads the text recognised on an Indonesian receipt into items and charges. Pure Kotlin, so it is tested against
 * fixtures of receipt layouts (`src/test/resources/receipts`).
 *
 * Lines are first put back into rows by their height on the photo, since the name and the price of an item are often
 * recognised as separate lines. A row ending in a price is an item, unless a keyword makes it a subtotal, total,
 * service, tax, discount or payment row. Rows after the total (payment, change, tax notes) are ignored. A discount
 * right under an item lowers that item; one elsewhere is a discount on the whole bill.
 */
object ReceiptParser {
    fun parse(lines: List<ReceiptLine>): ParsedReceipt {
        val items = mutableListOf<ParsedItem>()
        var subtotal: Long? = null
        var service: ParsedCharge? = null
        var tax: ParsedCharge? = null
        var discount = 0L
        var total: Long? = null
        // A row without a price that may name the item on the next row ("INDOMIE GORENG" above "5 x 3.500  17.500").
        var pendingName: String? = null
        var lastWasItem = false

        for (row in rowsOf(lines)) {
            if (total != null) break
            // A currency printed after the amount ("45.000 IDR") is not part of it.
            val tokens = row.split(WHITESPACE).filter { it.isNotEmpty() }.dropLastWhile(::isCurrency)
            val price = tokens.lastOrNull()?.let(::priceOf)
            if (price == null) {
                if (row.any(Char::isLetter)) pendingName = row.trim()
                continue
            }
            val labelTokens = tokens.dropLast(1).dropLastWhile { isCurrency(it) || it in PUNCTUATION }
            val label = labelTokens.joinToString(" ").lowercase()
            val amount = abs(price)
            val isItem = lastWasItem
            lastWasItem = false
            when {
                label.hasAny(SUBTOTAL) -> subtotal = amount
                // Before payment words, so "Total Bayar" is the total.
                label.hasAny(TOTAL) && !label.hasAny(COUNTS) -> total = amount
                label.hasAny(PAYMENT) -> Unit
                label.hasAny(DISCOUNT) ->
                    if (isItem && items.isNotEmpty()) {
                        val last = items.removeAt(items.lastIndex)
                        items += last.copy(totalMinor = last.totalMinor - amount)
                        lastWasItem = true
                    } else {
                        discount += amount
                    }
                label.hasAny(SERVICE) -> service = ParsedCharge(amount, rateOf(label))
                label.hasAny(TAX) -> tax = ParsedCharge(amount, rateOf(label))
                else -> {
                    val item = itemOf(labelTokens, amount, pendingName)
                    if (item != null) {
                        items += item
                        lastWasItem = true
                    }
                }
            }
            pendingName = null
        }
        return ParsedReceipt(items, subtotal, service, tax, discount, total)
    }

    /**
     * A price as printed: `45.000`, `45,000`, `45000`, `45.000,00` or `45,000.00`, with or without a currency stuck to
     * it (`Rp45.000`, `IDR45.000`, `$45,000`), negative with a leading minus or in brackets. The currency is ignored.
     * Null for anything else, and for amounts under 100 (quantities, table numbers).
     */
    fun priceOf(token: String): Long? {
        val trimmed = token.trim()
        val negative = trimmed.startsWith("-") || (trimmed.startsWith("(") && trimmed.endsWith(")"))
        val text = trimmed.trim('-', '(', ')').replace(CURRENCY_PREFIX, "")
        val value =
            when {
                GROUPED.matches(text) -> text.replace(DECIMALS, "").filter(Char::isDigit).toLong()
                PLAIN.matches(text) -> text.toLong()
                else -> return null
            }
        if (value < MIN_PRICE) return null
        return if (negative) -value else value
    }

    /** Lines put back into rows: a line whose middle falls within the first line of a row belongs to that row. */
    private fun rowsOf(lines: List<ReceiptLine>): List<String> {
        val rows = mutableListOf<MutableList<ReceiptLine>>()
        lines
            .filter { it.text.isNotBlank() }
            .sortedBy { it.centerY }
            .forEach { line ->
                val row = rows.lastOrNull()
                val first = row?.first()
                if (first != null && line.centerY in first.top..first.bottom) row += line else rows += mutableListOf(line)
            }
        return rows.map { row -> row.sortedBy { it.left }.joinToString(" ") { it.text.trim() } }
    }

    /**
     * An item row's label read as a quantity and a name: `2x Es Teh`, `2 Es Teh`, `Lemon Tea x2`, `Sate Ayam 2 35.000`
     * (quantity and unit price in columns), or only `5 x 3.500` with the name on the row above.
     */
    private fun itemOf(
        labelTokens: List<String>,
        totalMinor: Long,
        nameAbove: String?,
    ): ParsedItem? {
        if (totalMinor <= 0) return null
        val text = labelTokens.joinToString(" ")
        QUANTITY_ONLY.matchEntire(text)?.let { match ->
            val name = nameAbove?.let(::tidyName) ?: return null
            return ParsedItem(name, match.groupValues[1].toInt(), totalMinor)
        }

        val words = labelTokens.toMutableList()
        var quantity: Int? = null
        // Columns at the end: a unit price, then the quantity ("Sate Ayam  2  35.000").
        if (words.size > 1 && priceOf(words.last()) != null) words.removeAt(words.lastIndex)
        if (words.lastOrNull() == "@") words.removeAt(words.lastIndex)
        if (words.size > 1) {
            TRAILING_QUANTITY.matchEntire(words.last())?.let { match ->
                quantity =
                    match.groupValues
                        .drop(1)
                        .first { it.isNotEmpty() }
                        .toInt()
                words.removeAt(words.lastIndex)
            }
        }
        if (quantity == null && words.size > 1) {
            LEADING_QUANTITY.matchEntire(words.first())?.let { match ->
                quantity = match.groupValues[1].toInt()
                words.removeAt(0)
                if (words.size > 1 && words.first().lowercase() in TIMES) words.removeAt(0)
            }
        }
        val name = tidyName(words.joinToString(" ")) ?: return null
        return ParsedItem(name, quantity?.takeIf { it > 0 } ?: 1, totalMinor)
    }

    /** The name without stray punctuation, and in Title Case when the receipt prints it in capitals. */
    private fun tidyName(raw: String): String? {
        val name = raw.trim().trim(':', '-', '*', '.', ' ')
        if (name.none(Char::isLetter)) return null
        val shouting = name.filter(Char::isLetter).all(Char::isUpperCase)
        return if (shouting) name.lowercase().split(" ").joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) } else name
    }

    /** The rate printed in a charge's label ("PB1 10%", "Service 5,5%") in basis points. */
    private fun rateOf(label: String): Int? =
        RATE
            .find(label)
            ?.groupValues
            ?.get(1)
            ?.replace(',', '.')
            ?.toDoubleOrNull()
            ?.let { (it * BPS_PER_PERCENT).roundToInt() }

    private fun String.hasAny(words: List<Regex>): Boolean = words.any { it.containsMatchIn(this) }

    private fun words(vararg words: String): List<Regex> = words.map { Regex("""(?<![\p{L}\d])${Regex.escape(it)}(?![\p{L}\d])""") }

    private val SUBTOTAL = words("subtotal", "sub total", "sub-total", "harga jual")
    private val TOTAL = words("total", "jumlah bayar", "grand total")
    private val COUNTS = words("item", "items", "qty", "jml item")
    private val DISCOUNT = words("diskon", "discount", "disc", "potongan", "promo", "voucher")
    private val SERVICE = words("service", "servis", "sc", "s/c", "layanan")
    private val TAX = words("pajak", "ppn", "pb1", "pbj", "tax", "vat")
    private val PAYMENT =
        words(
            "tunai",
            "cash",
            "kembali",
            "kembalian",
            "change",
            "bayar",
            "debit",
            "kredit",
            "credit",
            "qris",
            "visa",
            "mastercard",
            "kartu",
            "card",
        )

    private val WHITESPACE = Regex("""\s+""")

    /** Currencies receipts print, as a word of their own or stuck to an amount; never part of the amount. */
    private const val CURRENCIES = """rp|idr|rm|myr|sgd|s\$|usd|us\$|php|thb|vnd|eur|\p{Sc}"""
    private val CURRENCY_PREFIX = Regex("""^(?:$CURRENCIES)\.?\s*""", RegexOption.IGNORE_CASE)
    private val CURRENCY_WORD = Regex("""(?:$CURRENCIES)\.?""", RegexOption.IGNORE_CASE)

    private fun isCurrency(token: String): Boolean = CURRENCY_WORD.matches(token)

    /** Thousands grouped by `.` or `,`, with optional two decimals: `45.000`, `1.250.000`, `45.000,00`. */
    private val GROUPED = Regex("""\d{1,3}(?:[.,]\d{3})+(?:[.,]\d{2})?""")
    private val DECIMALS = Regex("""[.,]\d{2}$""")

    /** Digits only, no leading zero (phone numbers start with 0), at most 7 of them. */
    private val PLAIN = Regex("""[1-9]\d{2,6}""")

    private val QUANTITY_ONLY = Regex("""(\d{1,3})\s*[xX×]\s*@?\s*[\d.,]+""")
    private val TRAILING_QUANTITY = Regex("""(?:[xX×](\d{1,2})|(\d{1,2})[xX×]?)""")
    private val LEADING_QUANTITY = Regex("""(\d{1,2})[xX×]?""")
    private val TIMES = setOf("x", "×")
    private val RATE = Regex("""(\d{1,2}(?:[.,]\d{1,2})?)\s*%""")
    private val PUNCTUATION = setOf(":", "=")

    private const val MIN_PRICE = 100L
    private const val BPS_PER_PERCENT = 100
}
