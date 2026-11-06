package com.primestrider.kotlinstarter.feature.example.gestures

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class TapScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private fun show() = rule.setContent { AppTheme(false) { TapScreen(onBack = {}) } }

    @Test
    fun clickable_counts_taps() {
        show()
        rule.onNodeWithTag("clickTarget").performScrollTo().performClick()
        rule.onNodeWithText("Taps: 1").assertExists()
    }

    @Test
    fun combined_clickable_logs_click_long_press_and_double_tap() {
        show()
        val target = rule.onNodeWithTag("combinedTarget").performScrollTo()
        target.performClick()
        rule.mainClock.advanceTimeBy(1_000)
        target.performTouchInput { longClick() }
        rule.mainClock.advanceTimeBy(1_000)
        target.performTouchInput { doubleClick() }
        rule.waitForIdle()
        rule.onNodeWithText("Click").assertExists()
        rule.onNodeWithText("Long press").assertExists()
        rule.onNodeWithText("Double tap").assertExists()
    }

    @Test
    fun tap_marks_the_touch_position() {
        show()
        rule.onNodeWithTag("tapMark").assertDoesNotExist()
        rule.onNodeWithTag("tapArea").performScrollTo().performTouchInput { click(center) }
        // A single tap is reported once the double-tap window has passed.
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
        rule.onNodeWithTag("tapMark", useUnmergedTree = true).assertExists()
    }

    @Test
    fun long_press_target_has_a_custom_action() {
        show()
        rule.onNodeWithTag("combinedTarget").assert(SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick))
    }
}
