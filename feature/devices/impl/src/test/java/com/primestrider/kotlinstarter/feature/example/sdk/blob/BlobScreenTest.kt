package com.primestrider.kotlinstarter.feature.example.sdk.blob

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class BlobScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private fun show() = rule.setContent { AppTheme(false) { BlobScreen(onBack = {}) } }

    private fun click(label: String) = rule.onNodeWithText(label).performScrollTo().performClick()

    @Test
    fun read_actions_wait_for_a_created_payload() {
        show()
        listOf("Text", "Slice", "Combine").forEach { rule.onNodeWithText(it).assertIsNotEnabled() }

        click("Create")

        listOf("Text", "Slice", "Combine").forEach { rule.onNodeWithText(it).assertIsEnabled() }
        rule.onNodeWithText("48 bytes").assertExists()
        rule.onNodeWithText("application/json").assertExists()
    }

    @Test
    fun slice_shows_the_first_eight_bytes_as_hex() {
        show()
        click("Create")
        click("Slice")
        rule.onNodeWithText("7b 22 74 72 78 22 3a 22").assertExists()
    }

    @Test
    fun combine_doubles_the_payload_plus_a_newline() {
        show()
        click("Create")
        click("Combine")
        rule.onNodeWithText("97 bytes").assertExists()
        rule.onNodeWithText("text/plain").assertExists()
    }

    @Test
    fun combining_twice_matches_combining_once() {
        show()
        click("Create")
        click("Combine")
        click("Combine")
        rule.onNodeWithText("97 bytes").assertExists()
        rule.onNodeWithText("195 bytes").assertDoesNotExist()
    }

    @Test
    fun combine_then_slice_then_combine_still_uses_the_created_payload() {
        show()
        click("Create")
        click("Combine")
        click("Slice")
        rule.onNodeWithText("48 bytes").assertExists()
        click("Combine")
        rule.onNodeWithText("97 bytes").assertExists()
    }
}
