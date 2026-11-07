package com.primestrider.kongsi.core.util

private const val SUBSCRIBER_MIN = 9
private const val SUBSCRIBER_MAX = 12
private const val SEPARATORS = " -.()"

/** Indonesian mobile numbers as people type them: `0812-3456-7890`, `+62 812 3456 7890`, `62812…`, `812…`. */
object IndonesianPhone {
    /**
     * The subscriber part (`81234567890`, without `0` or `62`), or null when [raw] is not an Indonesian mobile
     * number. Spaces, dashes, dots and brackets are ignored; a `+` is allowed only in front.
     */
    fun subscriber(raw: String): String? {
        val text = raw.trim()
        val body = text.removePrefix("+")
        if (body.any { !it.isDigit() && it !in SEPARATORS }) return null
        val digits = body.filter(Char::isDigit)
        val subscriber =
            when {
                text.startsWith("+") -> if (digits.startsWith("62")) digits.drop(2) else return null
                digits.startsWith("62") -> digits.drop(2)
                digits.startsWith("0") -> digits.drop(1)
                else -> digits
            }
        return subscriber.takeIf { it.startsWith("8") && it.length in SUBSCRIBER_MIN..SUBSCRIBER_MAX }
    }

    /** The number as written locally (`081234567890`), or null when it is not a mobile number. */
    fun local(raw: String): String? = subscriber(raw)?.let { "0$it" }
}
