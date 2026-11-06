package com.primestrider.kotlinstarter.core.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class SelectFieldTest {
    @get:Rule
    val rule = createComposeRule()

    private val options =
        listOf(
            SelectOption(1, "One"),
            SelectOption(2, "Two"),
            SelectOption(3, "Three", enabled = false),
        )

    private fun show(onSelect: (Int) -> Unit) {
        rule.setContent {
            AppTheme(false) {
                SelectField("Number", options, selected = 1, onSelect = onSelect, modifier = Modifier.testTag("s"))
            }
        }
        rule.onNodeWithTag("s").performClick()
    }

    @Test
    fun choosing_an_option_reports_it_once() {
        val picked = mutableListOf<Int>()
        show { picked += it }
        rule.onNodeWithText("Two").performClick()
        assertEquals(listOf(2), picked)
    }

    @Test
    fun disabled_option_does_nothing() {
        val picked = mutableListOf<Int>()
        show { picked += it }
        rule.onNodeWithText("Three").performClick()
        assertEquals(emptyList<Int>(), picked)
    }

    @Test
    fun long_selected_label_keeps_dropdown_arrow_visible() {
        val long = listOf(SelectOption(1, "x".repeat(120)))
        rule.setContent { AppTheme(false) { SelectField("Number", long, selected = 1, onSelect = {}) } }
        rule.onNodeWithTag(SELECT_FIELD_ARROW_TAG, useUnmergedTree = true).assertIsDisplayed()
    }
}
