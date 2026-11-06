package com.primestrider.kotlinstarter.feature.example.gestures

import org.junit.Assert.assertEquals
import org.junit.Test

class GesturePagesTest {
    @Test
    fun entries_map_one_to_one_to_pages() {
        val entries = gestureEntries()
        assertEquals(gesturePages.map { it.route }, entries.map { it.route })
        assertEquals(gesturePages.map { it.title }, entries.map { it.title })
        assertEquals(gesturePages.map { it.subtitle }, entries.map { it.subtitle })
    }

    @Test
    fun routes_are_unique() {
        assertEquals(gesturePages.size, gesturePages.map { it.route }.toSet().size)
    }
}
