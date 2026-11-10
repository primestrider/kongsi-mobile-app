package com.primestrider.kongsi.feature.bill.ui.flow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kongsi.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class FlowChromeTest {
    @get:Rule
    val rule = createComposeRule()

    private fun show(content: @Composable () -> Unit) = rule.setContent { AppTheme(false) { content() } }

    @Test
    fun total_that_matches_the_receipt_says_so() {
        show { RunningTotalBar(RunningTotal.Receipt(totalMinor = 152_460, receiptMinor = 152_460)) }
        rule.onNodeWithText("152,460").assertIsDisplayed()
        rule.onNodeWithText("Matches the receipt").assertIsDisplayed()
    }

    @Test
    fun total_that_does_not_match_the_receipt_names_both_amounts() {
        show { RunningTotalBar(RunningTotal.Receipt(totalMinor = 150_000, receiptMinor = 152_460)) }
        rule.onNodeWithText("150,000").assertIsDisplayed()
        rule.onNodeWithText("Receipt says 152,460").assertIsDisplayed()
    }

    @Test
    fun total_without_a_receipt_total_is_just_the_total() {
        show { RunningTotalBar(RunningTotal.Receipt(totalMinor = 30_000, receiptMinor = null)) }
        rule.onNodeWithText("30,000").assertIsDisplayed()
        rule.onNodeWithText("Matches the receipt").assertDoesNotExist()
    }

    @Test
    fun partly_assigned_shows_progress() {
        show { RunningTotalBar(RunningTotal.Assigned(assignedMinor = 96_000, totalMinor = 152_460)) }
        rule.onNodeWithText("96,000 of 152,460 assigned").assertIsDisplayed()
        rule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo(96_000f / 152_460f, 0f..1f)),
            )
    }

    @Test
    fun fully_assigned_says_everything_adds_up() {
        show { RunningTotalBar(RunningTotal.Shares(totalMinor = 152_460)) }
        rule.onNodeWithText("Everyone's share adds up to 152,460").assertIsDisplayed()
    }

    @Test
    fun action_button_reports_the_tap() {
        var taps = 0
        show { RunningTotalBar(RunningTotal.Shares(totalMinor = 1), actionLabel = "Next", onAction = { taps++ }) }
        rule.onNodeWithText("Next").performClick()
        assertEquals(1, taps)
    }

    @Test
    fun earlier_steps_can_be_reopened_and_later_ones_cannot() {
        val opened = mutableListOf<FlowStep>()
        show { StepIndicator(current = FlowStep.ASSIGN, onStepClick = { opened += it }) }

        rule.onNodeWithText("Assign").assertIsSelected()
        rule.onNodeWithText("Items").assertHasClickAction().performClick()
        rule.onNodeWithText("Summary").assertHasNoClickAction()
        assertEquals(listOf(FlowStep.ITEMS), opened)
    }

    @Test
    fun shorthand_amount_explains_what_it_means() {
        var value by mutableStateOf("")
        show { MoneyField(value = value, onValueChange = { value = it }, label = "Price") }

        rule.onNodeWithText("Price").performTextInput("45rb")

        rule.onNodeWithText("= 45,000").assertIsDisplayed()
    }

    @Test
    fun amount_that_cannot_be_read_says_how_to_type_it() {
        var value by mutableStateOf("")
        show { MoneyField(value = value, onValueChange = { value = it }, label = "Price") }

        rule.onNodeWithText("Price").performTextInput("abc")

        rule.onNodeWithText("Type an amount like 45.000 or 45rb").assertIsDisplayed()
    }
}
