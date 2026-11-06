package com.primestrider.kotlinstarter.app

// remove-examples: exercises the example features, so scripts/remove-examples.sh deletes it with them.

import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.app.navigation.MainShell
import com.primestrider.kotlinstarter.core.designsystem.component.LocalMessenger
import com.primestrider.kotlinstarter.core.designsystem.component.Messenger
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import com.primestrider.kotlinstarter.feature.example.HOME_LIST_TAG
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(application = HiltTestApplication::class, sdk = [36])
class MainShellTest {
    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(hiltRule).around(composeRule)

    @Inject
    lateinit var messenger: Messenger

    @Before
    fun setUp() {
        hiltRule.inject()
        composeRule.activity.setContent {
            CompositionLocalProvider(LocalMessenger provides messenger) { AppTheme(darkTheme = false) { MainShell() } }
        }
        composeRule.waitForIdle()
    }

    private fun openColors() {
        composeRule.onNodeWithTag(HOME_LIST_TAG).performScrollToNode(hasText("Colors"))
        composeRule.onNodeWithText("Colors").performClick()
        composeRule.onNodeWithText("Semantic colors").assertExists()
    }

    private fun tab(label: String) {
        composeRule.onNode(hasText(label) and isNavTab()).performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun tabs_keep_their_back_stacks() {
        openColors()
        tab("Profile")
        composeRule.onNodeWithText("You are not signed in").assertIsDisplayed()
        tab("Home")
        composeRule.onNodeWithText("Semantic colors").assertExists()
    }

    @Test
    fun reselecting_tab_pops_to_root() {
        openColors()
        tab("Home")
        composeRule.onNodeWithTag(HOME_LIST_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("Semantic colors").assertDoesNotExist()
    }

    /** Back from another tab's root shows Home as the user left it, and Home stays that way afterwards. */
    @Test
    fun back_to_home_restores_and_keeps_home_state() {
        openColors()
        tab("Profile")
        pressBack()
        composeRule.onNodeWithText("Semantic colors").assertExists()
        tab("Components")
        tab("Home")
        composeRule.onNodeWithText("Semantic colors").assertExists()
        pressBack()
        composeRule.onNodeWithTag(HOME_LIST_TAG).assertIsDisplayed()
        tab("Components")
        tab("Home")
        composeRule.onNodeWithText("Semantic colors").assertDoesNotExist()
    }

    @Test
    fun back_at_home_root_leaves_the_app() {
        pressBack()
        assertTrue(composeRule.activity.isFinishing)
    }

    private fun pressBack() {
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }

    @Test
    fun back_from_other_tab_root_returns_home() {
        tab("Profile")
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(HOME_LIST_TAG).assertIsDisplayed()
    }
}

private fun isNavTab() = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
