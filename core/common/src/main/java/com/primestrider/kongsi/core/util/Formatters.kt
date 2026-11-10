package com.primestrider.kongsi.core.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import java.math.BigDecimal
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
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

/**
 * [amountMinor] minor units of [currencyCode] as money in [locale], exactly (no floating point). Rupiah has no
 * decimals in practice and always shows as "Rp" (Java's defaults are two decimals and "IDR" outside Indonesia).
 */
fun formatMoney(
    amountMinor: Long,
    currencyCode: String,
    locale: Locale = Locale.getDefault(),
): String {
    val currency = Currency.getInstance(currencyCode)
    val digits = if (currencyCode == RUPIAH) 0 else currency.defaultFractionDigits.coerceAtLeast(0)
    val format =
        (NumberFormat.getCurrencyInstance(locale) as DecimalFormat).apply {
            this.currency = currency
            minimumFractionDigits = digits
            maximumFractionDigits = digits
            if (currencyCode == RUPIAH) {
                decimalFormatSymbols = decimalFormatSymbols.apply { currencySymbol = "Rp" }
                // Some locales put a space between symbol and amount; Indonesians write "Rp45.000".
                positivePrefix = positivePrefix.replace("Rp ", "Rp").replace("Rp ", "Rp")
                negativePrefix = negativePrefix.replace("Rp ", "Rp").replace("Rp ", "Rp")
            }
        }
    return format.format(BigDecimal.valueOf(amountMinor, digits))
}

private const val RUPIAH = "IDR"

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

/** The calendar date of [epochMillis] in [timeZone] (the device's by default), written the way [locale] writes dates. */
fun formatDate(
    epochMillis: Long,
    locale: Locale = Locale.getDefault(),
    timeZone: TimeZone = TimeZone.getDefault(),
): String = DateFormat.getDateInstance(DateFormat.MEDIUM, locale).apply { this.timeZone = timeZone }.format(Date(epochMillis))

/** The locale of the current app language; pass it to the formatters from composables. */
@Composable
@ReadOnlyComposable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]
