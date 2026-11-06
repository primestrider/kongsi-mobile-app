package com.primestrider.kotlinstarter.feature.example.form

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import com.primestrider.kotlinstarter.feature.catalog.impl.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class FormExampleScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private fun show(state: FormUiState) {
        rule.setContent {
            AppTheme(darkTheme = false) {
                FormExampleScreen(state = state, onChange = { _, _ -> }, onSubmit = {}, onClear = {}, onBack = {})
            }
        }
    }

    @Test
    fun shows_field_error() {
        show(FormUiState(errors = mapOf(FormField.EMAIL to R.string.form_error_email_invalid), submitAttempted = true))
        rule.onNodeWithText("Invalid email address").assertExists()
    }

    @Test
    fun submit_disabled_while_submitting() {
        show(FormUiState(submitting = true))
        rule.onNodeWithTag(SUBMIT_BUTTON_TAG).assertIsNotEnabled()
    }
}
