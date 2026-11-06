package com.primestrider.kotlinstarter.app

// remove-examples: exercises the example features, so scripts/remove-examples.sh deletes it with them.

import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.app.navigation.MainShell
import com.primestrider.kotlinstarter.core.auth.AuthTokens
import com.primestrider.kotlinstarter.core.auth.AuthUser
import com.primestrider.kotlinstarter.core.auth.FakeSessionCipher
import com.primestrider.kotlinstarter.core.auth.Session
import com.primestrider.kotlinstarter.core.auth.SessionCipher
import com.primestrider.kotlinstarter.core.auth.SessionModule
import com.primestrider.kotlinstarter.core.auth.SessionRepository
import com.primestrider.kotlinstarter.core.datastore.DataStoreModule
import com.primestrider.kotlinstarter.core.datastore.SessionStore
import com.primestrider.kotlinstarter.core.designsystem.component.LocalMessenger
import com.primestrider.kotlinstarter.core.designsystem.component.Messenger
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import com.primestrider.kotlinstarter.feature.example.AccountRoute
import com.primestrider.kotlinstarter.feature.example.SignInRoute
import com.primestrider.kotlinstarter.testing.GatedPreferencesDataStore
import com.primestrider.kotlinstarter.testing.InMemoryPreferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import javax.inject.Inject
import javax.inject.Singleton

/** Account is on the back stack while the stored session is still being read (process death restore). */
@HiltAndroidTest
@UninstallModules(SessionModule::class, DataStoreModule::class)
@RunWith(AndroidJUnit4::class)
@Config(application = HiltTestApplication::class, sdk = [36])
class AccountRestoreTest {
    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(hiltRule).around(composeRule)

    /** The session store's first read waits for [GatedPreferencesDataStore.readGate]. */
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
        fun sessionStore(): DataStore<Preferences> = GatedPreferencesDataStore().apply { readGate = CompletableDeferred() }
    }

    @Inject
    lateinit var messenger: Messenger

    @Inject
    lateinit var repository: SessionRepository

    @Inject
    @SessionStore
    lateinit var store: DataStore<Preferences>

    private lateinit var navController: NavHostController
    private val session = Session(AuthUser(1, "emilys", "e@x.com", "Emily", "Johnson"), AuthTokens("a", "r"))
    private val gated get() = store as GatedPreferencesDataStore

    @Before
    fun setUp() {
        hiltRule.inject()
        composeRule.activity.setContent {
            val nav = rememberNavController()
            navController = nav
            CompositionLocalProvider(LocalMessenger provides messenger) { AppTheme(darkTheme = false) { MainShell(nav) } }
        }
        composeRule.waitForIdle()
        composeRule.runOnUiThread { navController.navigate(AccountRoute) }
        composeRule.waitForIdle()
    }

    /** Stores [session] as a previous run would have, without touching the repository under test. */
    private fun storeSignedInSession() {
        val writer = SessionRepository(store, FakeSessionCipher(), Json, CoroutineScope(Job().apply { cancel() }))
        runBlocking { writer.signIn(session) }
    }

    private fun releaseRead() {
        gated.readGate!!.complete(Unit)
        composeRule.waitUntil { repository.loaded.value }
        composeRule.waitForIdle()
    }

    private fun onAccount() = navController.currentDestination?.hasRoute(AccountRoute::class) == true

    private fun onSignIn() = navController.currentDestination?.hasRoute(SignInRoute::class) == true

    @Test
    fun account_waits_for_the_stored_session_then_stays_when_signed_in() {
        storeSignedInSession()
        assertFalse(repository.loaded.value)
        assertTrue(onAccount())

        releaseRead()
        assertTrue(repository.isSignedIn)
        assertTrue(onAccount())
        composeRule.onNodeWithText("A screen only a signed-in session can reach").assertExists()
    }

    @Test
    fun account_redirects_to_sign_in_once_loaded_without_a_session() {
        assertFalse(repository.loaded.value)
        assertTrue(onAccount())

        releaseRead()
        assertTrue(onSignIn())
    }
}
