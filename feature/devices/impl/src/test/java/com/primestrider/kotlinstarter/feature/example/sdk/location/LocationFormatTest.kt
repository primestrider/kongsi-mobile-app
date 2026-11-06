package com.primestrider.kotlinstarter.feature.example.sdk.location

import com.google.android.gms.location.Priority
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class LocationFormatTest {
    @Test
    fun coordinate_has_six_decimals() {
        assertEquals("-6.200000", formatCoordinate(-6.2))
        assertEquals("106.816666", formatCoordinate(106.8166664))
    }

    @Test
    fun coordinate_uses_a_dot_in_any_locale() {
        val saved = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("id-ID"))
            assertEquals("-6.200000", formatCoordinate(-6.2))
        } finally {
            Locale.setDefault(saved)
        }
    }

    @Test
    fun high_accuracy_degrades_to_balanced_without_the_fine_permission() {
        assertEquals(Priority.PRIORITY_BALANCED_POWER_ACCURACY, effectivePriority(Priority.PRIORITY_HIGH_ACCURACY, fineGranted = false))
        assertEquals(Priority.PRIORITY_HIGH_ACCURACY, effectivePriority(Priority.PRIORITY_HIGH_ACCURACY, fineGranted = true))
        assertEquals(Priority.PRIORITY_LOW_POWER, effectivePriority(Priority.PRIORITY_LOW_POWER, fineGranted = false))
    }

    @Test
    fun accuracy_is_rounded_meters() {
        assertEquals("±13 m", formatAccuracy(12.6f))
        assertEquals("±5 m", formatAccuracy(4.9f))
    }
}
