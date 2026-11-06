package com.primestrider.kotlinstarter.feature.example.sdk.biometric

private val PIN_PATTERN = Regex("^\\d{6}$")

/** A transaction PIN is exactly six ASCII digits. */
fun isValidPin(pin: String): Boolean = PIN_PATTERN.matches(pin)
