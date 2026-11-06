package com.primestrider.kotlinstarter.feature.example.sdk.biometric

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinRulesTest {
    @Test
    fun six_digits_are_valid() {
        assertTrue(isValidPin("123456"))
    }

    @Test
    fun anything_else_is_invalid() {
        listOf("12345", "1234567", "12a456", " 123456", "", "١٢٣٤٥٦").forEach {
            assertFalse("'$it' should be invalid", isValidPin(it))
        }
    }
}
