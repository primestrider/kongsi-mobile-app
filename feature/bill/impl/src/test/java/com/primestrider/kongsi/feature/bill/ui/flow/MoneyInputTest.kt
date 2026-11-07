package com.primestrider.kongsi.feature.bill.ui.flow

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyInputTest {
    @Test
    fun parses_what_people_type_for_rupiah() {
        val table =
            mapOf(
                "45000" to 45_000L,
                "45.000" to 45_000L,
                "45,000" to 45_000L,
                "1.250.000" to 1_250_000L,
                "Rp45.000" to 45_000L,
                "rp 45.000" to 45_000L,
                " 45000 " to 45_000L,
                "0" to 0L,
                "45rb" to 45_000L,
                "45 rb" to 45_000L,
                "45ribu" to 45_000L,
                "45k" to 45_000L,
                "45K" to 45_000L,
                "2.5rb" to 2_500L,
                "2,5k" to 2_500L,
                "1,5jt" to 1_500_000L,
                "1.5 juta" to 1_500_000L,
                "2jt" to 2_000_000L,
            )
        table.forEach { (input, expected) -> assertEquals("\"$input\"", expected, parseMoneyInput(input)) }
    }

    @Test
    fun rejects_what_is_not_an_amount() {
        listOf("", "   ", "abc", "45rbx", "4.5.6rb", "1,2345k", "rb", "-5000", "12a34", "9".repeat(16))
            .forEach { input -> assertEquals("\"$input\"", null, parseMoneyInput(input)) }
    }

    @Test
    fun only_shorthand_needs_an_explanation() {
        assertEquals(false, needsExplanation("45000"))
        assertEquals(false, needsExplanation("45.000"))
        assertEquals(true, needsExplanation("45rb"))
        assertEquals(true, needsExplanation("1,5jt"))
    }

    @Test
    fun digits_are_grouped_while_typing_and_the_cursor_follows() {
        val transformed = ThousandsTransformation('.').filter(AnnotatedString("1250000"))

        assertEquals("1.250.000", transformed.text.text)
        val mapping = transformed.offsetMapping
        assertEquals(0, mapping.originalToTransformed(0))
        assertEquals(1, mapping.originalToTransformed(1))
        assertEquals(3, mapping.originalToTransformed(2))
        assertEquals(9, mapping.originalToTransformed(7))
        assertEquals(1, mapping.transformedToOriginal(2))
        assertEquals(2, mapping.transformedToOriginal(3))
        assertEquals(7, mapping.transformedToOriginal(9))
    }

    @Test
    fun english_groups_with_commas() {
        assertEquals("45,000", ThousandsTransformation(',').filter(AnnotatedString("45000")).text.text)
    }

    @Test
    fun text_that_is_not_plain_digits_is_left_as_typed() {
        listOf("45rb", "45.000", "", "12").forEach { input ->
            assertEquals(input, ThousandsTransformation('.').filter(AnnotatedString(input)).text.text)
        }
    }
}
