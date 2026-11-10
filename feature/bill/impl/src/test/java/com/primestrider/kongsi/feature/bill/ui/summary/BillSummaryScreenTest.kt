package com.primestrider.kongsi.feature.bill.ui.summary

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kongsi.core.designsystem.theme.AppTheme
import com.primestrider.kongsi.feature.bill.model.PaymentAccount
import com.primestrider.kongsi.feature.bill.model.PaymentAccountType
import com.primestrider.kongsi.feature.bill.model.Person
import com.primestrider.kongsi.feature.bill.ui.people.PersonRow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "w400dp-h800dp")
class BillSummaryScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val events = mutableListOf<String>()

    private val actions =
        BillSummaryActions(
            onBack = {},
            onStepClick = { events += "step $it" },
            onOpenBreakdown = { events += "open $it" },
            onCloseBreakdown = { events += "close" },
            onToggleAccount = { events += "account $it" },
            onAddAccount = { events += "add account" },
            onDone = { events += "done" },
        )

    private val me = PersonRow(Person(1, "Ricky"), index = 0, label = "Ricky", isMe = true)
    private val budi = PersonRow(Person(2, "Budi"), index = 1, label = "Budi", isMe = false)
    private val ani = PersonRow(Person(3, "Ani"), index = 2, label = "Ani", isMe = false)

    private val budiLines =
        listOf(
            BreakdownLine.Item("Nasi Goreng", 1, emptyList(), 1, 30_000),
            BreakdownLine.Item("Ocha", 1, listOf(ani), 2, 6_000),
            BreakdownLine.Service(500, 1_800),
            BreakdownLine.Tax(1_000, onService = true, 3_780),
        )

    private val state =
        BillSummaryUiState(
            loaded = true,
            people =
                listOf(
                    SummaryPerson(me, 0, emptyList()),
                    SummaryPerson(budi, 41_580, budiLines),
                    SummaryPerson(ani, 110_880, listOf(BreakdownLine.Item("Steak", 1, emptyList(), 1, 90_000))),
                ),
            totalMinor = 152_460,
            assignedMinor = 152_460,
            complete = true,
        )

    private fun show(state: BillSummaryUiState) = rule.setContent { AppTheme(false) { BillSummaryScreen(state, actions) } }

    @Test
    fun totals_add_up_and_done_finishes_the_bill() {
        show(state)

        rule.onNodeWithText("Everyone's share adds up to Rp152,460").assertIsDisplayed()
        rule.onNodeWithText("Done").assertIsEnabled().performClick()
        assertEquals(listOf("done"), events)
    }

    @Test
    fun an_incomplete_bill_cannot_be_finished() {
        show(state.copy(complete = false, assignedMinor = 100_000))

        rule.onNodeWithText("Rp100,000 of Rp152,460 assigned").assertIsDisplayed()
        rule.onNodeWithText("Done").assertIsNotEnabled()
    }

    @Test
    fun talkback_reads_what_each_person_owes_and_marks_the_payer() {
        show(state)

        rule.onNodeWithContentDescription("You paid the bill. Your share is 0 rupiah").assertIsDisplayed()
        rule.onNodeWithContentDescription("Budi owes 41,580 rupiah").performClick()
        assertEquals(listOf("open 2"), events)
    }

    @Test
    fun the_breakdown_explains_a_total_in_plain_words() {
        show(state.copy(openPersonId = 2))

        rule.onNodeWithText("How was this calculated?").assertIsDisplayed()
        rule.onNodeWithText("Nasi Goreng").assertIsDisplayed()
        rule.onNodeWithText("Ocha (shared with Ani, ½)").assertIsDisplayed()
        rule.onNodeWithText("Service 5% of your order").assertIsDisplayed()
        rule.onNodeWithText("Tax 10% of your order + service").assertIsDisplayed()
        rule.onNodeWithText("Rp3,780").assertIsDisplayed()
    }

    @Test
    fun accounts_are_chips_ticked_for_this_bill() {
        val bca = PaymentAccount(1, PaymentAccountType.BANK, "BCA", "1234567890", "Ricky", isDefault = true)
        val gopay = PaymentAccount(2, PaymentAccountType.EWALLET, "GoPay", "081234567890", "Ricky", isDefault = false)
        show(state.copy(accounts = listOf(bca, gopay), selectedAccountIds = setOf(1)))

        rule.onNodeWithText("BCA · 1234567890").performScrollTo().assertIsOn()
        rule.onNodeWithText("GoPay · 081234567890").assertIsOff().performClick()
        assertEquals(listOf("account 2"), events)
    }

    @Test
    fun without_accounts_the_summary_offers_to_add_one() {
        show(state)

        rule.onNodeWithText("Add account").performScrollTo().performClick()
        assertEquals(listOf("add account"), events)
    }

    @Test
    fun rounding_and_discount_are_spelled_out() {
        val lines = budiLines + BreakdownLine.Discount(3_000) + BreakdownLine.Rounding(-2)
        show(state.copy(people = listOf(SummaryPerson(budi, 38_578, lines)), openPersonId = 2))

        rule.onNodeWithText("Discount, by what you ordered").assertIsDisplayed()
        rule.onNodeWithText("−Rp3,000").assertIsDisplayed()
        rule.onNodeWithText("Rp2 rounding taken off").assertIsDisplayed()
    }
}
