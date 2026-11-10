package com.primestrider.kongsi.feature.bill.ui.assign

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kongsi.core.designsystem.theme.AppTheme
import com.primestrider.kongsi.feature.bill.model.BillItem
import com.primestrider.kongsi.feature.bill.model.Person
import com.primestrider.kongsi.feature.bill.ui.people.PersonRow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
// A phone-sized screen, so an item card and its chips fit in view together.
@Config(sdk = [36], qualifiers = "w400dp-h800dp")
class BillAssignScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val events = mutableListOf<String>()

    private val actions =
        BillAssignActions(
            onBack = {},
            onStepClick = { events += "step $it" },
            onModeChange = { events += "mode $it" },
            onSelectPerson = { events += "select $it" },
            onToggle = { item, person -> events += "toggle $item $person" },
            onToggleEveryone = { events += "everyone $it" },
            onSplitEqually = { events += "split" },
            onUnassignedClick = { events += "unassigned" },
            onFocusShown = {},
            onNext = { events += "next" },
        )

    private val me = PersonRow(Person(1, "Ricky"), index = 0, label = "Ricky", isMe = true)
    private val budi = PersonRow(Person(2, "Budi"), index = 1, label = "Budi", isMe = false)

    private val state =
        BillAssignUiState(
            loaded = true,
            people = listOf(me, budi),
            items =
                listOf(
                    AssignItemRow(BillItem(10, "Nasi Goreng", 30_000, setOf(2)), listOf(budi)),
                    AssignItemRow(BillItem(11, "Steak", 90_000, emptySet()), emptyList()),
                ),
            selectedPersonId = 2,
            assignedMinor = 34_650,
            totalMinor = 152_460,
            unassignedItemIds = listOf(11),
        )

    private fun show(state: BillAssignUiState) = rule.setContent { AppTheme(false) { BillAssignScreen(state, actions) } }

    @Test
    fun unassigned_items_block_next_with_a_message_that_leads_to_them() {
        show(state)

        rule.onNodeWithText("34,650 of 152,460 assigned").assertIsDisplayed()
        rule.onNodeWithText("Next").assertIsNotEnabled()
        rule.onNodeWithText("1 item not assigned yet").performClick()
        assertEquals(listOf("unassigned"), events)
    }

    @Test
    fun by_item_chips_show_who_had_it_and_toggle_people() {
        show(state)

        // The payer is "You"; Budi is ticked on Nasi Goreng, nobody on Steak.
        rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Nasi Goreng"))
        rule.onAllNodesWithText("Budi").onFirst().assertIsOn()
        rule
            .onAllNodesWithText("You")
            .onFirst()
            .assertIsOff()
            .performClick()
        rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Steak"))
        rule.onAllNodesWithText("Everyone").onLast().performClick()
        assertEquals(listOf("toggle 10 1", "everyone 11"), events)
    }

    @Test
    fun by_person_ticks_the_chosen_persons_items_with_the_same_change() {
        show(state.copy(mode = AssignMode.BY_PERSON))

        rule.onAllNodesWithText("Budi").onFirst().assertIsSelected()
        rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Budi, what did you have?"))
        rule.onNodeWithText("Budi, what did you have?").assertIsDisplayed()
        rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Nobody yet"))
        rule.onNodeWithText("Steak").performClick()
        rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("You"))
        rule.onNodeWithText("You").performClick()
        // Ticking Steak for Budi here is the same change as ticking Budi on Steak by item.
        assertEquals(listOf("toggle 11 2", "select 1"), events)
    }

    @Test
    fun complete_assignment_enables_next_and_split_equally_is_offered() {
        show(state.copy(unassignedItemIds = emptyList(), assignedMinor = 152_460, canContinue = true))

        rule.onNodeWithText("1 item not assigned yet").assertDoesNotExist()
        rule.onNodeWithText("Split whole bill equally").performClick()
        rule.onNodeWithText("By person").performClick()
        rule.onNodeWithText("Next").assertIsEnabled().performClick()
        assertEquals(listOf("split", "mode BY_PERSON", "next"), events)
    }
}
