package com.primestrider.kotlinstarter.core.designsystem.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.primestrider.kotlinstarter.core.designsystem.component.AppCheckbox
import com.primestrider.kotlinstarter.core.designsystem.component.AppRadioButton
import com.primestrider.kotlinstarter.core.designsystem.component.AppSwitch
import com.primestrider.kotlinstarter.core.designsystem.component.AppTextField
import com.primestrider.kotlinstarter.core.designsystem.component.LabeledRangeSlider
import com.primestrider.kotlinstarter.core.designsystem.component.LabeledSlider
import com.primestrider.kotlinstarter.core.designsystem.component.MultiSegmentedChoice
import com.primestrider.kotlinstarter.core.designsystem.component.SearchField
import com.primestrider.kotlinstarter.core.designsystem.component.SegmentedChoice
import com.primestrider.kotlinstarter.core.designsystem.component.SelectField
import com.primestrider.kotlinstarter.core.designsystem.component.SelectOption
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.LocalAppColors
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Reference images of the form controls in light and dark theme (the focused state is pinned by TextFieldLookTest:
 * a capture happens before focus lands). A plain `test` run does not capture; record with
 * the manual "record screenshots" CI job (Linux is the source of truth) and verify with `verifyRoborazziDebug`.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "w400dp-h1400dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ControlsScreenshotTest {
    @Test
    fun controls_light() = captureRoboImage("src/test/screenshots/controls_light.png") { Controls(dark = false) }

    @Test
    fun controls_dark() = captureRoboImage("src/test/screenshots/controls_dark.png") { Controls(dark = true) }
}

@Composable
private fun Controls(dark: Boolean) {
    AppTheme(darkTheme = dark) {
        Column(
            Modifier.width(400.dp).background(LocalAppColors.current.background).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppTextField("", {}, label = "Empty")
            AppTextField("ricky@example.com", {}, label = "Filled", supportingText = "Helper text")
            AppTextField("1234", {}, label = "Error", errorText = "At least 8 characters")
            AppTextField("", {}, label = "Disabled", enabled = false)
            AppTextField("812 3456 7890", {}, label = "Phone", prefix = "+62")
            SearchField(query = "camera", onQueryChange = {})
            SelectField("Sort by", listOf(SelectOption(1, "Newest")), selected = 1, onSelect = {})
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppCheckbox(ToggleableState.Off, null)
                AppCheckbox(ToggleableState.On, null)
                AppCheckbox(ToggleableState.Indeterminate, null)
                AppRadioButton(selected = false, onClick = null)
                AppRadioButton(selected = true, onClick = null)
                AppSwitch(checked = false, onCheckedChange = null)
                AppSwitch(checked = true, onCheckedChange = null)
            }
            SegmentedChoice(listOf("Day", "Week", "Month"), selected = "Week", onSelect = {}, label = { it })
            MultiSegmentedChoice(listOf("Bold", "Italic", "Underline"), selected = setOf("Bold"), onToggle = {}, label = { it })
            LabeledSlider("Volume", 40f, {}, 0f..100f, valueText = "40")
            LabeledRangeSlider("Price", 20f..80f, {}, 0f..100f, valueText = "20 – 80")
        }
    }
}
