package com.primestrider.kotlinstarter.feature.example.gestures

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class SwipeScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private fun show() = rule.setContent { AppTheme(false) { SwipeScreen(onBack = {}) } }

    @Test
    fun swiping_moves_between_menus() {
        show()
        rule.onNodeWithText("Fried rice, noodles, satay").assertIsDisplayed()
        val pager = rule.onNodeWithTag("menuPager").performScrollTo()
        pager.performTouchInput { swipeLeft() }
        rule.waitForIdle()
        rule.onNodeWithText("Iced tea, coffee, juice").assertIsDisplayed()
        rule.onNode(hasText("Drinks") and hasAnyAncestor(hasTestTag("menuTabs"))).assertIsSelected()
        pager.performTouchInput { swipeLeft() }
        rule.waitForIdle()
        rule.onNodeWithText("Pudding, ice cream, cake").assertIsDisplayed()
        pager.performTouchInput { swipeRight() }
        rule.waitForIdle()
        rule.onNodeWithText("Iced tea, coffee, juice").assertIsDisplayed()
    }

    @Test
    fun tapping_a_menu_tab_scrolls_the_pager() {
        show()
        rule.onNode(hasText("Desserts") and hasAnyAncestor(hasTestTag("menuTabs"))).performScrollTo().performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Pudding, ice cream, cake").assertIsDisplayed()
    }

    @Test
    fun menu_page_survives_recreation() {
        val restore = StateRestorationTester(rule)
        restore.setContent { AppTheme(false) { SwipeScreen(onBack = {}) } }
        rule.onNodeWithTag("menuPager").performScrollTo().performTouchInput { swipeLeft() }
        rule.waitForIdle()
        restore.emulateSavedInstanceStateRestore()
        rule.onNodeWithText("Iced tea, coffee, juice").assertIsDisplayed()
    }

    @Test
    fun swipe_row_reveals_actions() {
        show()
        rule.onNodeWithText("Actions: hidden").assertExists()
        rule.onNodeWithTag("revealRow").performScrollTo().performTouchInput { swipeLeft() }
        rule.waitForIdle()
        rule.onNodeWithText("Actions: shown").assertExists()
        rule.onNodeWithText("Archive").assertIsDisplayed()
    }

    @Test
    fun reveal_row_has_a_custom_action() {
        show()
        val node = rule.onNodeWithTag("revealRow").performScrollTo().fetchSemanticsNode()
        val action = node.config[SemanticsActions.CustomActions].single()
        org.junit.Assert.assertEquals("Show actions", action.label)
        rule.runOnIdle { action.action() }
        rule.waitForIdle()
        rule.onNodeWithText("Actions: shown").assertExists()
    }

    @Test
    fun card_stack_swipe_advances() {
        show()
        rule.onNodeWithText("Card 1").assertExists()
        rule.onNodeWithTag("cardStack").performScrollTo().performTouchInput { swipeRight() }
        rule.waitForIdle()
        rule.onNodeWithText("Card 2").assertExists()
        rule.onNodeWithText("Card 1").assertDoesNotExist()
        rule.onNodeWithContentDescription("Skip").performScrollTo().performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Card 3").assertExists()
        rule.onNodeWithText("Card 2").assertDoesNotExist()
    }

    @Test
    fun stack_end_offers_restart() {
        show()
        repeat(5) {
            rule.onNodeWithContentDescription("Keep").performScrollTo().performClick()
            rule.waitForIdle()
        }
        rule.onNodeWithText("Start over").performScrollTo().performClick()
        rule.onNodeWithText("Card 1").assertExists()
    }

    @Test
    fun fling_moves_the_puck() {
        show()
        rule.onNodeWithText("Position: 0").assertExists()
        rule.onNodeWithTag("flingArea").performScrollTo().performTouchInput { swipeRight(durationMillis = 100) }
        rule.waitForIdle()
        rule.onNodeWithText("Position: 0").assertDoesNotExist()
    }

    @Test
    fun stack_position_survives_recreation() {
        val restore = StateRestorationTester(rule)
        restore.setContent { AppTheme(false) { SwipeScreen(onBack = {}) } }
        rule.onNodeWithContentDescription("Keep").performScrollTo().performClick()
        rule.waitForIdle()
        restore.emulateSavedInstanceStateRestore()
        rule.onNodeWithText("Card 2").assertExists()
    }

    @Test
    fun a_short_fast_flick_sends_the_card_off() {
        show()
        val stack = rule.onNodeWithTag("cardStack").performScrollTo()
        stack.performTouchInput { down(center) }
        // Three quick moves, one per frame, well short of 40% of the width: only the speed can send the card off.
        repeat(3) {
            stack.performTouchInput { moveBy(Offset(30f, 0f), delayMillis = 0) }
            rule.mainClock.advanceTimeByFrame()
        }
        stack.performTouchInput { up() }
        rule.waitForIdle()
        rule.onNodeWithText("Card 2").assertExists()
    }
}
