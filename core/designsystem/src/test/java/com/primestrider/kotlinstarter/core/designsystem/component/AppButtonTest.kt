package com.primestrider.kotlinstarter.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
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
class AppButtonTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun loading_button_ignores_clicks_and_reports_disabled() {
        var clicks = 0
        rule.setContent {
            AppTheme(false) { AppButton("Save", onClick = { clicks++ }, loading = true, modifier = Modifier.testTag("b")) }
        }
        rule.onNodeWithTag("b").assertIsNotEnabled()
        rule.onNodeWithTag("b").performClick()
        assertEquals(0, clicks)
    }

    @Test
    fun click_fires_once() {
        var clicks = 0
        rule.setContent { AppTheme(false) { AppButton("Save", onClick = { clicks++ }) } }
        rule.onNodeWithText("Save").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun long_label_stays_on_one_line_within_the_button() {
        val label = "Continue to secure payment with your saved card ending in 4242"
        rule.setContent {
            AppTheme(false) {
                Box(Modifier.width(200.dp)) { AppButton(label, onClick = {}, modifier = Modifier.testTag("b")) }
            }
        }
        val button = rule.onNodeWithTag("b").fetchSemanticsNode().boundsInRoot
        val text = rule.onNodeWithText(label, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue("label overflows the button", text.right <= button.right && text.left >= button.left)
        assertTrue("label wrapped to several lines", text.height < button.height)
    }

    @Test
    fun every_variant_renders_its_text() {
        rule.setContent {
            AppTheme(false) {
                Column { ButtonVariant.entries.forEach { AppButton(it.name, onClick = {}, variant = it) } }
            }
        }
        ButtonVariant.entries.forEach { rule.onNodeWithText(it.name).assertExists() }
    }
}
