package com.primestrider.kotlinstarter.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class FormattersTest {
    @Test
    fun usd_in_us_locale() = assertEquals("$9.99", formatCurrency(9.99, "USD", Locale.US))

    @Test
    fun rupiah_in_indonesian_locale() {
        val text = formatCurrency(1500.0, "IDR", Locale.forLanguageTag("in-ID"))
        assertTrue(text, text.contains("Rp"))
        assertTrue(text, text.contains("1.500"))
    }

    @Test
    fun fraction_digits_override_drops_the_decimals() {
        val text = formatCurrency(50000.0, "IDR", Locale.US, fractionDigits = 0)
        assertEquals("IDR50,000", text)
    }

    @Test
    fun fractional_percent_keeps_one_decimal() = assertEquals("12.6%", formatPercent(0.1258, Locale.US))

    @Test
    fun whole_percent_has_no_decimals() = assertEquals("10%", formatPercent(0.1, Locale.US))

    @Test
    fun iso_date_is_shown_in_the_locale() = assertEquals("May 23, 2024", formatIsoDate("2024-05-23T08:56:21.618Z", Locale.US))

    @Test
    fun unparseable_date_is_shown_as_is() = assertEquals("soon", formatIsoDate("soon", Locale.US))
}
