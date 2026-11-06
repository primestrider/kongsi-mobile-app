package com.primestrider.kotlinstarter.feature.example.sdk.share

/** What the e-receipt shows; [amount] is whole rupiah and [paidAt] epoch milliseconds. */
data class Receipt(
    val merchant: String,
    val amount: Long,
    val reference: String,
    val paidAt: Long,
)

/** The digits in [text] as a number ("Rp 25.000" is 25000); 0 when there are none or they overflow. */
fun parseAmount(text: String): Long = text.filter { it.isDigit() }.toLongOrNull() ?: 0L

/** `TRX-` plus [now] in upper-case base 36. */
fun receiptReference(now: Long): String =
    "TRX-" +
        java.lang.Long
            .toString(now, 36)
            .uppercase()
