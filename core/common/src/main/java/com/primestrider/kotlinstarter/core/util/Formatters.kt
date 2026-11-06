package com.primestrider.kotlinstarter.core.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import java.text.DateFormat
import java.text.NumberFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.round

/** [amount] in [currencyCode], with the symbol, grouping and decimals of [locale]; [fractionDigits] overrides the decimals (0 for whole rupiah). */
fun formatCurrency(
    amount: Double,
    currencyCode: String = "USD",
    locale: Locale = Locale.getDefault(),
    fractionDigits: Int? = null,
): String =
    NumberFormat
        .getCurrencyInstance(locale)
        .apply {
            currency = Currency.getInstance(currencyCode)
            fractionDigits?.let {
                minimumFractionDigits = it
                maximumFractionDigits = it
            }
        }.format(amount)

/** [fraction] as a percentage (0.125 → "12.5%"): no decimals for whole percentages, otherwise one. */
fun formatPercent(
    fraction: Double,
    locale: Locale = Locale.getDefault(),
): String {
    val percent = fraction * 100
    val whole = abs(percent - round(percent)) < 1e-9
    return NumberFormat
        .getPercentInstance(locale)
        .apply { maximumFractionDigits = if (whole) 0 else 1 }
        .format(fraction)
}

/** The calendar date of an ISO-8601 timestamp ("2024-05-23T08:56:21Z") in [locale]; unparseable input is returned as is. */
fun formatIsoDate(
    iso: String,
    locale: Locale = Locale.getDefault(),
): String {
    val utc = TimeZone.getTimeZone("UTC")
    val date =
        try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .apply {
                    isLenient = false
                    timeZone = utc
                }.parse(iso.take(10))
        } catch (e: ParseException) {
            null
        } ?: return iso
    return DateFormat.getDateInstance(DateFormat.MEDIUM, locale).apply { timeZone = utc }.format(date)
}

/** The locale of the current app language; pass it to the formatters from composables. */
@Composable
@ReadOnlyComposable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]
