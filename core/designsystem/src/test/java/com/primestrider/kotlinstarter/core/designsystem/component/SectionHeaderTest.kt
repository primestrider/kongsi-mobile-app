package com.primestrider.kotlinstarter.core.designsystem.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
class SectionHeaderTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun header_is_a_heading_with_action() {
        var actions = 0
        rule.setContent { AppTheme(false) { SectionHeader("Recent", actionLabel = "See all", onAction = { actions++ }) } }
        rule.onNode(isHeading()).assertExists()
        rule.onNodeWithText("See all").performClick()
        assertEquals(1, actions)
    }

    @Test
    fun info_row_long_label_keeps_value_visible() {
        val label = "Nomor identitas kependudukan pemesan sesuai kartu tanda penduduk elektronik"
        rule.setContent { AppTheme(false) { InfoRow(label = label, value = "3171") } }
        rule.onNodeWithText("3171").assertIsDisplayed()
        assertTrue(
            rule
                .onNodeWithText("3171")
                .fetchSemanticsNode()
                .size.width > 0,
        )
    }

    @Test
    fun info_row_shows_label_and_long_value() {
        val value = "v".repeat(120)
        rule.setContent { AppTheme(false) { InfoRow(label = "Version", value = value) } }
        rule.onNodeWithText("Version").assertIsDisplayed()
        rule.onNodeWithText(value).assertIsDisplayed()
    }
}
