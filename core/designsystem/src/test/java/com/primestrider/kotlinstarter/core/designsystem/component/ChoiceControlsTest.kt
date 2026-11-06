package com.primestrider.kotlinstarter.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.SliderColors
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class ChoiceControlsTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun segmented_choice_reports_the_clicked_option() {
        var picked: String? = null
        rule.setContent {
            AppTheme(false) {
                SegmentedChoice(options = listOf("a", "b", "c"), selected = "a", onSelect = { picked = it }, label = { "Option $it" })
            }
        }
        rule.onNodeWithText("Option b").performClick()
        assertEquals("b", picked)
    }

    @Test
    fun segmented_pill_follows_selection_in_rtl() {
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                AppTheme(false) {
                    Box(Modifier.width(300.dp)) {
                        SegmentedChoice(
                            listOf("a", "b", "c"),
                            selected = "c",
                            onSelect = {},
                            label = { it },
                            modifier = Modifier.testTag("seg"),
                        )
                    }
                }
            }
        }
        rule.waitForIdle()
        val track = rule.onNodeWithTag("seg").getUnclippedBoundsInRoot()
        val pill = rule.onNodeWithTag(SEGMENTED_PILL_TAG, useUnmergedTree = true).getUnclippedBoundsInRoot()
        // In RTL the third option is the visually leftmost third of the track.
        assertTrue("pill at ${pill.left}, track ${track.left}..${track.right}", pill.right < track.left + (track.right - track.left) / 2)
    }

    @Test
    fun segmented_reports_selection_semantics() {
        rule.setContent { AppTheme(false) { SegmentedChoice(listOf("a", "b"), selected = "b", onSelect = {}, label = { "Option $it" }) } }
        rule.onNodeWithText("Option b").assertIsSelected().assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        rule.onNodeWithText("Option a").assertIsNotSelected()
    }

    @Test
    fun every_segment_is_a_48dp_target() {
        rule.setContent {
            AppTheme(false) {
                Column {
                    SegmentedChoice(listOf("a", "b"), selected = "a", onSelect = {}, label = { "One $it" })
                    MultiSegmentedChoice(listOf("x"), selected = emptySet(), onToggle = {}, label = { "Many $it" })
                }
            }
        }
        listOf("One a", "One b", "Many x").forEach { rule.onNodeWithText(it).assertHeightIsAtLeast(48.dp) }
    }

    @Test
    fun slider_step_ticks_contrast_with_their_track() {
        lateinit var colors: SliderColors
        rule.setContent { AppTheme(false) { colors = appSliderColors() } }
        assertNotEquals(colors.activeTrackColor, colors.activeTickColor)
        assertNotEquals(colors.inactiveTrackColor, colors.inactiveTickColor)
    }

    @Test
    fun disabled_segment_ignores_clicks() {
        var picked: String? = null
        rule.setContent {
            AppTheme(
                false,
            ) { SegmentedChoice(listOf("a", "b"), selected = "a", onSelect = { picked = it }, label = { "Option $it" }, enabled = false) }
        }
        rule.onNodeWithText("Option b").assertIsNotEnabled().performClick()
        assertEquals(null, picked)
    }

    @Test
    fun multi_segmented_toggles_independently() {
        val toggled = mutableListOf<String>()
        rule.setContent {
            AppTheme(false) {
                MultiSegmentedChoice(listOf("Bold", "Italic"), selected = setOf("Bold"), onToggle = { toggled += it }, label = { it })
            }
        }
        rule.onNodeWithText("Bold").assertIsOn().assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
        rule.onNodeWithText("Italic").assertIsOff()
        rule.onNodeWithText("Bold").performClick()
        rule.onNodeWithText("Italic").performClick()
        assertEquals(listOf("Bold", "Italic"), toggled)
    }

    @Test
    fun range_slider_shows_label_and_value_text() {
        rule.setContent { AppTheme(false) { LabeledRangeSlider("Price", 20f..80f, {}, 0f..100f, valueText = "20 to 80") } }
        rule.onNodeWithText("Price").assertIsDisplayed()
        rule.onNodeWithText("20 to 80").assertIsDisplayed()
        assertEquals(2, rule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)).fetchSemanticsNodes().size)
    }

    @Test
    fun switch_row_toggles_from_a_click_on_the_label_and_has_switch_role() {
        var checked = false
        rule.setContent { AppTheme(false) { SwitchRow(label = "Hidden", checked = checked, onCheckedChange = { checked = it }) } }
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)).assertIsDisplayed()
        rule.onNodeWithText("Hidden").performClick()
        assertEquals(true, checked)
    }

    @Test
    fun switch_row_has_one_click_target() {
        rule.setContent { AppTheme(false) { SwitchRow(label = "Hidden", checked = false, onCheckedChange = {}) } }
        assertEquals(1, rule.onAllNodes(hasClickAction()).fetchSemanticsNodes().size)
    }

    @Test
    fun labeled_slider_shows_label_and_value_text() {
        rule.setContent { AppTheme(false) { LabeledSlider("Brightness", 0.5f, {}, 0f..1f, valueText = "50%") } }
        rule.onNodeWithText("Brightness").assertIsDisplayed()
        rule.onNodeWithText("50%").assertIsDisplayed()
    }

    @Test
    fun info_card_renders_a_dash_for_null_and_blank() {
        rule.setContent { AppTheme(false) { InfoCard(rows = listOf("Name" to "Starter", "Empty" to null, "Blank" to "  ")) } }
        rule.onNodeWithText("Starter").assertIsDisplayed()
        rule.onAllNodesWithText("—").assertCountEquals(2)
    }
}
