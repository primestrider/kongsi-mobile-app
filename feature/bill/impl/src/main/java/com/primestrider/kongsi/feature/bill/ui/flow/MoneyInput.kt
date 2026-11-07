package com.primestrider.kongsi.feature.bill.ui.flow

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.math.BigDecimal

/** Shorthand multipliers people use for rupiah: "45rb" / "45k" is 45.000, "1,5jt" is 1.500.000. */
private val multipliers =
    mapOf(
        "k" to 1_000L,
        "rb" to 1_000L,
        "ribu" to 1_000L,
        "jt" to 1_000_000L,
        "juta" to 1_000_000L,
    )

private val shorthand = Regex("""^(\d+)(?:[.,](\d+))?\s*(k|rb|ribu|jt|juta)$""")
private val grouped = Regex("""^\d{1,3}(?:([.,])\d{3})(?:\1\d{3})*$""")
private val digitsOnly = Regex("""^\d+$""")

/** Above this, an amount is a typo, not a bill (999 trillion rupiah). */
private const val MAX_DIGITS = 15

/**
 * Reads a rupiah amount as people type it: `45000`, `45.000` or `45,000` (thousands separators, no decimals),
 * optionally after `Rp`, or shorthand `45rb`, `45k`, `2,5rb`, `1,5jt`. Returns whole rupiah, or null when the text is
 * not an amount (empty, letters, negative, a fraction of a rupiah, or absurdly large).
 */
fun parseMoneyInput(text: String): Long? {
    val clean =
        text
            .trim()
            .lowercase()
            .removePrefix("rp")
            .trim()
    if (clean.isEmpty()) return null
    shorthand.matchEntire(clean)?.let { match ->
        val (whole, fraction, unit) = match.destructured
        val amount = BigDecimal("$whole.${fraction.ifEmpty { "0" }}") * BigDecimal.valueOf(multipliers.getValue(unit))
        return amount.toWholeRupiahOrNull()
    }
    val digits =
        when {
            digitsOnly.matches(clean) -> clean
            grouped.matches(clean) -> clean.filter { it.isDigit() }
            else -> return null
        }
    if (digits.length > MAX_DIGITS) return null
    return digits.toLong()
}

/** True for shorthand ("45rb", "1,5jt"), where showing the amount it stands for avoids a surprise. */
fun needsExplanation(text: String): Boolean =
    shorthand.matches(
        text
            .trim()
            .lowercase()
            .removePrefix("rp")
            .trim(),
    )

private fun BigDecimal.toWholeRupiahOrNull(): Long? {
    val whole = stripTrailingZeros()
    if (whole.scale() > 0 || whole.precision() - whole.scale() > MAX_DIGITS) return null
    return whole.toLong()
}

/**
 * Shows a run of digits with [separator] every three digits ("1250000" → "1.250.000") while the field keeps the plain
 * digits, so the cursor and editing stay natural. Anything else (shorthand, already grouped) is shown as typed.
 */
class ThousandsTransformation(
    private val separator: Char,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (!digitsOnly.matches(raw) || raw.length <= 3) return TransformedText(text, OffsetMapping.Identity)
        // Separators go in front of these digit indices, e.g. 1 and 4 for "1250000".
        val before = (1 until raw.length).filter { (raw.length - it) % 3 == 0 }
        val shown =
            buildString {
                raw.forEachIndexed { i, c ->
                    if (i in before) append(separator)
                    append(c)
                }
            }
        // Where each separator lands in the shown text.
        val shownSeparators = before.mapIndexed { n, digit -> digit + n }
        val mapping =
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = offset + before.count { it < offset }

                override fun transformedToOriginal(offset: Int): Int = offset - shownSeparators.count { it < offset }
            }
        return TransformedText(AnnotatedString(shown), mapping)
    }

    override fun equals(other: Any?): Boolean = other is ThousandsTransformation && other.separator == separator

    override fun hashCode(): Int = separator.hashCode()
}
