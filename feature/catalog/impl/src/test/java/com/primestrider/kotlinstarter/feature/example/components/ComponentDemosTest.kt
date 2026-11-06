package com.primestrider.kotlinstarter.feature.example.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class ComponentDemosTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private var backs = 0

    private fun show(id: String) =
        rule.setContent {
            AppTheme(false) { componentDemos.single { it.id == id }.content { backs++ } }
        }

    private fun height(tag: String) =
        rule
            .onNodeWithTag(tag)
            .fetchSemanticsNode()
            .size.height

    @Test
    fun ids_are_unique_and_in_order() {
        assertEquals(
            listOf(
                "top-app-bar-small",
                "top-app-bar-center",
                "top-app-bar-medium",
                "top-app-bar-large",
                "bottom-app-bar",
                "modal-drawer",
                "dismissible-drawer",
                "search-bar",
                "docked-search-bar",
                "bottom-sheet-scaffold",
            ),
            componentDemos.map { it.id },
        )
    }

    @Test
    fun back_arrow_leaves_the_demo() {
        show("top-app-bar-small")
        rule.onNodeWithContentDescription("Back").performClick()
        assertEquals(1, backs)
    }

    @Test
    fun large_top_app_bar_collapses_on_scroll() {
        show("top-app-bar-large")
        val before = height("topAppBar")
        rule.onNodeWithTag("demoList").performTouchInput { swipeUp() }
        rule.waitForIdle()
        assertTrue("collapsed: $before -> ${height("topAppBar")}", height("topAppBar") < before)
    }

    @Test
    fun bottom_app_bar_has_fab_and_actions() {
        show("bottom-app-bar")
        rule.onNodeWithContentDescription("Add").assertIsDisplayed()
        rule.onNodeWithContentDescription("Search").assertIsDisplayed()
        rule.onNodeWithContentDescription("Favourites").assertIsDisplayed()
    }

    @Test
    fun modal_drawer_opens_from_menu() {
        show("modal-drawer")
        rule.onNodeWithText("Outbox").assertIsNotDisplayed()
        rule.onNodeWithContentDescription("Open navigation drawer").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Outbox").assertIsDisplayed().performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Selected: Outbox").assertIsDisplayed()
    }

    @Test
    fun dismissible_drawer_toggles() {
        show("dismissible-drawer")
        rule.onNodeWithText("Outbox").assertIsNotDisplayed()
        rule.onNodeWithContentDescription("Open navigation drawer").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Outbox").assertIsDisplayed()
        rule.onNodeWithContentDescription("Close navigation drawer").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Outbox").assertIsNotDisplayed()
    }

    @Test
    fun search_bar_expands_and_filters() {
        show("search-bar")
        rule.onNodeWithTag("searchInput").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("searchInputExpanded").performTextInput("ap")
        rule.waitForIdle()
        rule.onNodeWithText("Apple").assertExists()
        rule.onNodeWithText("Banana").assertDoesNotExist()
    }

    @Test
    fun bottom_sheet_scaffold_peeks_and_expands() {
        show("bottom-sheet-scaffold")
        rule.onNodeWithText("Swipe up for more").assertIsDisplayed()
        rule.onNodeWithText("Sheet content").assertIsNotDisplayed()
        rule.onNodeWithTag("sheetHandle").performTouchInput { swipeUp() }
        rule.waitForIdle()
        rule.onNodeWithText("Sheet content").assertIsDisplayed()
    }

    @Test
    fun bottom_sheet_scaffold_button_expands_for_talkback() {
        show("bottom-sheet-scaffold")
        rule.onNodeWithText("Expand sheet").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Sheet content").assertIsDisplayed()
    }

    @Test
    fun tapping_a_suggestion_fills_the_query_and_collapses() {
        show("search-bar")
        rule.onNodeWithTag("searchInput").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Apple").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Searched for: Apple").assertIsDisplayed()
        rule.onNodeWithTag("searchInputExpanded").assertDoesNotExist()
    }

    @Test
    fun docked_search_bar_expands_and_filters() {
        show("docked-search-bar")
        rule.onNodeWithTag("searchInput").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("searchInputExpanded").performTextInput("ap")
        rule.waitForIdle()
        rule.onNodeWithText("Apricot").assertExists()
        rule.onNodeWithText("Banana").assertDoesNotExist()
    }
}
