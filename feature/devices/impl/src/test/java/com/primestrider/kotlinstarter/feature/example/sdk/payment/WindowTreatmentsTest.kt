package com.primestrider.kotlinstarter.feature.example.sdk.payment

import android.app.Activity
import android.view.WindowManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class WindowTreatmentsTest {
    private val window =
        Robolectric
            .buildActivity(Activity::class.java)
            .setup()
            .get()
            .window

    private fun has(flag: Int) = window.attributes.flags and flag != 0

    @Test
    fun apply_sets_all_three_and_restore_undoes_them() {
        val before = window.attributes.screenBrightness
        val treatments = WindowTreatments(window)

        treatments.apply(brightness = true, keepOn = true, secure = true)
        assertEquals(1f, window.attributes.screenBrightness, 0f)
        assertTrue(has(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON))
        assertTrue(has(WindowManager.LayoutParams.FLAG_SECURE))

        treatments.restore()
        assertEquals(before, window.attributes.screenBrightness, 0f)
        assertFalse(has(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON))
        assertFalse(has(WindowManager.LayoutParams.FLAG_SECURE))
    }

    @Test
    fun restore_keeps_flags_the_window_already_had() {
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.attributes = window.attributes.apply { screenBrightness = 0.3f }
        val treatments = WindowTreatments(window)

        treatments.apply(brightness = true, keepOn = true, secure = true)
        treatments.apply(brightness = true, keepOn = false, secure = false)
        treatments.restore()

        assertEquals(0.3f, window.attributes.screenBrightness, 0f)
        assertTrue(has(WindowManager.LayoutParams.FLAG_SECURE))
        assertTrue(has(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON))
    }

    @Test
    fun switching_one_off_undoes_only_that_one() {
        val treatments = WindowTreatments(window)
        treatments.apply(brightness = true, keepOn = true, secure = true)

        treatments.apply(brightness = true, keepOn = true, secure = false)

        assertFalse(has(WindowManager.LayoutParams.FLAG_SECURE))
        assertTrue(has(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON))
        assertEquals(1f, window.attributes.screenBrightness, 0f)
    }

    @Test
    fun restore_twice_is_harmless() {
        val before = window.attributes.screenBrightness
        val treatments = WindowTreatments(window)
        treatments.apply(brightness = true, keepOn = false, secure = false)
        treatments.restore()
        treatments.restore()
        assertEquals(before, window.attributes.screenBrightness, 0f)
    }
}
