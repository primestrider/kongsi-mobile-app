package com.primestrider.kongsi.feature.bill.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
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
                )
            }
        }

    @Test
    fun empty_history_explains_the_steps() {
        show(BillHomeUiState())
        rule.onNodeWithText("No bills yet").assertIsDisplayed()
        rule.onNodeWithText("Send every friend their total on WhatsApp").assertIsDisplayed()
    }

    @Test
    fun account_hint_shows_only_without_accounts_and_opens_them() {
        show(BillHomeUiState(showAccountHint = true))
        rule.onNodeWithText("Where should friends pay you?").assertIsDisplayed()
        rule.onNodeWithText("Add account").performClick()
        assertEquals(listOf("accounts"), events)
    }

    @Test
    fun account_hint_is_hidden_once_an_account_exists() {
        show(BillHomeUiState(showAccountHint = false))
        rule.onNodeWithText("Where should friends pay you?").assertDoesNotExist()
    }
}
