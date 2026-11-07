package com.primestrider.kongsi.feature.bill.ui.review

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kongsi.core.designsystem.theme.AppTheme
import com.primestrider.kongsi.feature.bill.model.BillItem
import com.primestrider.kongsi.feature.bill.model.ChargeType
import com.primestrider.kongsi.feature.bill.model.SplitIssue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class BillReviewScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val events = mutableListOf<String>()

    private val actions =
        BillReviewActions(
            onBack = { events += "back" },
            onTitleChange = {},
            onChargeChange = { _, _ -> },
            onRateChange = { _, _ -> },
            onAddItem = { events += "add" },
            onEditItem = { events += "edit $it" },
            onEditorChange = {},
            onSaveItem = { events += "save" },
            onDeleteItem = { events += "delete $it" },
            onDismissEditor = {},
            onWarningClick = { events += "warning ${it.focus}" },
            onFocusShown = { events += "focus shown" },
            onNext = { events += "next" },
        )

    private val items =
        listOf(
            BillItem(10, "Nasi Goreng", 30_000, sharedBy = emptySet()),
            BillItem(12, "Ocha", 12_000, sharedBy = emptySet(), quantity = 2, taxable = false),
        )

    private fun show(state: BillReviewUiState) = rule.setContent { AppTheme(false) { BillReviewScreen(state, actions) } }

    @Test
    fun items_show_their_quantity_price_and_what_they_are_not_charged() {
        show(BillReviewUiState(loaded = true, items = items, totalMinor = 42_000, canContinue = true))

        rule.onNodeWithText("Nasi Goreng").assertIsDisplayed()
        rule.onNodeWithText("2× Ocha").assertIsDisplayed()
        rule.onNodeWithText("no tax").assertIsDisplayed()
        rule.onNodeWithText("Ocha", substring = true).performClick()
        assertEquals(listOf("edit 12"), events)
    }

    @Test
    fun rate_warning_names_the_amounts_and_the_likely_cause_and_opens_it() {
        val warning =
            ReviewWarning(SplitIssue.RateMismatch(ChargeType.TAX, printedMinor = 13_860, calculatedMinor = 12_600), ReviewFocus.Item(12))
        show(
            BillReviewUiState(
                loaded = true,
                items = items,
                charges = BillReviewUiState().charges + (ChargeField.TAX to ChargeInput("13860", rate = "10")),
                warnings = listOf(warning),
                canContinue = true,
            ),
        )

        rule
            .onNodeWithText("Tax on the receipt is Rp13,860, but 10% of the items is Rp12,600. Is an item tax-free?")
            .performClick()
        rule.waitForIdle()
        assertEquals("warning Item(id=12)", events.first())
    }

    @Test
    fun blocking_warning_keeps_next_disabled() {
        show(
            BillReviewUiState(
                loaded = true,
                items = items,
                warnings = listOf(ReviewWarning(SplitIssue.NoEligibleItem(ChargeType.TAX), ReviewFocus.Item(10))),
                canContinue = false,
            ),
        )

        rule.onNodeWithText("There is tax, but every item is tax-free. Turn tax on for the items it is charged on.").assertIsDisplayed()
        rule.onNodeWithText("Next").assertIsNotEnabled()
    }

    @Test
    fun next_reports_the_tap_when_allowed() {
        show(BillReviewUiState(loaded = true, items = items, canContinue = true))

        rule.onNodeWithText("Next").assertIsEnabled().performClick()
        assertEquals(listOf("next"), events)
    }

    @Test
    fun empty_bill_invites_to_add_the_first_item() {
        show(BillReviewUiState(loaded = true))

        rule.onNodeWithText("Add what is on the receipt, one line at a time.").assertIsDisplayed()
        rule.onNodeWithText("Add item").performClick()
        assertEquals(listOf("add"), events)
    }

    @Test
    fun item_sheet_says_what_is_missing() {
        show(
            BillReviewUiState(
                loaded = true,
                editor = ItemEditor(name = "", price = "0", nameError = true, priceError = true),
            ),
        )

        rule.onNodeWithText("Type what the item is").assertIsDisplayed()
        rule.onNodeWithText("Type the price on the receipt").assertIsDisplayed()
        rule.onNodeWithText("Save").performScrollTo().performClick()
        assertEquals(listOf("save"), events)
    }

    @Test
    fun editing_an_existing_item_offers_delete() {
        show(BillReviewUiState(loaded = true, items = items, editor = ItemEditor(itemId = 12, name = "Ocha", price = "12000")))

        rule.onNodeWithText("Edit item").assertIsDisplayed()
        rule.onNodeWithText("Delete item").performScrollTo().performClick()
        assertEquals(listOf("delete 12"), events)
    }

    @Test
    fun a_warning_target_is_shown_then_acknowledged() {
        show(BillReviewUiState(loaded = true, items = items, focus = ReviewFocus.Item(12)))

        rule.mainClock.advanceTimeBy(3_000)
        rule.waitForIdle()
        assertEquals(listOf("focus shown"), events)
    }
}
