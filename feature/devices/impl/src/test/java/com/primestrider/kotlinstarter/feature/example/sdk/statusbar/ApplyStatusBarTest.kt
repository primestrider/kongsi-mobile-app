package com.primestrider.kotlinstarter.feature.example.sdk.statusbar

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.core.view.WindowCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class ApplyStatusBarTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun lightAppearance(): Boolean {
        val activity = rule.activity
        return WindowCompat.getInsetsController(activity.window, activity.window.decorView).isAppearanceLightStatusBars
    }

    @Test
    fun leaving_restores_the_app_look_for_the_current_dark_theme() {
        var present by mutableStateOf(true)
        rule.setContent { if (present) ApplyStatusBar(appearanceLight = true, hidden = false, darkTheme = true) }
        rule.waitForIdle()
        assertEquals(true, lightAppearance())

        present = false
        rule.waitForIdle()

        assertEquals(false, lightAppearance())
    }

    @Test
    fun the_applied_style_is_reapplied_when_the_theme_flips() {
        var dark by mutableStateOf(false)
        rule.setContent { ApplyStatusBar(appearanceLight = false, hidden = false, darkTheme = dark) }
        rule.waitForIdle()
        // Stands in for MainActivity's theme effect overwriting the bar.
        WindowCompat.getInsetsController(rule.activity.window, rule.activity.window.decorView).isAppearanceLightStatusBars = true

        dark = true
        rule.waitForIdle()

        assertEquals(false, lightAppearance())
    }
}
