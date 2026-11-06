package com.primestrider.kotlinstarter.core.designsystem.component

import androidx.activity.ComponentActivity
import androidx.activity.ComponentDialog
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class BottomSheetsTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun option_selection_reports_once_then_dismisses() {
        val picked = mutableListOf<Int>()
        var dismisses = 0
        rule.setContent {
            AppTheme(false) {
                OptionsBottomSheet(
                    title = "Sort by",
                    options = listOf(SelectOption(1, "Newest"), SelectOption(2, "Oldest")),
                    selected = 1,
                    onSelect = { picked += it },
                    onDismiss = { dismisses++ },
                )
            }
        }
        rule.mainClock.autoAdvance = false
        rule.onNodeWithText("Oldest").performClick()
        rule.onNodeWithText("Oldest").performClick()
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        assertEquals(listOf(2), picked)
        assertEquals(1, dismisses)
    }

    @Test
    fun option_rows_are_at_least_48dp_tall() {
        rule.setContent {
            AppTheme(false) {
                OptionsBottomSheet(
                    title = "Sort by",
                    options = listOf(SelectOption(1, "Newest")),
                    selected = 1,
                    onSelect = {},
                    onDismiss = {},
                )
            }
        }
        rule.onNodeWithText("Newest").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun non_dismissible_sheet_ignores_back() {
        var dismisses = 0
        rule.setContent {
            AppTheme(false) {
                AppBottomSheet(onDismiss = { dismisses++ }, title = "Required", dismissible = false) { Text("Body") }
            }
        }
        rule.waitForIdle()
        rule.onNodeWithText("Body").assertExists()
        rule.runOnUiThread { (ShadowDialog.getLatestDialog() as ComponentDialog).onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        assertEquals(0, dismisses)
        rule.onNodeWithText("Body").assertExists()
    }
}
