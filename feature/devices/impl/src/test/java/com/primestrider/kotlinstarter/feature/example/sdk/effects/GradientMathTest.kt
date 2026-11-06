package com.primestrider.kotlinstarter.feature.example.sdk.effects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Test

class GradientMathTest {
    private fun assertOffset(
        expected: Offset,
        actual: Offset,
    ) {
        assertEquals(expected.x, actual.x, 0.01f)
        assertEquals(expected.y, actual.y, 0.01f)
    }

    @Test
    fun zero_degrees_runs_left_to_right() {
        val (start, end) = gradientOffsets(0f, Size(100f, 100f))
        assertOffset(Offset(0f, 50f), start)
        assertOffset(Offset(100f, 50f), end)
    }

    @Test
    fun ninety_degrees_runs_top_to_bottom() {
        val (start, end) = gradientOffsets(90f, Size(100f, 100f))
        assertOffset(Offset(50f, 0f), start)
        assertOffset(Offset(50f, 100f), end)
    }

    @Test
    fun blur_radius_scales_intensity_to_24_dp() {
        assertEquals(14.4f, blurRadius(60f).value, 0.001f)
        assertEquals(0f, blurRadius(0f).value, 0f)
        assertEquals(24f, blurRadius(100f).value, 0f)
    }
}
