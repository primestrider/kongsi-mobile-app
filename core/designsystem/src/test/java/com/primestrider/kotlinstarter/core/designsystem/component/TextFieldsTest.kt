package com.primestrider.kotlinstarter.core.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.ImeAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class TextFieldsTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun error_text_is_shown_and_marks_field() {
        rule.setContent {
            AppTheme(false) {
                AppTextField("", {}, label = "Email", errorText = "Invalid email", modifier = Modifier.testTag("f"))
            }
        }
        rule.onNodeWithText("Invalid email").assertExists()
        rule.onNodeWithTag("f").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
    }

    @Test
    fun max_length_truncates_and_counts() {
        rule.setContent {
            var value by remember { mutableStateOf("") }
            AppTheme(false) { AppTextField(value, { value = it }, label = "Code", maxLength = 5, modifier = Modifier.testTag("f")) }
        }
        rule.onNodeWithTag("f").performTextInput("abcdefg")
        assertEquals("abcde", drawn("f"))
        rule.onNodeWithText("5/5").assertExists()
    }

    @Test
    fun max_length_never_splits_an_emoji() {
        rule.setContent {
            var value by remember { mutableStateOf("") }
            AppTheme(false) { AppTextField(value, { value = it }, label = "Note", maxLength = 2, modifier = Modifier.testTag("f")) }
        }
        rule.onNodeWithTag("f").performTextInput("a😀b")
        assertEquals("a😀", drawn("f"))
        rule.onNodeWithText("2/2").assertExists()
    }

    @Test
    fun counter_stays_next_to_supporting_text() {
        rule.setContent {
            AppTheme(false) { AppTextField("abc", {}, label = "Bio", supportingText = "Shown on your profile", maxLength = 10) }
        }
        rule.onNodeWithText("Shown on your profile").assertExists()
        rule.onNodeWithText("3/10").assertExists()
    }

    @Test
    fun password_is_masked_until_shown() {
        rule.setContent {
            var value by remember { mutableStateOf("") }
            AppTheme(false) { PasswordTextField(value, { value = it }, label = "Password", modifier = Modifier.testTag("p")) }
        }
        rule.onNodeWithTag("p").performTextInput("abc")
        assertEquals("•••", drawn("p"))
        rule.onNodeWithContentDescription("Show password").performClick()
        assertEquals("abc", drawn("p"))
    }

    @Test
    fun search_clear_button_only_when_not_empty() {
        rule.setContent {
            var query by remember { mutableStateOf("") }
            AppTheme(false) { SearchField(query, { query = it }, modifier = Modifier.testTag("s")) }
        }
        rule.onNodeWithContentDescription("Clear search").assertDoesNotExist()
        rule.onNodeWithTag("s").performTextInput("abc")
        rule.onNodeWithContentDescription("Clear search").performClick()
        assertEquals("", drawn("s"))
    }

    /** The text a field draws, after its visual transformation. */
    private fun drawn(tag: String): String {
        val results = mutableListOf<TextLayoutResult>()
        rule
            .onNodeWithTag(tag)
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsActions.GetTextLayoutResult)
            ?.action
            ?.invoke(results)
        return results
            .first()
            .layoutInput.text.text
    }

    @Test
    fun ime_action_runs_callback_for_app_and_password_fields() {
        var app = 0
        var password = 0
        rule.setContent {
            AppTheme(false) {
                AppTextField("", {}, label = "A", imeAction = ImeAction.Next, onImeAction = { app++ }, modifier = Modifier.testTag("a"))
                PasswordTextField("", {}, label = "P", onImeAction = { password++ }, modifier = Modifier.testTag("p"))
            }
        }
        rule.onNodeWithTag("a").performImeAction()
        rule.onNodeWithTag("p").performImeAction()
        assertEquals(1, app)
        assertEquals(1, password)
    }
}
