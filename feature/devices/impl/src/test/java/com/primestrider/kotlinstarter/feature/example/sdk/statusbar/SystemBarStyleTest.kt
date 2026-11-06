package com.primestrider.kotlinstarter.feature.example.sdk.statusbar

import org.junit.Assert.assertEquals
import org.junit.Test

class SystemBarStyleTest {
    @Test
    fun auto_follows_the_theme() {
        assertEquals(true, lightIcons(BarStyle.AUTO, darkTheme = false))
        assertEquals(false, lightIcons(BarStyle.AUTO, darkTheme = true))
    }

    @Test
    fun light_icons_are_always_light() {
        assertEquals(false, lightIcons(BarStyle.LIGHT_ICONS, darkTheme = false))
        assertEquals(false, lightIcons(BarStyle.LIGHT_ICONS, darkTheme = true))
    }

    @Test
    fun dark_icons_are_always_dark() {
        assertEquals(true, lightIcons(BarStyle.DARK_ICONS, darkTheme = false))
        assertEquals(true, lightIcons(BarStyle.DARK_ICONS, darkTheme = true))
    }

    @Test
    fun inverted_flips_the_theme() {
        assertEquals(false, lightIcons(BarStyle.INVERTED, darkTheme = false))
        assertEquals(true, lightIcons(BarStyle.INVERTED, darkTheme = true))
    }
}
