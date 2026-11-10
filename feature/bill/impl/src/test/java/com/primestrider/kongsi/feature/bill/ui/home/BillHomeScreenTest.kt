package com.primestrider.kongsi.feature.bill.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kongsi.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class BillHomeScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val events = mutableListOf<String>()

    private fun show(state: BillHomeUiState) =
        rule.setContent {
            AppTheme(false) {
                BillHomeScreen(
                    state = state,
                    onOpenPaymentAccounts = { events += "accounts" },
                    onDeleteBill = { events += "delete $it" },
                    onOpenBill = { events += "open $it" },
                    onRenameBill = { id, title -> events += "rename $id $title" },
                    onDuplicateBill = { events += "duplicate $it" },
                )
            }
        }

    private val loadedWithBills =
        BillHomeUiState(
            loaded = true,
            bills =
                listOf(
                    BillRow(
                        id = 7,
                        title = "Makan Malam Sushi",
                        createdAt = 0,
                        totalMinor = 152_460,
                        peopleCount = 3,
                        owingCount = 2,
                        paidCount = 1,
                    ),
                    BillRow(id = 8, title = "", createdAt = 0, totalMinor = 30_000, peopleCount = 1),
                ),
        )

    @Test
    fun empty_history_explains_the_steps() {
        show(BillHomeUiState(loaded = true))
        rule.onNodeWithText("No bills yet").assertIsDisplayed()
        rule.onNodeWithText("Send every friend their total on WhatsApp").assertIsDisplayed()
    }

    @Test
    fun nothing_flashes_before_the_database_answers() {
        show(BillHomeUiState(loaded = false))
        rule.onNodeWithText("No bills yet").assertDoesNotExist()
    }

    @Test
    fun bills_show_their_title_total_and_people() {
        show(loadedWithBills)
        rule.onNodeWithText("Makan Malam Sushi").assertIsDisplayed()
        rule.onNodeWithText("152,460", substring = true).assertIsDisplayed()
        rule.onNodeWithText("3 people", substring = true).assertIsDisplayed()
        rule.onNodeWithText("1 person", substring = true).assertIsDisplayed()
        // A bill without a title still has a name to read.
        rule.onNodeWithText("Untitled bill").assertIsDisplayed()
        rule.onNodeWithText("No bills yet").assertDoesNotExist()
    }

    @Test
    fun tapping_a_bill_opens_it() {
        show(loadedWithBills)
        rule.onNodeWithText("Makan Malam Sushi").performClick()
        assertEquals(listOf("open 7"), events)
    }

    @Test
    fun the_menu_of_a_bill_deletes_it() {
        show(loadedWithBills)
        rule.onNodeWithContentDescription("More for Makan Malam Sushi").performClick()
        rule.onNodeWithText("Delete").performClick()
        assertEquals(listOf("delete 7"), events)
    }

    @Test
    fun the_menu_of_a_bill_duplicates_it() {
        show(loadedWithBills)
        rule.onNodeWithContentDescription("More for Makan Malam Sushi").performClick()
        rule.onNodeWithText("Duplicate").performClick()
        assertEquals(listOf("duplicate 7"), events)
    }

    @Test
    fun renaming_starts_from_the_current_name() {
        show(loadedWithBills)
        rule.onNodeWithContentDescription("More for Makan Malam Sushi").performClick()
        rule.onNodeWithText("Rename").performClick()

        rule.onNode(hasSetTextAction()).assertTextContains("Makan Malam Sushi").performTextReplacement("Sushi Jumat")
        rule.onNodeWithText("Save").performClick()

        assertEquals(listOf("rename 7 Sushi Jumat"), events)
    }

    @Test
    fun a_bill_says_how_many_have_paid() {
        show(loadedWithBills)
        rule.onNodeWithText("1 of 2 paid", substring = true).assertIsDisplayed()
    }

    @Test
    fun a_bill_everyone_paid_says_so() {
        show(loadedWithBills.copy(bills = listOf(loadedWithBills.bills.first().copy(paidCount = 2))))
        rule.onNodeWithText("All paid", substring = true).assertIsDisplayed()
    }

    @Test
    fun account_hint_shows_only_without_accounts_and_opens_them() {
        show(BillHomeUiState(loaded = true, showAccountHint = true))
        rule.onNodeWithText("Where should friends pay you?").assertIsDisplayed()
        rule.onNodeWithText("Add account").performClick()
        assertEquals(listOf("accounts"), events)
    }

    @Test
    fun account_hint_is_hidden_once_an_account_exists() {
        show(BillHomeUiState(loaded = true, showAccountHint = false))
        rule.onNodeWithText("Where should friends pay you?").assertDoesNotExist()
    }
}
