package com.primestrider.kotlinstarter.core.i18n

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {
    @Test
    fun explicitAppLocaleWinsOverDevice() {
        assertEquals(AppLanguage.ID, resolveLanguage("id", "en-US"))
    }

    @Test
    fun fallsBackToDeviceLanguage() {
        assertEquals(AppLanguage.ID, resolveLanguage(null, "id-ID"))
    }

    @Test
    fun legacyInTagIsIndonesian() {
        assertEquals(AppLanguage.ID, resolveLanguage(null, "in-ID"))
    }

    @Test
    fun unsupportedDeviceLanguageFallsBackToEnglish() {
        assertEquals(AppLanguage.EN, resolveLanguage(null, "ja-JP"))
    }

    @Test
    fun emptyAppLocaleIsTreatedAsUnset() {
        assertEquals(AppLanguage.EN, resolveLanguage("", "ja-JP"))
    }
}
