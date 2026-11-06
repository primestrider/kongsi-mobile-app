package com.primestrider.kotlinstarter.feature.example.auth

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.auth.AuthTokens
import com.primestrider.kotlinstarter.core.auth.AuthUser
import com.primestrider.kotlinstarter.core.auth.Session
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import com.primestrider.kotlinstarter.core.network.ApiError
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class AccountScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private var loads = 0
    private var signOuts = 0
    private val session = Session(AuthUser(1, "emilys", "e@x.com", "Emily", "Johnson"), AuthTokens("a", "r"))

    private fun show(profile: ProfileLoad = ProfileLoad.Idle) {
        rule.setContent {
            AppTheme(darkTheme = false) {
                AccountScreen(
                    session = session,
                    profile = profile,
                    onLoadProfile = { loads++ },
                    onSignOut = { signOuts++ },
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun shows_name_and_email() {
        show()
        rule.onNodeWithText("Emily Johnson").assertExists()
        rule.onNodeWithText("e@x.com").assertExists()
        rule.onNodeWithText("A screen only a signed-in session can reach").assertExists()
    }

    @Test
    fun load_button_calls_back_and_is_disabled_while_loading() {
        show()
        rule.onNodeWithText("Load from /auth/me").performClick()
        assertEquals(1, loads)
    }

    @Test
    fun loading_disables_load_button() {
        show(ProfileLoad.Loading)
        rule.onNodeWithText("Load from /auth/me").assertIsNotEnabled()
    }

    @Test
    fun loaded_shows_three_info_rows() {
        show(ProfileLoad.Loaded(UserResponse(42, "emilys42", "srv@x.com", "Emily", "Johnson")))
        rule.onNodeWithText("ID").assertExists()
        rule.onNodeWithText("42").assertExists()
        rule.onNodeWithText("emilys42").assertExists()
        rule.onNodeWithText("srv@x.com").assertExists()
    }

    @Test
    fun failed_network_shows_network_message() {
        show(ProfileLoad.Failed(ApiError(null, isNetworkError = true)))
        rule.onNodeWithText("Cannot reach the server. Check your connection.").assertExists()
    }

    @Test
    fun failed_server_message_is_shown() {
        show(ProfileLoad.Failed(ApiError("Token expired", 401)))
        rule.onNodeWithText("Token expired").assertExists()
    }

    @Test
    fun failed_load_banner_is_titled_as_a_profile_error() {
        show(ProfileLoad.Failed(ApiError(null, 500)))
        rule.onNodeWithText("Could not load profile").assertExists()
        rule.onNodeWithText("Could not sign in").assertDoesNotExist()
    }

    @Test
    fun sign_out_asks_first_and_only_confirm_signs_out() {
        show()
        rule.onNodeWithText("Sign out").performClick()
        rule.onNodeWithText("Sign out?").assertExists()
        assertEquals(0, signOuts)
        rule.onNodeWithText("Cancel").performClick()
        assertEquals(0, signOuts)
        rule.onNodeWithText("Sign out?").assertDoesNotExist()

        rule.onNodeWithText("Sign out").performClick()
        rule.onNodeWithText("You will need to sign in again to see your account.").assertExists()
        rule.onNode(hasText("Sign out") and hasAnyAncestor(isDialog())).performClick()
        assertEquals(1, signOuts)
    }
}
