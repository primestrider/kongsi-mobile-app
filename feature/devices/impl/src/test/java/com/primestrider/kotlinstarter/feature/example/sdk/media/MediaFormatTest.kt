package com.primestrider.kotlinstarter.feature.example.sdk.media

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaFormatTest {
    @Test
    fun zero_is_zero_minutes() {
        assertEquals("0:00", formatClock(0))
    }

    @Test
    fun partial_seconds_are_dropped() {
        assertEquals("1:01", formatClock(61_500))
    }

    @Test
    fun minutes_are_not_capped_at_an_hour() {
        assertEquals("61:05", formatClock(3_665_000))
    }

    @Test
    fun negative_values_read_as_zero() {
        assertEquals("0:00", formatClock(-5))
    }
}
