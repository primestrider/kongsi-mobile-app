package com.primestrider.kotlinstarter.feature.example.components

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Demo pages keep their state across configuration changes (rotation) and process recreation. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class DemoStateRestorationTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun filter_chip_selection_survives_recreation() {
        val tester = StateRestorationTester(rule)
        tester.setContent { AppTheme(darkTheme = false) { ActionsScreen(onBack = {}) } }
        rule.onNodeWithText("Favorites").performScrollTo().performClick()
        tester.emulateSavedInstanceStateRestore()
        rule.onNodeWithText("Favorites").performScrollTo().assertIsSelected()
    }

    @Test
    fun dismissed_banner_stays_dismissed() {
        val tester = StateRestorationTester(rule)
        tester.setContent { AppTheme(darkTheme = false) { DisplayScreen(onBack = {}) } }
        rule.onNodeWithText("Heads up").performScrollTo()
        rule.onNodeWithContentDescription("Dismiss Heads up").performClick()
        tester.emulateSavedInstanceStateRestore()
        rule.onNodeWithText("Heads up").assertDoesNotExist()
    }

    @Test
    fun typed_text_survives_recreation() {
        val tester = StateRestorationTester(rule)
        tester.setContent { AppTheme(darkTheme = false) { InputsScreen(onBack = {}) } }
        rule.onNodeWithText("Full name").performScrollTo().performTextInput("Ada")
        tester.emulateSavedInstanceStateRestore()
        rule.onNodeWithText("Ada", substring = true).assertExists()
    }
}
