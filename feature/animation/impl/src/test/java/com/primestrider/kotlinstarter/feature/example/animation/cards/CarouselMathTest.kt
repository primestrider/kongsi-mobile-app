package com.primestrider.kotlinstarter.feature.example.animation.cards

import org.junit.Assert.assertEquals
import org.junit.Test

class CarouselMathTest {
    @Test
    fun scale_is_full_at_centre_and_min_at_one_page_or_more() {
        assertEquals(1f, carouselScale(0f), 0f)
        assertEquals(0.85f, carouselScale(1f), 1e-6f)
        assertEquals(0.85f, carouselScale(-1f), 1e-6f)
        assertEquals(0.85f, carouselScale(3f), 1e-6f)
    }

    @Test
    fun scale_is_linear_between() {
        assertEquals(0.925f, carouselScale(-0.5f), 1e-6f)
        assertEquals(0.925f, carouselScale(0.5f), 1e-6f)
    }

    @Test
    fun alpha_has_the_same_shape() {
        assertEquals(1f, carouselAlpha(0f), 0f)
        assertEquals(0.6f, carouselAlpha(1f), 1e-6f)
        assertEquals(0.6f, carouselAlpha(-4f), 1e-6f)
        assertEquals(0.8f, carouselAlpha(0.5f), 1e-6f)
    }
}
