package com.primestrider.kotlinstarter.feature.example.animation.cards

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
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
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class CardsScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private fun show() = rule.setContent { AppTheme(false) { CardsScreen(onBack = {}) } }

    private fun archive(
        title: String,
        letSnackbarTimeOut: Boolean = true,
    ) {
        rule.onNodeWithContentDescription("Archive $title").performScrollTo().performClick()
        // A timed-out snackbar would otherwise sit over the next button.
        if (letSnackbarTimeOut) rule.mainClock.advanceTimeBy(11_000)
        rule.waitForIdle()
    }

    /** The title also appears in the carousel, so the swipe list is addressed by its tag. */
    private fun listCard(title: String) = rule.onNode(hasText(title) and hasAnyAncestor(hasTestTag("swipeList")))

    @Test
    fun archiving_a_card_removes_it_and_undo_brings_it_back() {
        show()
        listCard("Mountain Trip").performScrollTo().assertExists()
        archive("Mountain Trip", letSnackbarTimeOut = false)
        listCard("Mountain Trip").assertDoesNotExist()
        rule.onNodeWithText("Undo").performClick()
        listCard("Mountain Trip").performScrollTo().assertExists()
    }

    @Test
    fun reset_dismisses_a_showing_undo_snackbar() {
        show()
        archive("Mountain Trip", letSnackbarTimeOut = false)
        rule.onNodeWithText("Undo").assertExists()
        rule.onNodeWithText("Reset").performScrollTo().performClick()
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(1_000)
        rule.onNodeWithText("Undo").assertDoesNotExist()
    }

    @Test
    fun swiping_a_card_archives_it() {
        show()
        listCard("Ocean Days").performScrollTo().performTouchInput { swipeLeft() }
        rule.waitForIdle()
        listCard("Ocean Days").assertDoesNotExist()
        listCard("City Lights").assertExists()
    }

    @Test
    fun archiving_every_card_shows_the_empty_state_and_reset_restores() {
        show()
        listOf("Mountain Trip", "City Lights", "Ocean Days", "Forest Walk", "Desert Road").forEach { archive(it) }
        rule.onNodeWithText("All cards archived").performScrollTo().assertExists()
        rule.onNodeWithText("Reset").performScrollTo().performClick()
        rule.onNodeWithText("All cards archived").assertDoesNotExist()
        listCard("Desert Road").performScrollTo().assertExists()
    }

    @Test
    fun expand_toggles_the_extra_text() {
        show()
        rule.onNodeWithText("Expanded details", substring = true).assertDoesNotExist()
        rule.onNodeWithText("Trip details").performScrollTo().performClick()
        rule.onNodeWithText("Expanded details", substring = true).assertExists()
        rule.onNodeWithText("Trip details").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Expanded details", substring = true).assertDoesNotExist()
    }

    @Test
    fun flip_button_shows_the_back_face() {
        show()
        rule.onNodeWithText("Flip card back").assertDoesNotExist()
        rule.onNodeWithText("Flip").performScrollTo().performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Flip card back").performScrollTo().assertExists()
        rule.onNodeWithText("Flip card front").assertDoesNotExist()
    }

    @Test
    fun expanded_flipped_and_archived_state_survives_recreation() {
        val tester = StateRestorationTester(rule)
        tester.setContent { AppTheme(false) { CardsScreen(onBack = {}) } }
        rule.onNodeWithText("Trip details").performScrollTo().performClick()
        rule.onNodeWithText("Flip").performScrollTo().performClick()
        archive("City Lights")
        tester.emulateSavedInstanceStateRestore()
        rule.onNodeWithText("Expanded details", substring = true).performScrollTo().assertExists()
        rule.onNodeWithText("Flip card back").performScrollTo().assertExists()
        listCard("City Lights").assertDoesNotExist()
        listCard("Mountain Trip").performScrollTo().assertExists()
    }

    private fun hasState(text: String) = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, text)

    @Test
    fun expand_card_is_a_toggle_with_a_state_description() {
        show()
        rule
            .onNodeWithTag("expandCard")
            .performScrollTo()
            .assertIsOff()
            .assert(hasState("Collapsed"))
        rule.onNodeWithTag("expandCard").performClick()
        rule.onNodeWithTag("expandCard").assertIsOn().assert(hasState("Expanded"))
    }

    @Test
    fun flip_card_is_a_toggle_with_a_state_description() {
        show()
        rule
            .onNodeWithTag("flipCard")
            .performScrollTo()
            .assertIsOff()
            .assert(hasState("Showing the front"))
        rule.onNodeWithTag("flipCard").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("flipCard").assertIsOn().assert(hasState("Showing the back"))
        rule.onNodeWithText("Flip card back").assertExists()
    }

    @Test
    fun carousel_position_follows_the_page() {
        show()
        rule.onNodeWithText("Card 1 of 5").performScrollTo().assertExists()
        rule.onNodeWithTag("carousel").performScrollToIndex(1)
        rule.waitForIdle()
        rule.onNodeWithText("Card 2 of 5").assertExists()
    }

    @Test
    fun press_counts_each_press() {
        show()
        rule.onNodeWithText("Pressed 0 times").assertExists()
        rule.onNodeWithText("Press and hold me").performClick()
        rule.onNodeWithText("Pressed 1 time").assertExists()
    }

    @Test
    fun undo_restores_the_latest_archive_and_reset_restores_the_rest() {
        show()
        archive("Mountain Trip")
        archive("City Lights", letSnackbarTimeOut = false)
        listCard("Mountain Trip").assertDoesNotExist()
        listCard("City Lights").assertDoesNotExist()
        rule.onNodeWithText("Undo").performClick()
        listCard("City Lights").performScrollTo().assertExists()
        listCard("Mountain Trip").assertDoesNotExist()
        rule.onNodeWithText("Reset").performScrollTo().performClick()
        listCard("Mountain Trip").performScrollTo().assertExists()
        listCard("City Lights").assertExists()
    }

    @Test
    fun undo_still_works_after_state_restoration() {
        val tester = StateRestorationTester(rule)
        tester.setContent { AppTheme(false) { CardsScreen(onBack = {}) } }
        archive("City Lights", letSnackbarTimeOut = false)
        tester.emulateSavedInstanceStateRestore()
        listCard("City Lights").assertDoesNotExist()
        rule.onNodeWithText("Undo").performClick()
        listCard("City Lights").performScrollTo().assertExists()
    }
}
