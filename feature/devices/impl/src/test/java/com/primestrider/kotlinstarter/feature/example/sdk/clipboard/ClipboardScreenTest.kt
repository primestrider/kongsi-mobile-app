package com.primestrider.kotlinstarter.feature.example.sdk.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.AnnotatedString
import androidx.test.core.app.ApplicationProvider
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
class ClipboardScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val clipboard =
        ApplicationProvider.getApplicationContext<Context>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    @Before
    fun emptyClipboard() {
        clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
        clipboard.clearPrimaryClip()
    }

    private fun show() = rule.setContent { AppTheme(false) { ClipboardScreen(onBack = {}) } }

    private fun button(text: String) = hasText(text) and hasClickAction()

    @Test
    fun copy_puts_the_referral_code_on_the_clipboard() {
        show()
        rule.onNode(button("Copy")).performScrollTo().performClick()
        assertEquals(
            "SUPER-7K2Q9",
            clipboard.primaryClip
                ?.getItemAt(0)
                ?.text
                ?.toString(),
        )
    }

    @Test
    fun paste_fills_the_field_and_remembers_the_last_pasted_text() {
        clipboard.setPrimaryClip(ClipData.newPlainText("", "1234567890"))
        show()
        rule.onNode(button("Paste")).performScrollTo().performClick()
        rule.onNode(hasSetTextAction()).assertTextContains("1234567890")
        rule.onNodeWithText("Last pasted").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun paste_with_an_empty_clipboard_leaves_the_field_empty() {
        show()
        rule.onNode(button("Paste")).performScrollTo().performClick()
        rule.onNodeWithText("Last pasted").assertDoesNotExist()
        rule.onNode(hasSetTextAction()).assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
    }
}
