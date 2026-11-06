package com.primestrider.kotlinstarter.feature.example.auth

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import com.primestrider.kotlinstarter.core.network.ApiError
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class SignInScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private var filled = 0
    private var submitted = 0

    private fun show(state: SignInUiState) {
        rule.setContent {
            AppTheme(darkTheme = false) {
                SignInScreen(
                    state = state,
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onShortLivedChange = {},
                    onFillDemo = { filled++ },
                    onSubmit = { submitted++ },
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun network_error_shows_connection_message() {
        show(SignInUiState(error = ApiError(null, isNetworkError = true)))
        rule.onNodeWithText("Cannot reach the server. Check your connection.").assertExists()
    }

    @Test
    fun server_message_is_shown() {
        show(SignInUiState(error = ApiError("Invalid credentials", 400)))
        rule.onNodeWithText("Invalid credentials").assertExists()
    }

    @Test
    fun fill_button_calls_back() {
        show(SignInUiState())
        rule.onNodeWithText("Fill the form").performClick()
        assertEquals(1, filled)
    }

    @Test
    fun sign_in_button_disabled_while_loading() {
        show(SignInUiState(loading = true))
        rule.onNodeWithTag(SIGN_IN_BUTTON_TAG).assertIsNotEnabled()
    }

    @Test
    fun ime_done_on_password_submits() {
        show(SignInUiState())
        rule.onNodeWithTag(PASSWORD_FIELD_TAG).performImeAction()
        assertEquals(1, submitted)
    }
}
