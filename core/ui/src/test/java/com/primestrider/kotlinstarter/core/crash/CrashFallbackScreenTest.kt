package com.primestrider.kotlinstarter.core.crash

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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
class CrashFallbackScreenTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun shows_the_report_and_continues() {
        var continued = 0
        rule.setContent {
            AppTheme(false) {
                CrashFallbackScreen(CrashReport("java.lang.IllegalStateException", "boom", 0L), onContinue = { continued++ })
            }
        }
        rule.onNodeWithText("Something went wrong").assertExists()
        rule.onNodeWithText("IllegalStateException").assertExists()
        rule.onNodeWithText("Continue").performClick()
        assertEquals(1, continued)
    }
}
