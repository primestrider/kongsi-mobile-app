package com.primestrider.kotlinstarter.feature.example.auth

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.auth.AuthTokens
import com.primestrider.kotlinstarter.core.auth.AuthUser
import com.primestrider.kotlinstarter.core.auth.Session
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class ProfileScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private var signIn = 0
    private var account = 0
    private var settings = 0
    private val session = Session(AuthUser(1, "emilys", "e@x.com", "Emily", "Johnson"), AuthTokens("a", "r"))

    private fun show(session: Session?) {
        rule.setContent {
            AppTheme(darkTheme = false) {
                ProfileScreen(
                    session = session,
                    onSignIn = { signIn++ },
                    onOpenAccount = { account++ },
                    onOpenSettings = { settings++ },
                )
            }
        }
    }

    @Test
    fun signed_out_shows_prompt_and_sign_in_calls_back() {
        show(null)
        rule.onNodeWithText("You are not signed in").assertExists()
        rule.onNodeWithText("Sign in").performClick()
        assertEquals(1, signIn)
        assertEquals(0, account)
    }

    @Test
    fun signed_in_shows_name_and_opens_account() {
        show(session)
        rule.onNodeWithText("You are not signed in").assertDoesNotExist()
        rule.onNodeWithText("e@x.com").assertExists()
        rule.onNodeWithText("Emily Johnson").performClick()
        assertEquals(1, account)
    }

    @Test
    fun settings_row_is_always_there() {
        show(null)
        rule.onNodeWithText("Settings").performClick()
        assertEquals(1, settings)
    }
}
