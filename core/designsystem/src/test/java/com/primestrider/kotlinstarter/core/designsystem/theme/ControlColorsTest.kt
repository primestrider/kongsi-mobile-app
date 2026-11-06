package com.primestrider.kotlinstarter.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ControlColorsTest {
    @Test
    fun focus_tint_follows_primary() {
        val green = LightColors.copy(primary = Palette.Success.c500)
        assertNotEquals(LightColors.controlFocusFill, green.controlFocusFill)
        assertEquals(
            Palette.Primary.c500
                .copy(alpha = 0.12f)
                .compositeOver(LightColors.controlFill),
            LightColors.controlFocusFill,
        )
    }

    @Test
    fun error_tint_follows_destructive() {
        assertEquals(
            Palette.Error.c500
                .copy(alpha = 0.10f)
                .compositeOver(DarkColors.controlFill),
            DarkColors.controlErrorFill,
        )
    }

    /** Largest per-channel difference on a 0..255 scale. */
    private fun distance(
        a: Color,
        b: Color,
    ): Int = maxOf(abs(a.red - b.red), abs(a.green - b.green), abs(a.blue - b.blue)).times(255).toInt()

    @Test
    fun focus_and_error_fills_stand_out_from_card_and_background() {
        listOf(LightColors, DarkColors).forEach { colors ->
            listOf(colors.controlFocusFill, colors.controlErrorFill).forEach { fill ->
                assertTrue("$fill too close to card ${colors.card}", distance(fill, colors.card) >= 12)
                assertTrue("$fill too close to background ${colors.background}", distance(fill, colors.background) >= 12)
            }
        }
    }

    @Test
    fun control_fill_differs_from_background_and_card() {
        listOf(LightColors, DarkColors).forEach {
            assertNotEquals(it.background, it.controlFill)
            assertNotEquals(it.card, it.controlFill)
        }
    }
}
