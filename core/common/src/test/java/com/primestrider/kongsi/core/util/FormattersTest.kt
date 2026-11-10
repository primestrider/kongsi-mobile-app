package com.primestrider.kongsi.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale
import java.util.TimeZone

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
    fun epoch_millis_date_is_shown_in_the_locale_and_time_zone() {
        // 2026-11-06T17:30:00Z is already Nov 7 in Jakarta (UTC+7).
        val millis = 1_793_986_200_000L
        assertEquals("Nov 7, 2026", formatDate(millis, Locale.US, TimeZone.getTimeZone("Asia/Jakarta")))
        assertEquals("Nov 6, 2026", formatDate(millis, Locale.US, TimeZone.getTimeZone("UTC")))
    }

    @Test
    fun amounts_are_grouped_numbers_without_a_currency() {
        assertEquals("152.460", formatAmount(152_460, Locale.forLanguageTag("id-ID")))
        assertEquals("152,460", formatAmount(152_460, Locale.US))
        assertEquals("0", formatAmount(0, Locale.forLanguageTag("id-ID")))
        assertEquals("-8.525", formatAmount(-8_525, Locale.forLanguageTag("id-ID")))
        // Far beyond what a Double holds exactly.
        assertEquals("9.007.199.254.740.993", formatAmount(9_007_199_254_740_993, Locale.forLanguageTag("id-ID")))
    }

    @Test
    fun unparseable_date_is_shown_as_is() = assertEquals("soon", formatIsoDate("soon", Locale.US))
}
