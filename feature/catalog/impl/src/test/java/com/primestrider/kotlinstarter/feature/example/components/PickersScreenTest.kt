package com.primestrider.kotlinstarter.feature.example.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class PickersScreenTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        rule.setContent { AppTheme(darkTheme = false) { PickersScreen(onBack = {}) } }
    }

    @Test
    fun inline_date_picker_renders() {
        rule.onNodeWithTag("inlineDatePicker").performScrollTo().assertExists()
    }

    @Test
    fun date_dialog_cancel_keeps_previous_value() {
        rule.onNodeWithText("Selected: none").performScrollTo().assertExists()
        rule.onNodeWithText("Pick a date").performScrollTo().performClick()
        rule.onNode(isDialog()).assertExists()
        rule.onNodeWithText("Cancel").performClick()
        rule.onNode(isDialog()).assertDoesNotExist()
        rule.onNodeWithText("Selected: none").assertExists()
    }

    @Test
    fun date_dialog_confirm_without_a_choice_keeps_none() {
        rule.onNodeWithText("Pick a date").performScrollTo().performClick()
        rule.onNodeWithText("OK").performClick()
        rule.onNodeWithText("Selected: none").assertExists()
    }

    @Test
    fun time_dialog_confirm_shows_time() {
        rule.onNodeWithText("Time: none").performScrollTo().assertExists()
        rule.onNodeWithText("Pick a time").performScrollTo().performClick()
        rule.onNodeWithText("OK").performClick()
        rule.onNode(isDialog()).assertDoesNotExist()
        rule.onNodeWithText("Time: none").assertDoesNotExist()
        rule.onNodeWithText("Time: ", substring = true).assertExists()
    }

    @Test
    fun range_picker_and_time_input_render() {
        rule.onNodeWithTag("rangePicker").performScrollTo().assertExists()
        rule.onNodeWithTag("timeInput").performScrollTo().assertExists()
    }
}
