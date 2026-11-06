package com.primestrider.kotlinstarter.feature.example.components

import org.junit.Assert.assertEquals
import org.junit.Test

class FilterDecimalTest {
    @Test
    fun keeps_digits_and_first_dot() {
        assertEquals("12.34", filterDecimal("12a.3.4"))
        assertEquals("", filterDecimal(""))
        assertEquals(".5", filterDecimal("..5"))
        assertEquals("1.00050", filterDecimal("Rp 1.000,50"))
    }
}
