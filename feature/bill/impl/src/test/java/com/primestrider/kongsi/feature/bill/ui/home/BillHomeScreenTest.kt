package com.primestrider.kongsi.feature.bill.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
                )
            }
        }

    private val loadedWithBills =
        BillHomeUiState(
            loaded = true,
            bills =
                listOf(
                    BillRow(id = 7, title = "Makan Malam Sushi", createdAt = 0, totalMinor = 152_460, peopleCount = 3),
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
    fun delete_button_names_the_bill_and_reports_it() {
        show(loadedWithBills)
        rule.onNodeWithContentDescription("Delete Makan Malam Sushi").performClick()
        assertEquals(listOf("delete 7"), events)
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
