package com.primestrider.kotlinstarter.feature.example.animation.lists

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class ListsScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private fun show() = rule.setContent { AppTheme(false) { ListsScreen(onBack = {}) } }

    /** Removed rows fade out with animateItem(), so let the animation finish before asserting they are gone. */
    private fun settle() {
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
    }

    /** Row buttons sit in a LazyColumn, which only places them once the list itself is scrolled into view. */
    private fun pressRowButton(description: String) {
        rule.onNodeWithTag("demoList").performScrollTo()
        rule.onNodeWithContentDescription(description).performClick()
    }

    /** "Add" appears in the toolbar and, when the list is empty, in the empty state; this is the toolbar one. */
    private fun add() = rule.onNode(hasText("Add") and hasAnyAncestor(hasTestTag("listActions"))).performScrollTo().performClick()

    @Test
    fun add_shows_a_new_item_on_top() {
        show()
        rule.onNodeWithText("Item 4").assertDoesNotExist()
        add()
        rule.onNodeWithText("Item 4").assertExists()
    }

    @Test
    fun remove_deletes_that_row() {
        show()
        pressRowButton("Remove Item 1")
        settle()
        rule.onNodeWithText("Item 1").assertDoesNotExist()
        rule.onNodeWithText("Item 2").assertExists()
    }

    @Test
    fun removing_every_row_shows_the_empty_state() {
        show()
        listOf(1, 2, 3).forEach {
            pressRowButton("Remove Item $it")
            settle()
        }
        rule.onNodeWithText("No items").performScrollTo().assertExists()
        add()
        settle()
        rule.onNodeWithText("No items").assertDoesNotExist()
        rule.onNodeWithText("Item 4").assertExists()
    }

    @Test
    fun the_empty_state_has_its_own_add_button() {
        show()
        listOf(1, 2, 3).forEach {
            pressRowButton("Remove Item $it")
            settle()
        }
        rule.onNode(hasText("Add") and hasAnyAncestor(hasTestTag("emptyState"))).performScrollTo().performClick()
        settle()
        rule.onNodeWithText("No items").assertDoesNotExist()
        rule.onNodeWithText("Item 4").assertExists()
    }

    @Test
    fun removing_the_last_row_keeps_the_list_composed() {
        show()
        listOf(1, 2).forEach {
            pressRowButton("Remove Item $it")
            settle()
        }
        // Stop the clock so the removal is still animating right after the click: the list must still be composed.
        rule.mainClock.autoAdvance = false
        pressRowButton("Remove Item 3")
        rule.onNodeWithTag("demoList").assertExists()
        rule.mainClock.autoAdvance = true
        settle()
        rule.onNodeWithText("Item 3").assertDoesNotExist()
        rule.onNodeWithText("No items").assertExists()
    }

    @Test
    fun move_buttons_are_labelled_with_the_item() {
        show()
        rule.onNodeWithTag("demoList").performScrollTo()
        rule.onNodeWithContentDescription("Move Item 2 up").assertExists()
        rule.onNodeWithContentDescription("Move Item 2 down").assertExists()
    }

    @Test
    fun reload_shows_the_placeholder_then_the_rows() {
        show()
        // Opening the page starts loading; the placeholder is announced as "Loading" and the rows follow.
        rule.onNodeWithContentDescription("Loading").assertExists()
        rule.mainClock.advanceTimeBy(2_000)
        settle()
        rule.onNodeWithContentDescription("Loading").assertDoesNotExist()
        rule.onNodeWithText("Loaded row 1").performScrollTo().assertExists()
        rule.onNodeWithText("Reload").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Loading").assertExists()
        rule.mainClock.advanceTimeBy(2_000)
        settle()
        rule.onNodeWithContentDescription("Loading").assertDoesNotExist()
        rule.onNodeWithText("Loaded row 1").performScrollTo().assertExists()
    }

    @Test
    fun list_contents_survive_recreation() {
        val tester = StateRestorationTester(rule)
        tester.setContent { AppTheme(false) { ListsScreen(onBack = {}) } }
        add()
        pressRowButton("Remove Item 2")
        settle()
        tester.emulateSavedInstanceStateRestore()
        rule.onNodeWithText("Item 4").performScrollTo().assertExists()
        rule.onNodeWithText("Item 2").assertDoesNotExist()
        rule.onNodeWithText("Item 1").assertExists()
    }
}
