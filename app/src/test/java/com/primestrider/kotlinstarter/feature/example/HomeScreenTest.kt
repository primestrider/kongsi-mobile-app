package com.primestrider.kotlinstarter.feature.example

import android.content.Context
import androidx.compose.material3.Surface
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import com.primestrider.kotlinstarter.feature.catalog.impl.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import com.primestrider.kotlinstarter.feature.devices.impl.R as DevicesR

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class HomeScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private fun show(onOpen: (Any) -> Unit) =
        rule.setContent {
            AppTheme(darkTheme = false) {
                Surface { HomeScreen(sections = exampleSections, onOpen = onOpen) }
            }
        }

    private fun scrollTo(text: String) = rule.onNodeWithTag(HOME_LIST_TAG).performScrollToNode(hasText(text))

    @Test
    fun rendersSectionTitleAndFormEntry() {
        show {}
        rule.onNode(hasText("Features") and isHeading()).assertIsDisplayed()
        scrollTo("Form")
        rule.onNodeWithText("Form").assertIsDisplayed()
    }

    @Test
    fun formEntryExposesButtonRole() {
        show {}
        scrollTo("Form")
        rule
            .onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button) and hasText("Form"))
            .assertIsDisplayed()
    }

    @Test
    fun clickingSettingsOpensFormExampleRoute() {
        var opened: Any? = null
        show { opened = it }
        scrollTo("Form")
        rule.onNodeWithText("Form").performClick()
        assertEquals(FormExampleRoute, opened)
    }

    @Test
    fun searching_shows_only_matching_features() {
        show {}
        rule.onNodeWithText("Search features").performTextInput("camera")
        rule.onNodeWithTag(HOME_LIST_TAG).performScrollToNode(hasText("Camera & QR Scanner"))
        rule.onNodeWithText("Camera & QR Scanner").assertIsDisplayed()
        rule.onNodeWithText("Form").assertDoesNotExist()
    }

    @Test
    fun a_search_without_matches_offers_to_clear_it() {
        show {}
        rule.onNodeWithText("Search features").performTextInput("zzzz")
        rule.onNodeWithText("No features match \"zzzz\"").assertIsDisplayed()
        rule.onNodeWithText("Clear search").performClick()
        scrollTo("Form")
        rule.onNodeWithText("Form").assertIsDisplayed()
    }

    @Test
    fun section_chip_limits_the_list_to_that_section() {
        show {}
        rule.onNode(hasText("Styling") and SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected)).performScrollTo().performClick()
        rule.onNodeWithText("Colors").assertIsDisplayed()
        rule.onNodeWithText("Form").assertDoesNotExist()
        rule.onNode(hasText("All") and SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected)).performScrollTo().performClick()
        scrollTo("Form")
        rule.onNodeWithText("Form").assertIsDisplayed()
    }

    @Test
    fun query_and_chip_survive_recreation() {
        val restore = StateRestorationTester(rule)
        restore.setContent {
            AppTheme(darkTheme = false) { Surface { HomeScreen(sections = exampleSections, onOpen = {}) } }
        }
        rule.onNode(hasText("Features") and SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected)).performClick()
        rule.onNodeWithText("Search features").performTextInput("todo")
        restore.emulateSavedInstanceStateRestore()
        rule.onNodeWithText("Todos").assertIsDisplayed()
        rule.onNodeWithText("Products").assertDoesNotExist()
        rule.onNode(hasText("Features") and SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected)).assertIsSelected()
    }

    @Test
    fun chips_are_radio_buttons_and_the_chosen_one_is_selected() {
        show {}
        val styling = hasText("Styling") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
        rule.onNode(styling).performScrollTo().performClick()
        rule.onNode(styling).assertIsSelected()
        rule.onNode(hasText("All") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)).assertIsNotSelected()
    }

    @Test
    fun a_search_shows_how_many_of_a_sections_examples_matched() {
        show {}
        val context = ApplicationProvider.getApplicationContext<Context>()
        val total = exampleSections.first { it.title == DevicesR.string.example_home_sdk_title }.entries.size
        val matched =
            filterSections(exampleSections, "camera", null) { context.getString(it) }
                .first { it.title == DevicesR.string.example_home_sdk_title }
                .entries.size
        rule.onNodeWithText("Search features").performTextInput("camera")
        rule.onNodeWithContentDescription("$matched of $total examples").assertExists()
    }

    private fun resultsLine() =
        SemanticsMatcher("a results summary") { node ->
            node.config.getOrNull(SemanticsProperties.Text)?.any { Regex("""\d+ results?""").matches(it.text) } == true
        }

    @Test
    fun a_search_shows_a_visible_polite_results_line() {
        show {}
        rule.onNodeWithText("Search features").performTextInput("camera")
        val line = resultsLine() and SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
        rule.onAllNodes(line).assertCountEquals(1)
        rule.onNode(line).assertIsDisplayed()
    }

    @Test
    fun a_chip_alone_shows_the_results_line_and_no_filter_hides_it() {
        show {}
        rule.onAllNodes(resultsLine()).assertCountEquals(0)
        rule.onNode(hasText("Features") and SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected)).performClick()
        rule.onNode(resultsLine()).assertIsDisplayed()
        rule.onNode(hasText("All") and SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected)).performClick()
        rule.onAllNodes(resultsLine()).assertCountEquals(0)
    }

    @Test
    fun a_chip_alone_shows_the_plain_count_not_n_of_n() {
        show {}
        val total = exampleSections.first { it.title == R.string.example_home_features_title }.entries.size
        rule.onNode(hasText("Features") and SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected)).performClick()
        rule.onNodeWithContentDescription("$total examples").assertExists()
        rule.onNodeWithContentDescription("$total of $total examples").assertDoesNotExist()
    }

    @Test
    fun the_title_scrolls_away_while_search_and_chips_stay_pinned() {
        show {}
        rule.onNode(hasText("Kotlin Starter") and isHeading()).assertIsDisplayed()
        rule.onNodeWithTag(HOME_LIST_TAG).performScrollToNode(hasText("Form"))
        rule.onNode(hasText("Kotlin Starter") and isHeading()).assertDoesNotExist()
        rule.onNodeWithText("Search features").assertIsDisplayed()
        rule.onNode(hasText("All") and SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected)).assertIsDisplayed()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w640dp-h360dp-land", fontScale = 2.0f)
    fun the_first_entry_is_reachable_in_a_short_window_at_a_large_font() {
        show {}
        rule.onNodeWithTag(HOME_LIST_TAG).performScrollToNode(hasText("Products"))
        rule.onNodeWithText("Products").assertIsDisplayed()
    }

    @Test
    fun the_empty_state_is_a_polite_live_region() {
        show {}
        rule.onNodeWithText("Search features").performTextInput("zzzz")
        rule
            .onNode(
                SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite) and
                    hasText("No features match \"zzzz\""),
            ).assertExists()
    }

    @Test
    fun scrolling_the_list_keeps_focus_in_the_search_field() {
        show {}
        rule.onNode(hasSetTextAction()).performTextInput("e")
        rule.onNode(hasSetTextAction()).assertIsFocused()
        rule.onNodeWithTag(HOME_LIST_TAG).performTouchInput { swipeUp() }
        rule.onNode(hasSetTextAction()).assertIsFocused()
    }

    @Test
    @Config(sdk = [36], qualifiers = "in")
    fun indonesian_search_matches_translated_titles() {
        show {}
        rule.onNodeWithText("Cari fitur").performTextInput("kamera")
        rule.onNodeWithTag(HOME_LIST_TAG).performScrollToNode(hasText("Kamera & Pemindai QR"))
        rule.onNodeWithText("Kamera & Pemindai QR").assertIsDisplayed()
        rule.onNodeWithText("Formulir").assertDoesNotExist()
    }

    @Test
    fun a_single_section_hides_search_and_chips() {
        rule.setContent {
            AppTheme(darkTheme = false) { Surface { ComponentsHomeScreen(entries = componentEntries, onOpen = {}) } }
        }
        rule.onNodeWithText("Search features").assertDoesNotExist()
        rule.onNodeWithText("All").assertDoesNotExist()
    }

    @Test
    @Config(sdk = [36], fontScale = 2.0f)
    fun settingsEntryReachableAtLargeFontScale() {
        show {}
        rule.onNodeWithTag(HOME_LIST_TAG).performScrollToNode(hasText("Form"))
        rule.onNodeWithText("Form").assertIsDisplayed()
    }
}
