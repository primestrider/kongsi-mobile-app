package com.primestrider.kotlinstarter.app

// remove-examples: exercises the example features, so scripts/remove-examples.sh deletes it with them.

import android.app.Application
import android.content.ComponentName
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.testing.WorkManagerTestInitHelper
import com.primestrider.kotlinstarter.app.navigation.MainShell
import com.primestrider.kotlinstarter.core.designsystem.component.LocalMessenger
import com.primestrider.kotlinstarter.core.designsystem.component.Messenger
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import com.primestrider.kotlinstarter.feature.example.componentEntries
import com.primestrider.kotlinstarter.feature.example.exampleSections
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(application = HiltTestApplication::class, sdk = [36])
class AppNavHostTest {
    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(hiltRule).around(composeRule)

    @Inject
    lateinit var messenger: Messenger

    @Test
    fun everyCatalogEntryIsRegisteredAndShowsItsPage() {
        hiltRule.inject()
        // HiltTestApplication is not a Configuration.Provider: the Background Task page needs a test WorkManager.
        WorkManagerTestInitHelper.initializeTestWorkManager(ApplicationProvider.getApplicationContext())
        // Robolectric binds services with a null component; the Background Music page binds MusicService. Answer its
        // bind with the component and no binder, so the MediaController fails to connect cleanly (no real service).
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).setComponentNameAndServiceForBindService(
            ComponentName(app, "com.primestrider.kotlinstarter.feature.example.sdk.music.MusicService"),
            null,
        )
        lateinit var navController: NavHostController
        // Replaces MainActivity's own content so the test controls the NavHostController.
        composeRule.activity.setContent {
            navController = rememberNavController()
            CompositionLocalProvider(LocalMessenger provides messenger) { AppTheme(darkTheme = false) { MainShell(navController) } }
        }
        composeRule.waitForIdle()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val entries = exampleSections.flatMap { it.entries } + componentEntries
        assertTrue(entries.isNotEmpty())
        entries.forEach { entry ->
            composeRule.runOnUiThread { navController.navigate(entry.route) }
            composeRule.waitForIdle()
            composeRule.onNodeWithText(context.getString(entry.title)).assertIsDisplayed()
            composeRule.runOnUiThread { navController.popBackStack() }
            composeRule.waitForIdle()
        }
    }
}
