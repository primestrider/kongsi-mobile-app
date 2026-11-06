package com.primestrider.kotlinstarter.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
class StateViewsTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun error_state_retry_calls_back() {
        var retries = 0
        rule.setContent { AppTheme(false) { ErrorState(message = "Network down", onRetry = { retries++ }) } }
        rule.onNodeWithText("Retry").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun empty_state_without_action_has_no_button() {
        rule.setContent { AppTheme(false) { EmptyState(title = "Nothing here") } }
        rule.onNodeWithText("Nothing here").assertExists()
        rule.onAllNodes(hasClickAction()).assertCountEquals(0)
    }

    @Test
    fun loading_overlay_blocks_touches() {
        var clicks = 0
        rule.setContent {
            AppTheme(false) {
                Box {
                    Button(onClick = { clicks++ }) { Text("Pay") }
                    LoadingOverlay(visible = true)
                }
            }
        }
        rule.onNodeWithText("Pay").performClick()
        assertEquals(0, clicks)
    }

    @Test
    fun loading_overlay_is_not_announced_as_a_button() {
        rule.setContent { AppTheme(false) { Box { LoadingOverlay(visible = true) } } }
        rule
            .onNodeWithContentDescription("Loading")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
    }
}
