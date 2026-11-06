package com.primestrider.kotlinstarter.feature.example.animation.transitions

import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class TransitionsScreenTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private var backs = 0

    /**
     * Stand-in for the app NavHost: a back callback registered before the screen's own (so with lower priority),
     * which counts the presses that would leave the page. The screen has no page-level handler of its own.
     */
    @Composable
    private fun Host() {
        AppTheme(false) {
            BackHandler { backs++ }
            TransitionsScreen(onBack = {})
        }
    }

    private fun show() = rule.setContent { Host() }

    private fun systemBack() {
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
    }

    private fun open(title: String) {
        rule.onNodeWithText(title).performScrollTo().performClick()
        rule.waitForIdle()
    }

    private fun detail(title: String) = rule.onNodeWithText("$title details", substring = true)

    private fun patternChip(label: String) = rule.onNode(hasText(label) and isSelectable())

    @Test
    fun tapping_an_item_opens_its_detail_and_back_button_returns() {
        show()
        detail("Mountain Trip").assertDoesNotExist()
        open("Mountain Trip")
        detail("Mountain Trip").assertExists()
        // The detail scrolls on its own, so bring the whole frame into the page viewport first.
        rule.onNodeWithTag("demoFrame").performScrollTo()
        rule.onNodeWithText("Back").performClick()
        rule.waitForIdle()
        detail("Mountain Trip").assertDoesNotExist()
        rule.onNodeWithText("City Lights").assertExists()
    }

    @Test
    fun system_back_pops_the_demo_detail_before_leaving_the_page() {
        show()
        open("Mountain Trip")
        systemBack()
        detail("Mountain Trip").assertDoesNotExist()
        rule.onNodeWithText("Mountain Trip").assertExists()
        assertEquals(0, backs)
        systemBack()
        assertEquals(1, backs)
    }

    @Test
    fun system_back_during_the_open_transition_pops_only_the_detail() {
        show()
        rule.onNodeWithText("Ocean Days").performScrollTo()
        rule.mainClock.autoAdvance = false
        rule.onNodeWithText("Ocean Days").performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        detail("Ocean Days").assertDoesNotExist()
        assertEquals(0, backs)
    }

    @Test
    fun double_tapping_an_item_opens_exactly_one_detail() {
        show()
        rule.onNodeWithText("Mountain Trip").performScrollTo()
        rule.mainClock.autoAdvance = false
        rule.onNodeWithText("Mountain Trip").performClick()
        rule.onNodeWithText("Mountain Trip").performClick()
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        detail("Mountain Trip").assertExists()
        systemBack()
        detail("Mountain Trip").assertDoesNotExist()
        assertEquals(0, backs)
    }

    @Test
    fun every_pattern_opens_and_closes_the_detail() {
        show()
        listOf("Container transform", "Shared axis X", "Shared axis Y", "Shared axis Z", "Fade through", "Fade").forEach {
            patternChip(it).performScrollTo().performClick()
            open("City Lights")
            detail("City Lights").assertExists()
            systemBack()
            detail("City Lights").assertDoesNotExist()
        }
        assertEquals(0, backs)
    }

    @Test
    fun selected_pattern_and_open_detail_survive_state_restoration() {
        val tester = StateRestorationTester(rule)
        tester.setContent { Host() }
        patternChip("Container transform").assertIsSelected()
        patternChip("Shared axis Z").performScrollTo().performClick()
        open("Mountain Trip")
        tester.emulateSavedInstanceStateRestore()
        patternChip("Shared axis Z").assertIsSelected()
        patternChip("Container transform").assertIsNotSelected()
        detail("Mountain Trip").assertExists()
        systemBack()
        detail("Mountain Trip").assertDoesNotExist()
        assertEquals(0, backs)
    }

    @Test
    fun predictive_back_preview_closes_with_system_back_before_the_page_leaves() {
        show()
        rule.onNodeWithText("Open preview").performScrollTo().performClick()
        rule.onNodeWithText("Swipe back from the edge", substring = true).assertExists()
        systemBack()
        rule.onNodeWithText("Swipe back from the edge", substring = true).assertDoesNotExist()
        assertEquals(0, backs)
    }

    @Test
    @Config(sdk = [33])
    fun predictive_back_example_shows_the_fallback_below_android_14() {
        show()
        rule.onNodeWithText("Predictive back needs Android 14.").performScrollTo().assertExists()
        rule.onNodeWithText("Open preview").performScrollTo().performClick()
        rule.onNodeWithText("Swipe back from the edge", substring = true).assertExists()
        systemBack()
        rule.onNodeWithText("Swipe back from the edge", substring = true).assertDoesNotExist()
        assertEquals(0, backs)
    }

    private fun gesture(progress: Float) = BackEventCompat(10f, 500f, progress, BackEventCompat.EDGE_LEFT)

    /** Starts a predictive back gesture and drags it halfway, as the system does while the finger moves. */
    private fun startGesture() {
        // Idle before and between events, as on a device: the panel's handler is registered and collecting
        // before the finger moves.
        rule.waitForIdle()
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.dispatchOnBackStarted(gesture(0f)) }
        rule.waitForIdle()
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.dispatchOnBackProgressed(gesture(0.5f)) }
        rule.waitForIdle()
    }

    private fun cancelGesture() {
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.dispatchOnBackCancelled() }
        rule.waitForIdle()
    }

    @Test
    fun cancelled_back_gesture_keeps_the_preview_and_a_committed_one_closes_it() {
        show()
        rule.onNodeWithText("Open preview").performScrollTo().performClick()
        startGesture()
        cancelGesture()
        rule.onNodeWithText("Swipe back from the edge", substring = true).assertExists()
        startGesture()
        systemBack()
        rule.onNodeWithText("Swipe back from the edge", substring = true).assertDoesNotExist()
        assertEquals(0, backs)
    }

    @Test
    fun cancelled_back_gesture_keeps_the_demo_detail_and_a_committed_one_pops_only_it() {
        show()
        open("City Lights")
        startGesture()
        cancelGesture()
        detail("City Lights").assertExists()
        startGesture()
        systemBack()
        detail("City Lights").assertDoesNotExist()
        rule.onNodeWithText("City Lights").assertExists()
        assertEquals(0, backs)
    }

    @Test
    fun pattern_chips_are_radio_buttons_with_a_selected_state() {
        show()
        val radio = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
        patternChip("Fade through").assert(radio).assertIsNotSelected().performClick()
        patternChip("Fade through").assert(radio).assertIsSelected()
        patternChip("Container transform").assert(radio).assertIsNotSelected()
    }
}
