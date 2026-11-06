package com.primestrider.kotlinstarter.feature.example.sdk.sqlite

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
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
class TransactionsScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val kinds = mutableListOf<TransactionKind>()
    private var added = 0

    private fun show(state: TransactionsUiState) =
        rule.setContent {
            AppTheme(false) {
                TransactionsContent(
                    state = state,
                    onKind = { kinds += it },
                    onAmount = {},
                    onAdd = { added++ },
                    onClear = {},
                    onBack = {},
                )
            }
        }

    @Test
    fun empty_history_says_so_once_loaded() {
        show(TransactionsUiState(loaded = true))
        rule.onNodeWithText("No transactions yet.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun empty_state_does_not_flash_before_the_database_answers() {
        show(TransactionsUiState(loaded = false))
        rule.onNodeWithText("No transactions yet.").assertDoesNotExist()
    }

    @Test
    fun rows_show_the_kind_and_a_signed_amount() {
        show(
            TransactionsUiState(
                items =
                    listOf(
                        TransactionEntity(2, "PAYMENT", -50_000, 2_000),
                        TransactionEntity(1, "TOP_UP", 100_000, 1_000),
                    ),
                balance = 50_000,
                loaded = true,
            ),
        )
        rule.onNodeWithText("Payment ·", substring = true).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Top-up ·", substring = true).assertExists()
        rule.onNodeWithText("+", substring = true).assertExists()
        rule.onNodeWithText("Balance", substring = true).assertExists()
    }

    @Test
    fun buttons_report_the_selection_and_add() {
        show(TransactionsUiState(loaded = true))
        rule.onNodeWithText("Payment").performScrollTo().performClick()
        rule.onNodeWithText("Add transaction").performScrollTo().performClick()
        assertEquals(listOf(TransactionKind.PAYMENT), kinds)
        assertEquals(1, added)
    }
}
