package com.primestrider.kotlinstarter.feature.example.sdk.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QrResultGateTest {
    @Test
    fun first_offer_is_accepted() {
        val gate = QrResultGate()
        assertTrue(gate.offer("https://example.com"))
        assertEquals("https://example.com", gate.current)
    }

    @Test
    fun the_same_code_seen_again_is_rejected() {
        val gate = QrResultGate()
        gate.offer("a")
        assertFalse(gate.offer("a"))
        assertEquals("a", gate.current)
    }

    @Test
    fun another_code_is_rejected_while_one_shows() {
        val gate = QrResultGate()
        gate.offer("a")
        assertFalse(gate.offer("b"))
        assertEquals("a", gate.current)
    }

    @Test
    fun a_code_is_accepted_again_after_clear() {
        val gate = QrResultGate()
        gate.offer("a")
        gate.clear()
        assertNull(gate.current)
        assertTrue(gate.offer("a"))
    }

    @Test
    fun nothing_is_accepted_when_not_scanning() {
        val gate = QrResultGate()
        gate.scanning = false
        assertFalse(gate.offer("a"))
        assertNull(gate.current)
        gate.scanning = true
        assertTrue(gate.offer("a"))
    }

    @Test
    fun a_blank_value_is_rejected() {
        val gate = QrResultGate()
        assertFalse(gate.offer(""))
        assertNull(gate.current)
    }
}
