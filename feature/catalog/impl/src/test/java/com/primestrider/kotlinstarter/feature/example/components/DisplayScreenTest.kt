package com.primestrider.kotlinstarter.feature.example.components

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class DisplayScreenTest {
    @get:Rule
    val rule = createComposeRule()

    @Before
    fun setUp() {
        rule.setContent { AppTheme(darkTheme = false) { DisplayScreen(onBack = {}) } }
    }

    @Test
    fun progress_is_clamped() {
        val bar = rule.onNodeWithTag(PROGRESS_BAR_TAG).performScrollTo()
        repeat(10) { rule.onNodeWithText("−10%").performClick() }
        bar.assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..1f))
        repeat(11) { rule.onNodeWithText("+10%").performClick() }
        bar.assertRangeInfoEquals(ProgressBarRangeInfo(1f, 0f..1f))
    }

    @Test
    fun long_headline_keeps_trailing_switch_visible() {
        rule.onNodeWithText(LONG_HEADLINE).performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag(LONG_ROW_SWITCH_TAG).assertIsDisplayed()
    }

    @Test
    fun closing_banner_removes_it() {
        rule.onNodeWithText("Heads up").performScrollTo()
        rule.onNodeWithContentDescription("Dismiss Heads up").performClick()
        rule.onNodeWithText("Heads up").assertDoesNotExist()
    }

    @Test
    fun skeleton_toggle_label_follows_state() {
        rule.onNodeWithText("Show content").performScrollTo().performClick()
        rule.onNodeWithText("Show skeleton").assertExists()
        rule.onNodeWithText("Show skeleton").performClick()
        rule.onNodeWithText("Show content").assertExists()
    }

    @Test
    fun badge_caps_at_99_plus() {
        rule.onNodeWithText("99+").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun pull_to_refresh_updates_the_list() {
        rule.onNodeWithText("Refreshed 0×").performScrollTo().assertExists()
        rule.onNodeWithTag("pullRefreshList").performScrollTo().performTouchInput { swipeDown() }
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
        rule.onNodeWithText("Refreshed 1×").assertExists()
    }

    @Test
    fun refresh_button_is_the_accessible_alternative() {
        rule.onNodeWithText("Refresh list").performScrollTo().performClick()
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
        rule.onNodeWithText("Refreshed 1×").assertExists()
    }

    @Test
    fun swipe_to_dismiss_removes_row_and_undo_restores() {
        rule.onNodeWithTag("dismissRow-1").performScrollTo().performTouchInput { swipeLeft() }
        rule.waitForIdle()
        rule.onNodeWithText("Message 1").assertDoesNotExist()
        rule.onNodeWithText("Undo").performScrollTo().performClick()
        rule.onNodeWithText("Message 1").assertExists()
        rule.onNodeWithContentDescription("Delete Message 2").performScrollTo().performClick()
        rule.onNodeWithText("Message 2").assertDoesNotExist()
    }
}
