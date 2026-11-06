package com.primestrider.kotlinstarter.app.di

import org.junit.Assert.assertEquals
import org.junit.Test

class ParsePinsTest {
    @Test
    fun blank_means_no_pins() {
        assertEquals(emptySet<String>(), parsePins(""))
        assertEquals(emptySet<String>(), parsePins(" , "))
    }

    @Test
    fun pins_are_split_and_trimmed() {
        assertEquals(setOf("sha256/a=", "sha256/b="), parsePins("sha256/a=, sha256/b= ,"))
    }
}
