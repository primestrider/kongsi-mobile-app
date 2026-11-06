package com.primestrider.kotlinstarter.feature.example.components

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class InputsScreenTest {
    @get:Rule
    val rule = createComposeRule()

    @Before
    fun setUp() {
        rule.setContent { AppTheme(darkTheme = false) { InputsScreen(onBack = {}) } }
    }

    /** The text the password field actually draws, after its visual transformation. */
    private fun renderedPassword(): String {
        val results = mutableListOf<TextLayoutResult>()
        val node = rule.onNodeWithTag(PASSWORD_FIELD_TAG).fetchSemanticsNode()
        node.config
            .getOrNull(SemanticsActions.GetTextLayoutResult)
            ?.action
            ?.invoke(results)
        return results
            .first()
            .layoutInput.text.text
    }

    @Test
    fun password_is_hidden_until_toggled() {
        rule.onNodeWithTag(PASSWORD_FIELD_TAG).performScrollTo().performTextInput("secret1")
        assertEquals("•••••••", renderedPassword())
        rule.onNodeWithContentDescription("Show password").performClick()
        assertEquals("secret1", renderedPassword())
    }

    @Test
    fun parent_checkbox_checks_all_children() {
        rule.onNodeWithTag(PARENT_CHECKBOX_TAG).performScrollTo().performClick()
        listOf("Email updates", "SMS", "Push").forEach { label ->
            rule.onNodeWithText(label).performScrollTo().assertIsOn()
        }
    }

    @Test
    fun select_shows_chosen_option() {
        rule.onNodeWithTag(SELECT_FIELD_TAG).performScrollTo().performClick()
        rule.onNodeWithText("Oldest").performClick()
        rule.onNodeWithText("Oldest").assertExists()
    }

    @Test
    fun secondary_tabs_switch_content() {
        val monthTab = hasText("Month") and hasAnyAncestor(hasTestTag(SECONDARY_TAB_ROW_TAG))
        rule.onNode(monthTab).performScrollTo().performClick()
        rule.onNodeWithText("This month's summary").assertExists()
    }

    @Test
    fun tabs_switch_content() {
        rule.onNodeWithTag(TAB_ROW_TAG).performScrollTo()
        rule.onNodeWithText("Following").performClick()
        rule.onNodeWithText("Posts from people you follow").assertExists()
    }
}
