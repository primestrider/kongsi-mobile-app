package com.primestrider.kotlinstarter.core.designsystem.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
class AppScaffoldTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun no_back_arrow_without_onBack() {
        rule.setContent { AppTheme(false) { AppScaffold(title = "Title") { } } }
        rule.onNodeWithContentDescription("Back").assertDoesNotExist()
    }

    @Test
    fun back_arrow_calls_onBack() {
        var backs = 0
        rule.setContent { AppTheme(false) { AppScaffold(title = "Title", onBack = { backs++ }) { } } }
        rule.onNodeWithContentDescription("Back").performClick()
        assertEquals(1, backs)
    }

    @Test
    fun long_title_keeps_actions_visible() {
        rule.setContent {
            AppTheme(false) {
                AppScaffold(
                    title = "x".repeat(200),
                    onBack = {},
                    actions = { IconButton(onClick = {}) { Icon(Icons.Filled.Share, "Share") } },
                ) { }
            }
        }
        rule.onNodeWithContentDescription("Share").assertIsDisplayed()
    }
}
