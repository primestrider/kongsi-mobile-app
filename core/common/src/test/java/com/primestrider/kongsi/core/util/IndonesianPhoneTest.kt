package com.primestrider.kongsi.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IndonesianPhoneTest {
    @Test
    fun accepts_the_ways_people_write_a_mobile_number() {
        listOf(
            "081234567890",
            "0812-3456-7890",
            "0812 3456 7890",
            "+62 812 3456 7890",
            "+6281234567890",
            "6281234567890",
            "81234567890",
            "(0812) 3456.7890",
            "  081234567890  ",
        ).forEach { raw ->
            assertEquals(raw, "81234567890", IndonesianPhone.subscriber(raw))
            assertEquals(raw, "081234567890", IndonesianPhone.local(raw))
        }
    }

    @Test
    fun accepts_the_shortest_and_longest_subscriber_numbers() {
        assertEquals("0812345678", IndonesianPhone.local("0812345678"))
        assertNull(IndonesianPhone.local("081234567"))
        assertEquals("0812345678901", IndonesianPhone.local("0812345678901"))
    }

    @Test
    fun rejects_what_is_not_an_indonesian_mobile_number() {
        listOf(
            "",
            "0812",
            "08123456",
            "08123456789012",
            "0212345678",
            "+1 812 345 6789",
            "+081234567890",
            "0812-3456-789a",
            "0812+34567890",
        ).forEach { raw -> assertNull(raw, IndonesianPhone.subscriber(raw)) }
    }
}
