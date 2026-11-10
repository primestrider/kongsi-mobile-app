package com.primestrider.kongsi.feature.bill.ui.people

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kongsi.core.designsystem.theme.AppTheme
import com.primestrider.kongsi.feature.bill.contacts.ContactMatch
import com.primestrider.kongsi.feature.bill.data.FrequentPerson
import com.primestrider.kongsi.feature.bill.model.Person
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class BillPeopleScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val events = mutableListOf<String>()

    private val actions =
        BillPeopleActions(
            onBack = {},
            onBackToItems = { events += "items" },
            onQueryChange = {},
            onContactsAllowed = {},
            onAdd = { events += "add $it" },
            onRemove = { events += "remove $it" },
            onPickerResult = {},
            onNext = { events += "next" },
        )

    private val me = PersonRow(Person(1, "Ricky", "6281111222233"), index = 0, label = "Ricky", isMe = true)
    private val budi = PersonRow(Person(2, "Budi", "6281234567890"), index = 1, label = "Budi", isMe = false)
    private val ani = PersonRow(Person(3, "Ani", null), index = 2, label = "Ani", isMe = false)

    private fun show(
        state: BillPeopleUiState,
        query: String = "",
    ) = rule.setContent { AppTheme(false) { BillPeopleScreen(state, query, actions) } }

    @Test
    fun the_payer_is_shown_as_you_and_cannot_be_removed() {
        show(BillPeopleUiState(loaded = true, people = listOf(me)))

        rule.onNodeWithText("You (Ricky)").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Paid the bill").performScrollTo().assertIsDisplayed()
        rule.onNodeWithContentDescription("Remove Ricky").assertDoesNotExist()
        rule.onNodeWithText("Next").assertIsNotEnabled()
    }

    @Test
    fun friends_can_be_removed_and_one_without_a_number_says_so() {
        show(BillPeopleUiState(loaded = true, people = listOf(me, budi, ani), canContinue = true))

        rule.onNode(hasScrollAction()).performScrollToNode(hasText("No number"))
        rule.onNodeWithText("No number").assertIsDisplayed()
        rule.onNode(hasScrollAction()).performScrollToNode(hasContentDescription("Remove Budi"))
        rule.onNodeWithContentDescription("Remove Budi").performClick()
        rule.onNodeWithText("Next").assertIsEnabled().performClick()
        assertEquals(listOf("remove 2", "next"), events)
    }

    @Test
    fun search_results_show_contacts_with_whatsapp_and_the_typed_name() {
        val contact = ContactMatch(9, "Budi Santoso", "0812-3456-7890", "6281234567890", mobile = true, photoUri = null, hasWhatsApp = true)
        show(
            BillPeopleUiState(
                loaded = true,
                people = listOf(me),
                suggestions = listOf(Suggestion.Contact(contact), Suggestion.Typed("budi")),
            ),
            query = "budi",
        )

        rule.onNodeWithText("Budi Santoso").assertIsDisplayed()
        rule.onNodeWithText("WhatsApp").assertIsDisplayed()
        rule.onNodeWithText("Add \"budi\" without a number").performClick()
        rule.onNodeWithText("Budi Santoso").performClick()
        assertEquals(listOf("add Typed(name=budi)", "add Contact(match=$contact)"), events)
    }

    @Test
    fun before_typing_people_from_earlier_bills_are_offered() {
        val earlier = FrequentPerson(4, "Citra", "6281311112222", useCount = 3)
        show(BillPeopleUiState(loaded = true, people = listOf(me), suggestions = listOf(Suggestion.Frequent(earlier))))

        rule.onNodeWithText("From earlier bills").assertIsDisplayed()
        rule.onNodeWithText("Citra").performClick()
        assertEquals(listOf("add Frequent(person=$earlier)"), events)
    }

    @Test
    fun the_contact_picker_is_always_offered() {
        show(BillPeopleUiState(loaded = true, people = listOf(me)))

        rule.onNodeWithText("Choose from contacts").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun the_items_step_reopens_from_the_step_indicator() {
        show(BillPeopleUiState(loaded = true, people = listOf(me)))

        rule.onNodeWithText("Items").performClick()
        assertEquals(listOf("items"), events)
    }
}
