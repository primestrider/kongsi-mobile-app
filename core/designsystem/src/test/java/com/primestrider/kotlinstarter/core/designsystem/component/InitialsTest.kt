package com.primestrider.kotlinstarter.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Test

class InitialsTest {
    @Test
    fun takes_first_letters_of_first_two_words() {
        assertEquals("AL", initialsOf("Ada Lovelace"))
        assertEquals("G", initialsOf("  grace  "))
        assertEquals("JC", initialsOf("Jean Claude Van Damme"))
        assertEquals("?", initialsOf(""))
    }
}
