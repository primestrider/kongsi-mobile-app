package com.primestrider.kongsi.app

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.navigation.compose.composable
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kongsi.R
import com.primestrider.kongsi.app.navigation.MainShell
import com.primestrider.kongsi.app.navigation.TABS_PAGER_TAG
import com.primestrider.kongsi.app.navigation.TopLevelTab
import com.primestrider.kongsi.core.designsystem.component.LocalMessenger
import com.primestrider.kongsi.core.designsystem.component.Messenger
import com.primestrider.kongsi.core.designsystem.theme.AppTheme
import kotlinx.serialization.Serializable
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@Serializable
internal data object SwipeTestDetailRoute

/** The swipeable shell with two stand-in tabs: swipe, bar taps, back, and state kept across tabs and pages. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class SwipeableTabsTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val tabs =
        listOf(
            TopLevelTab(R.string.nav_home, Icons.Filled.Home) { onOpen ->
                var taps by rememberSaveable { mutableIntStateOf(0) }
                Column {
                    Text("Home page")
                    TextButton(onClick = { taps++ }) { Text("Home taps $taps") }
                    TextButton(onClick = { onOpen(SwipeTestDetailRoute) }) { Text("Open detail") }
                }
            },
            // A focusable control: an off-screen tab must not take initial focus and scroll itself into view.
            TopLevelTab(R.string.nav_profile, Icons.Filled.Person) {
                Column {
                    Text("Profile page")
                    TextButton(onClick = {}) { Text("Profile action") }
                }
            },
        )

    @Before
    fun setUp() {
        rule.setContent {
            CompositionLocalProvider(LocalMessenger provides Messenger()) {
                AppTheme(darkTheme = false) {
                    MainShell(tabs = tabs, extraDestinations = { composable<SwipeTestDetailRoute> { Text("Detail page") } })
                }
            }
        }
    }

    private fun pressBack() {
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
    }

    private fun onPage(text: String) = rule.onNodeWithText(text).assertIsDisplayed()

    @Test
    fun swiping_moves_between_tabs_and_selects_them_in_the_bar() {
        onPage("Home page")
        rule.onNodeWithTag(TABS_PAGER_TAG).performTouchInput { swipeLeft() }
        onPage("Profile page")
        rule.onNodeWithText("Profile").assertIsSelected()
        rule.onNodeWithTag(TABS_PAGER_TAG).performTouchInput { swipeRight() }
        onPage("Home page")
        rule.onNodeWithText("Home").assertIsSelected()
    }

    @Test
    fun starts_on_the_first_tab() {
        onPage("Home page")
        rule.onNodeWithText("Home").assertIsSelected()
    }

    @Test
    fun tapping_the_bar_switches_tabs() {
        rule.onNodeWithText("Profile").performClick()
        onPage("Profile page")
    }

    @Test
    fun back_on_another_tab_returns_home() {
        rule.onNodeWithText("Profile").performClick()
        rule.waitForIdle()
        pressBack()
        onPage("Home page")
    }

    @Test
    fun tab_state_survives_switching_tabs() {
        rule.onNodeWithText("Home taps 0").performClick()
        rule.onNodeWithTag(TABS_PAGER_TAG).performTouchInput { swipeLeft() }
        rule.onNodeWithTag(TABS_PAGER_TAG).performTouchInput { swipeRight() }
        onPage("Home taps 1")
    }

    @Test
    fun detail_opens_over_the_tabs_and_back_returns_to_the_same_tab_state() {
        rule.onNodeWithText("Home taps 0").performClick()
        rule.onNodeWithText("Open detail").performClick()
        onPage("Detail page")
        assertFalse(rule.onNodeWithText("Home page").isDisplayed())
        pressBack()
        onPage("Home taps 1")
    }
}
