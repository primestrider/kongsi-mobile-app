package com.primestrider.kongsi.feature.bill.ui.accounts

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kongsi.core.designsystem.theme.AppTheme
import com.primestrider.kongsi.feature.bill.model.PaymentAccount
import com.primestrider.kongsi.feature.bill.model.PaymentAccountType
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class PaymentAccountScreensTest {
    @get:Rule
    val rule = createComposeRule()

    private val bca = PaymentAccount(1, PaymentAccountType.BANK, "BCA", "1234567890", "Ricky", isDefault = true)

    private val events = mutableListOf<String>()

    private fun showList(state: PaymentAccountsUiState) =
        rule.setContent {
            AppTheme(false) {
                PaymentAccountsScreen(
                    state = state,
                    onAdd = { events += "add" },
                    onEdit = { events += "edit $it" },
                    onDelete = { events += "delete $it" },
                    onBack = {},
                )
            }
        }

    private fun showForm(state: PaymentAccountEditUiState) =
        rule.setContent {
            AppTheme(false) {
                PaymentAccountEditScreen(
                    state = state,
                    actions =
                        PaymentAccountEditActions(
                            onTypeChange = { events += "type $it" },
                            onProviderChange = {},
                            onOtherProviderChange = {},
                            onNumberChange = {},
                            onUseMyNumber = { events += "my number" },
                            onHolderNameChange = {},
                            onDefaultChange = {},
                            onSave = { events += "save" },
                            onBack = {},
                        ),
                )
            }
        }

    @Test
    fun empty_list_offers_to_add_an_account() {
        showList(PaymentAccountsUiState(loaded = true))
        rule.onNodeWithText("No accounts yet").assertIsDisplayed()
        rule.onNodeWithText("Add account").performClick()
        assertEquals(listOf("add"), events)
    }

    @Test
    fun list_shows_account_and_tapping_edits_or_deletes_it() {
        showList(PaymentAccountsUiState(listOf(bca), loaded = true))
        rule.onNodeWithText("Default").assertIsDisplayed()
        rule.onNodeWithText("(Ricky)").assertIsDisplayed()
        rule.onNodeWithText("1234567890").performClick()
        rule.onNodeWithContentDescription("Delete BCA 1234567890").performClick()
        assertEquals(listOf("edit 1", "delete 1"), events)
    }

    @Test
    fun form_shows_errors_and_message_preview() {
        showForm(
            PaymentAccountEditUiState(
                isNew = true,
                loaded = true,
                type = PaymentAccountType.EWALLET,
                providerChoice = "GoPay",
                number = "12",
                holderName = "Ricky",
                submitted = true,
            ),
        )
        rule.onNodeWithText("Bank").performClick()
        rule.onNodeWithText("Registered phone number").assertIsDisplayed()
        rule.onNodeWithText("Enter an Indonesian mobile number, e.g. 0812 3456 7890").assertIsDisplayed()
        rule.onNodeWithText("GoPay (Ricky)\n12").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Save").performScrollTo().performClick()
        assertEquals(listOf("type BANK", "save"), events)
    }

    @Test
    fun ewallet_offers_the_number_from_profile() {
        showForm(
            PaymentAccountEditUiState(isNew = true, loaded = true, type = PaymentAccountType.EWALLET, myPhone = "081234567890"),
        )
        rule.onNodeWithText("Use my number (081234567890)").performClick()
        assertEquals(listOf("my number"), events)
    }

    @Test
    fun bank_does_not_offer_the_phone_number() {
        showForm(PaymentAccountEditUiState(isNew = true, loaded = true, myPhone = "081234567890"))
        rule.onNodeWithText("Use my number (081234567890)").assertDoesNotExist()
    }

    @Test
    fun other_provider_asks_for_the_name() {
        showForm(PaymentAccountEditUiState(isNew = false, loaded = true, providerChoice = OTHER_PROVIDER))
        rule.onNodeWithText("Edit account").assertIsDisplayed()
        rule.onNodeWithText("Bank name").assertIsDisplayed()
    }
}
