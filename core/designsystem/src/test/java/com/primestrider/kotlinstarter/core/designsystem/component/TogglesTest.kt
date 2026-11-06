package com.primestrider.kotlinstarter.core.designsystem.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.LightColors
import com.primestrider.kotlinstarter.core.designsystem.theme.controlFill
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TogglesTest {
    @get:Rule
    val rule = createComposeRule()

    private fun role(role: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    @Test
    fun checkbox_toggles_and_reports_state() {
        val reported = mutableListOf<Boolean>()
        rule.setContent {
            var checked by remember { mutableStateOf(false) }
            AppTheme(false) {
                AppCheckbox(checked, {
                    reported += it
                    checked = it
                }, Modifier.testTag("c"))
            }
        }
        rule
            .onNodeWithTag("c")
            .assertIsOff()
            .assert(role(Role.Checkbox))
            .performClick()
        rule.onNodeWithTag("c").assertIsOn()
        assertEquals(listOf(true), reported)
    }

    @Test
    fun indeterminate_checkbox_reports_indeterminate() {
        rule.setContent { AppTheme(false) { AppCheckbox(ToggleableState.Indeterminate, {}, Modifier.testTag("c")) } }
        rule.onNodeWithTag("c").assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Indeterminate))
    }

    @Test
    fun checkbox_without_callback_adds_no_semantics_of_its_own() {
        rule.setContent {
            AppTheme(false) {
                Row(Modifier.toggleable(value = false, role = Role.Checkbox, onValueChange = {}).testTag("row")) {
                    AppCheckbox(checked = false, onCheckedChange = null)
                    Text("Terms")
                }
            }
        }
        assertEquals(1, rule.onAllNodes(hasClickAction()).fetchSemanticsNodes().size)
        assertEquals(1, rule.onAllNodes(role(Role.Checkbox), useUnmergedTree = true).fetchSemanticsNodes().size)
        rule.onNodeWithTag("row").assert(role(Role.Checkbox))
    }

    @Test
    fun radio_reports_selected_and_role() {
        rule.setContent { AppTheme(false) { AppRadioButton(selected = true, onClick = {}, modifier = Modifier.testTag("r")) } }
        rule.onNodeWithTag("r").assertIsSelected().assert(role(Role.RadioButton))
    }

    @Test
    fun switch_has_switch_role_and_48dp_target() {
        rule.setContent { AppTheme(false) { AppSwitch(checked = true, onCheckedChange = {}, modifier = Modifier.testTag("s")) } }
        rule
            .onNodeWithTag("s")
            .assert(role(Role.Switch))
            .assertIsOn()
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
    }

    @Test
    fun disabled_switch_ignores_clicks() {
        var calls = 0
        rule.setContent {
            AppTheme(false) { AppSwitch(checked = false, onCheckedChange = { calls++ }, enabled = false, modifier = Modifier.testTag("s")) }
        }
        rule.onNodeWithTag("s").assertIsNotEnabled().performClick()
        assertEquals(0, calls)
    }

    @Test
    fun checked_checkbox_paints_primary_and_unchecked_paints_fill() {
        lateinit var density: Density
        var checked by mutableStateOf(true)
        rule.setContent {
            density = LocalDensity.current
            AppTheme(false) { AppCheckbox(checked, null, Modifier.testTag("c")) }
        }
        // 4dp in from the top-start corner of the 22dp box: on the fill, off the check mark.
        assertColorClose(LightColors.primary, rule.onNodeWithTag("c").pixelAt(4.dp, 4.dp, density))
        checked = false
        rule.waitForIdle()
        assertColorClose(LightColors.controlFill, rule.onNodeWithTag("c").pixelAt(4.dp, 4.dp, density))
    }
}
