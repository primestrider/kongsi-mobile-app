package com.primestrider.kotlinstarter.feature.example.sdk.payment

import android.view.Window
import android.view.WindowManager

/**
 * Brightness, keep-screen-on and secure (screenshot-proof) treatments of one [Window]. Each is undone by
 * switching it off or by [restore], and only what this object changed is undone: a flag the window already
 * had before the first [apply] stays set, and brightness returns to the value it had before it was raised.
 */
class WindowTreatments(
    private val window: Window,
) {
    private var originalBrightness: Float? = null
    private var setKeepOn = false
    private var setSecure = false

    fun apply(
        brightness: Boolean,
        keepOn: Boolean,
        secure: Boolean,
    ) {
        applyBrightness(brightness)
        setKeepOn = applyFlag(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, keepOn, setKeepOn)
        setSecure = applyFlag(WindowManager.LayoutParams.FLAG_SECURE, secure, setSecure)
    }

    /** Puts back everything [apply] changed. Safe to call more than once. */
    fun restore() = apply(brightness = false, keepOn = false, secure = false)

    private fun applyBrightness(on: Boolean) {
        val original = originalBrightness
        if (on) {
            if (original == null) originalBrightness = window.attributes.screenBrightness
            window.attributes = window.attributes.apply { screenBrightness = MAX_BRIGHTNESS }
        } else if (original != null) {
            window.attributes = window.attributes.apply { screenBrightness = original }
            originalBrightness = null
        }
    }

    /** Sets or clears [flag]; returns whether this object now owns it (it set it itself). */
    private fun applyFlag(
        flag: Int,
        on: Boolean,
        owned: Boolean,
    ): Boolean =
        when {
            on && !owned && window.attributes.flags and flag == 0 -> {
                window.addFlags(flag)
                true
            }
            !on && owned -> {
                window.clearFlags(flag)
                false
            }
            else -> owned
        }

    private companion object {
        const val MAX_BRIGHTNESS = 1f
    }
}
