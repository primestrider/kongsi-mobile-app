package com.primestrider.kotlinstarter.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.CONTROL_ANIMATION_MS
import com.primestrider.kotlinstarter.core.designsystem.theme.LightColors
import com.primestrider.kotlinstarter.core.designsystem.theme.controlErrorFill
import com.primestrider.kotlinstarter.core.designsystem.theme.controlFill
import com.primestrider.kotlinstarter.core.designsystem.theme.controlFocusFill
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** How the filled fields look: fill, focus tint, error tint (pixels need native graphics). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TextFieldLookTest {
    @get:Rule
    val rule = createComposeRule()

    private lateinit var density: Density

    private fun field(errorText: String? = null) {
        rule.setContent {
            density = LocalDensity.current
            AppTheme(false) {
                Box(Modifier.width(300.dp)) {
                    AppTextField("", {}, label = "Email", errorText = errorText, modifier = Modifier.testTag("f"))
                }
            }
        }
    }

    /** 2dp in from the start edge, a third of the way down: inside the box, where an outline would paint. */
    private fun edgePixel(tag: String = "f") = rule.onNodeWithTag(tag).pixelAt(2.dp, 20.dp, density)

    @Test
    fun field_has_no_border_and_uses_fill() {
        field()
        assertColorClose(LightColors.controlFill, edgePixel())
    }

    @Test
    fun focused_field_switches_to_focus_tint() {
        field()
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("f").performClick()
        rule.mainClock.advanceTimeBy(CONTROL_ANIMATION_MS + 200L)
        assertColorClose(LightColors.controlFocusFill, edgePixel())
    }

    @Test
    fun error_field_uses_error_tint_and_keeps_error_text() {
        field(errorText = "Minimal 8 karakter")
        assertColorClose(LightColors.controlErrorFill, edgePixel())
        rule.onNodeWithText("Minimal 8 karakter").assertExists()
        rule.onNodeWithTag("f").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
    }

    @Test
    fun empty_field_exposes_its_label() {
        field()
        rule.onNodeWithText("Email").assertExists()
    }

    @Test
    fun no_trailing_icon_by_default() {
        field(errorText = "Wrong")
        // Only the field itself is clickable: no "!" / clear / check button was added.
        val clickable = rule.onAllNodesWithText("Email").fetchSemanticsNodes().size
        assertEquals(1, clickable)
        rule.onNodeWithTag("f").assert(hasClickAction())
        assertEquals(1, rule.onAllNodes(hasClickAction()).fetchSemanticsNodes().size)
    }

    @Test
    fun prefix_is_shown_before_the_value() {
        rule.setContent {
            density = LocalDensity.current
            AppTheme(false) { AppTextField("812", {}, label = "Phone", prefix = "+62") }
        }
        val prefix = rule.onNodeWithText("+62", useUnmergedTree = true).getUnclippedBoundsInRoot()
        // The value node is the whole editable field; the floating label starts where the value text starts.
        val labelLeft =
            rule.onAllNodesWithText("Phone", useUnmergedTree = true).fetchSemanticsNodes().maxOf { it.boundsInRoot.left }
        assertTrue("prefix ends at ${prefix.right}, value text starts at $labelLeft", prefix.right <= with(density) { labelLeft.toDp() })
    }

    @Test
    fun select_field_uses_fill_not_outline() {
        rule.setContent {
            density = LocalDensity.current
            AppTheme(false) {
                Box(Modifier.width(300.dp)) {
                    SelectField("Number", listOf(SelectOption(1, "One")), selected = null, onSelect = {}, modifier = Modifier.testTag("s"))
                }
            }
        }
        assertColorClose(LightColors.controlFill, edgePixel("s"))
    }
}
