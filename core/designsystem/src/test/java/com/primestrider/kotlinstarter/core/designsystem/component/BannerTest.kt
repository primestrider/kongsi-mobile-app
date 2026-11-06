package com.primestrider.kotlinstarter.core.designsystem.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class BannerTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun dismiss_calls_back() {
        var dismisses = 0
        rule.setContent { AppTheme(false) { Banner(title = "Saved", variant = BannerVariant.SUCCESS, onDismiss = { dismisses++ }) } }
        rule.onNodeWithContentDescription("Dismiss Saved").performClick()
        assertEquals(1, dismisses)
    }

    @Test
    fun long_title_keeps_dismiss_visible() {
        val title = "t".repeat(150)
        rule.setContent { AppTheme(false) { Banner(title = title, message = "body", onDismiss = {}) } }
        rule.onNodeWithContentDescription("Dismiss $title").assertIsDisplayed()
    }
}
