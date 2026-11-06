package com.primestrider.kotlinstarter.feature.example.sdk.contacts

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class ContactsSearchContentTest {
    @get:Rule
    val rule = createComposeRule()

    private fun show(state: ContactsUiState) = rule.setContent { AppTheme(false) { ContactsSearchContent(state) {} } }

    @Test
    fun before_the_first_answer_neither_rows_nor_the_empty_message_show() {
        show(ContactsUiState())
        rule.onNodeWithText("Search by name").assertIsDisplayed()
        rule.onNodeWithText("No contacts found.").assertDoesNotExist()
    }

    @Test
    fun an_empty_answer_says_no_contacts_found() {
        show(ContactsUiState(query = "zz", loaded = true))
        rule.onNodeWithText("No contacts found.").assertIsDisplayed()
    }

    @Test
    fun rows_show_the_name_and_first_phone_with_fallbacks() {
        show(ContactsUiState(rows = listOf(ContactRow("Budi", "0812"), ContactRow(null, null)), loaded = true))
        rule.onNodeWithText("Budi").assertIsDisplayed()
        rule.onNodeWithText("0812").assertIsDisplayed()
        rule.onNodeWithText("Unnamed").assertIsDisplayed()
        rule.onNodeWithText("—").assertIsDisplayed()
        rule.onNodeWithText("No contacts found.").assertDoesNotExist()
    }

    @Test
    fun the_count_replaces_the_plain_caption() {
        show(ContactsUiState(count = 42, loaded = true))
        rule.onNodeWithText("Search 42 contacts by name").assertIsDisplayed()
    }
}
