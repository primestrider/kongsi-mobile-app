package com.primestrider.kotlinstarter.app

// remove-examples: exercises the example features, so scripts/remove-examples.sh deletes it with them.

import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.app.navigation.MainShell
import com.primestrider.kotlinstarter.core.auth.AuthTokens
import com.primestrider.kotlinstarter.core.auth.AuthUser
import com.primestrider.kotlinstarter.core.auth.FakeSessionCipher
import com.primestrider.kotlinstarter.core.auth.Session
import com.primestrider.kotlinstarter.core.auth.SessionCipher
import com.primestrider.kotlinstarter.core.auth.SessionModule
import com.primestrider.kotlinstarter.core.auth.SessionRepository
import com.primestrider.kotlinstarter.core.auth.SignOutReason
import com.primestrider.kotlinstarter.core.datastore.DataStoreModule
import com.primestrider.kotlinstarter.core.datastore.SessionStore
import com.primestrider.kotlinstarter.core.designsystem.component.LocalMessenger
import com.primestrider.kotlinstarter.core.designsystem.component.Messenger
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import com.primestrider.kotlinstarter.testing.InMemoryPreferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import javax.inject.Inject
import javax.inject.Singleton

@HiltAndroidTest
@UninstallModules(SessionModule::class, DataStoreModule::class)
@RunWith(AndroidJUnit4::class)
@Config(application = HiltTestApplication::class, sdk = [36])
class ProtectedRouteTest {
    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(hiltRule).around(composeRule)

    /** Robolectric has no AndroidKeyStore and a file-backed DataStore fails on Windows, so both are swapped here. */
    @Module
    @InstallIn(SingletonComponent::class)
    object TestModule {
        @Provides
        fun cipher(): SessionCipher = FakeSessionCipher()

        @Provides
        @Singleton
        fun settingsStore(): DataStore<Preferences> = InMemoryPreferencesDataStore()

        @Provides
        @Singleton
        @SessionStore
        fun sessionStore(): DataStore<Preferences> = InMemoryPreferencesDataStore()
    }

    @Inject
    lateinit var messenger: Messenger

    @Inject
    lateinit var repository: SessionRepository

    private val session = Session(AuthUser(1, "emilys", "e@x.com", "Emily", "Johnson"), AuthTokens("a", "r"))

    @Before
    fun setUp() {
        hiltRule.inject()
        runBlocking { repository.signIn(session) }
        composeRule.activity.setContent {
            CompositionLocalProvider(LocalMessenger provides messenger) { AppTheme(darkTheme = false) { MainShell() } }
        }
        composeRule.waitForIdle()
        composeRule.onNode(hasText("Profile") and isNavTab()).performClick()
        composeRule.waitForIdle()
    }

    private fun pressBack() {
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }

    @Test
    fun signed_in_user_reaches_account_from_profile() {
        composeRule.onNodeWithText("Emily Johnson").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("A screen only a signed-in session can reach").assertIsDisplayed()
    }

    @Test
    fun user_sign_out_from_account_returns_to_profile_hub() {
        composeRule.onNodeWithText("Emily Johnson").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Sign out").performScrollTo().performClick()
        composeRule.onNode(hasText("Sign out") and hasAnyAncestor(isDialog())).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("You are not signed in").assertIsDisplayed()
        composeRule.onNodeWithText("A screen only a signed-in session can reach").assertDoesNotExist()

        pressBack()
        composeRule.onNodeWithText("A screen only a signed-in session can reach").assertDoesNotExist()
    }

    @Test
    fun expiry_on_account_redirects_to_sign_in_and_back_skips_account() {
        composeRule.onNodeWithText("Emily Johnson").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("A screen only a signed-in session can reach").assertIsDisplayed()

        runBlocking { repository.signOut(SignOutReason.EXPIRED) }
        composeRule.waitForIdle()
        composeRule.onNode(hasText("Sign in") and hasClickAction().not()).assertIsDisplayed()
        composeRule.onNodeWithText("A screen only a signed-in session can reach").assertDoesNotExist()

        pressBack()
        composeRule.onNodeWithText("A screen only a signed-in session can reach").assertDoesNotExist()
        composeRule.onNodeWithText("You are not signed in").assertIsDisplayed()
    }
}

private fun isNavTab() = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
