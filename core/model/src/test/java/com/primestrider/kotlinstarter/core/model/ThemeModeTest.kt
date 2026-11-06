package com.primestrider.kotlinstarter.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {
    @Test
    fun fromStoredParsesKnownValue() {
        assertEquals(ThemeMode.DARK, ThemeMode.fromStored("DARK"))
    }

    @Test
    fun fromStoredFallsBackToSystemForUnknown() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStored("PURPLE"))
    }

    @Test
    fun fromStoredFallsBackToSystemForNull() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStored(null))
    }

    @Test
    fun resolveDarkThemeFollowsSystemInSystemMode() {
        assertEquals(true, resolveDarkTheme(ThemeMode.SYSTEM, systemDark = true))
        assertEquals(false, resolveDarkTheme(ThemeMode.SYSTEM, systemDark = false))
    }

    @Test
    fun resolveDarkThemeHonorsExplicitModes() {
        assertEquals(false, resolveDarkTheme(ThemeMode.LIGHT, systemDark = true))
        assertEquals(true, resolveDarkTheme(ThemeMode.DARK, systemDark = false))
    }
}
