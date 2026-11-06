package com.primestrider.kotlinstarter.feature.example.sdk.effects

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Below API 31 the blur and the AGSL mesh are not available, so the screen shows their fallback text. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class EffectsScreenTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun older_devices_see_both_fallback_captions() {
        rule.setContent { AppTheme(false) { EffectsScreen(onBack = {}) } }

        rule.onNodeWithText("Balance hidden").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Blur needs Android 12; showing a scrim instead.").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Mesh gradients need Android 13; showing a linear gradient instead.").performScrollTo().assertIsDisplayed()
    }
}
