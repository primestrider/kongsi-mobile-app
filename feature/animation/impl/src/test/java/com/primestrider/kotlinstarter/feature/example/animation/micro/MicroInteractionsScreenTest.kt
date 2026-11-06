package com.primestrider.kotlinstarter.feature.example.animation.micro

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class MicroInteractionsScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private fun click(description: String) {
        rule.onNodeWithContentDescription(description).performScrollTo().performClick()
    }

    private fun tab(text: String) = rule.onNodeWithText(text).performScrollTo()

    @Test
    fun like_toggles_its_state() {
        rule.setContent { AppTheme(false) { MicroInteractionsScreen(onBack = {}) } }
        rule.onNodeWithContentDescription("Like").performScrollTo().assertIsOff()
        click("Like")
        rule.onNodeWithContentDescription("Like").assertIsOn()
        click("Like")
        rule.onNodeWithContentDescription("Like").assertIsOff()
    }

    @Test
    fun like_spam_with_an_odd_number_of_taps_ends_liked_and_settles() {
        rule.setContent { AppTheme(false) { MicroInteractionsScreen(onBack = {}) } }
        rule.onNodeWithContentDescription("Like").performScrollTo()
        rule.mainClock.autoAdvance = false
        repeat(7) { click("Like") }
        rule.mainClock.autoAdvance = true
        rule.waitForIdle() // returns only once the spring animations have settled
        rule.onNodeWithContentDescription("Like").assertIsOn()
    }

    @Test
    fun counter_is_exact_after_rapid_taps() {
        rule.setContent { AppTheme(false) { MicroInteractionsScreen(onBack = {}) } }
        rule.onNodeWithContentDescription("Increase").performScrollTo()
        rule.mainClock.autoAdvance = false
        repeat(5) { click("Increase") }
        repeat(2) { click("Decrease") }
        rule.mainClock.autoAdvance = true
        rule.onNodeWithText("3").performScrollTo().assertExists()
    }

    @Test
    fun content_change_cycles_pending_paid_shipped() {
        rule.setContent { AppTheme(false) { MicroInteractionsScreen(onBack = {}) } }
        rule.onNodeWithText("Pending").performScrollTo().assertExists()
        click("Next status")
        rule.onNodeWithText("Paid").performScrollTo().assertExists()
        click("Next status")
        rule.onNodeWithText("Shipped").performScrollTo().assertExists()
        click("Next status")
        rule.onNodeWithText("Pending").performScrollTo().assertExists()
    }

    @Test
    fun selected_tab_is_exposed_to_accessibility() {
        rule.setContent { AppTheme(false) { MicroInteractionsScreen(onBack = {}) } }
        tab("Photos").assertIsSelected()
        tab("Videos").assertIsNotSelected()
        tab("Albums").assertIsNotSelected()
        tab("Videos").performClick()
        tab("Photos").assertIsNotSelected()
        tab("Videos").assertIsSelected()
    }

    private fun assertIndicatorUnder(text: String) {
        rule.waitForIdle()
        val tab = rule.onNode(hasText(text) and isSelectable()).getBoundsInRoot()
        val bar = rule.onNodeWithTag("tabIndicator").getBoundsInRoot()
        assertTrue("$bar not inside $tab", bar.left >= tab.left - 1.dp && bar.right <= tab.right + 1.dp && bar.right > bar.left)
    }

    private fun indicatorFollowsSelection(direction: LayoutDirection) {
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                AppTheme(false) { MicroInteractionsScreen(onBack = {}) }
            }
        }
        tab("Photos")
        assertIndicatorUnder("Photos")
        tab("Albums").performClick()
        assertIndicatorUnder("Albums")
        tab("Videos").performClick()
        assertIndicatorUnder("Videos")
    }

    @Test
    fun indicator_sits_under_the_selected_tab_in_ltr() = indicatorFollowsSelection(LayoutDirection.Ltr)

    @Test
    fun indicator_sits_under_the_selected_tab_in_rtl() = indicatorFollowsSelection(LayoutDirection.Rtl)

    @Test
    fun state_survives_recreation() {
        val tester = StateRestorationTester(rule)
        tester.setContent { AppTheme(false) { MicroInteractionsScreen(onBack = {}) } }
        click("Like")
        repeat(4) { click("Increase") }
        click("Next status")
        tab("Albums").performClick()
        tester.emulateSavedInstanceStateRestore()
        rule.onNodeWithContentDescription("Like").performScrollTo().assertIsOn()
        rule.onNodeWithText("4").performScrollTo().assertExists()
        rule.onNodeWithText("Paid").performScrollTo().assertExists()
        tab("Albums").assertIsSelected()
        tab("Photos").assertIsNotSelected()
    }
}
