package com.primestrider.kotlinstarter.feature.example.sdk.appinfo

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class AppInfoScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val application = ApplicationProvider.getApplicationContext<Application>()

    @After
    fun tearDown() = shadowOf(application).checkActivities(false)

    @Test
    fun without_a_store_or_browser_the_store_button_shows_the_info_banner_instead_of_crashing() {
        // Unresolvable intents now throw ActivityNotFoundException, as on a device with neither app.
        shadowOf(application).checkActivities(true)
        rule.setContent { AppTheme(false) { AppInfoScreen(onBack = {}) } }

        rule.onNodeWithText("Open store page").performScrollTo().performClick()

        rule.onNodeWithText("In-app review is not available here.").performScrollTo().assertIsDisplayed()
    }
}
