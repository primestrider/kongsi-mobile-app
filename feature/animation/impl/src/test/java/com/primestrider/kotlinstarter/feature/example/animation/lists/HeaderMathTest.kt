package com.primestrider.kotlinstarter.feature.example.animation.lists

import org.junit.Assert.assertEquals
import org.junit.Test

class HeaderMathTest {
    @Test
    fun collapse_fraction_is_clamped_to_zero_and_one() {
        assertEquals(0f, collapseFraction(0, 300), 0f)
        assertEquals(0.5f, collapseFraction(150, 300), 0f)
        assertEquals(1f, collapseFraction(900, 300), 0f)
        assertEquals(0f, collapseFraction(-20, 300), 0f)
    }

    @Test
    fun a_range_of_zero_or_less_counts_as_fully_collapsed() {
        assertEquals(1f, collapseFraction(10, 0), 0f)
        assertEquals(1f, collapseFraction(10, -5), 0f)
    }

    @Test
    fun parallax_moves_at_half_speed_and_never_goes_negative() {
        assertEquals(100f, parallaxOffset(200), 0f)
        assertEquals(0f, parallaxOffset(-5), 0f)
        assertEquals(30f, parallaxOffset(100, factor = 0.3f), 0.001f)
    }
}
