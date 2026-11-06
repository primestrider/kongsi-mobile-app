package com.primestrider.kotlinstarter.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

class TokensTest {
    @Test
    fun light_colors_map_to_palette() {
        assertEquals(Palette.white, LightColors.background)
        assertEquals(Palette.Gray.c900, LightColors.foreground)
    }

    @Test
    fun dark_colors_map_to_palette() {
        assertEquals(Palette.Gray.c950, DarkColors.background)
        assertEquals(Palette.Primary.c400, DarkColors.primary)
        assertEquals(Palette.Gray.c950, DarkColors.primaryForeground)
    }

    @Test
    fun palette_values_match_rn() {
        assertEquals(Color(0xFF208AEF), Palette.Primary.c500)
        assertEquals(Color(0xFF808FAD), Palette.Mist.c500)
        assertEquals(Color(0xFF1D4ED8), Palette.Info.c700)
    }

    @Test
    fun palette_scales_list_covers_every_scale() {
        val names = Palette.scales.map { it.first }
        assertEquals(31, names.size)
        assertEquals(
            listOf("taupe", "mauve", "mist", "olive"),
            names.subList(names.indexOf("taupe"), names.indexOf("olive") + 1),
        )
        val gray = Palette.scales.first { it.first == "gray" }
        assertEquals(11, gray.second.size)
    }

    @Test
    fun spacing_radii_typography() {
        assertEquals(16.dp, Spacing.s4)
        assertEquals(384.dp, Spacing.s96)
        assertEquals(12.dp, Radii.lg)
        assertEquals(30.sp, AppTypography.headlineLarge.fontSize)
    }
}
