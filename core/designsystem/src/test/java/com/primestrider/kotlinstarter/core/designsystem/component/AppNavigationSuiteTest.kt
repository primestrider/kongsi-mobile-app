package com.primestrider.kotlinstarter.core.designsystem.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Bottom bar on phones, a navigation rail from medium width (tablets, foldables, landscape), nothing for one item. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class AppNavigationSuiteTest {
    @get:Rule
    val rule = createComposeRule()

    private val items = listOf(NavBarItem("Home", Icons.Filled.Home), NavBarItem("Profile", Icons.Filled.Person))

    private fun show(
        navItems: List<NavBarItem> = items,
        onSelect: (Int) -> Unit = {},
    ) = rule.setContent {
        AppTheme(false) { AppNavigationSuite(navItems, selectedIndex = 0, onSelect = onSelect) { Text("Content") } }
    }

    @Test
    @Config(qualifiers = "w400dp-h800dp")
    fun compact_width_shows_the_bottom_bar() {
        show()
        rule.onNodeWithTag(APP_NAVIGATION_BAR_TAG).assertExists()
        rule.onNodeWithTag(APP_NAVIGATION_RAIL_TAG).assertDoesNotExist()
        rule.onNodeWithText("Content").assertExists()
    }

    @Test
    @Config(qualifiers = "w840dp-h900dp")
    fun wide_window_shows_the_rail() {
        var picked = -1
        show(onSelect = { picked = it })
        rule.onNodeWithTag(APP_NAVIGATION_RAIL_TAG).assertExists()
        rule.onNodeWithTag(APP_NAVIGATION_BAR_TAG).assertDoesNotExist()
        rule.onNodeWithText("Profile").performClick()
        assertEquals(1, picked)
    }

    @Test
    @Config(qualifiers = "w840dp-h900dp")
    fun one_item_shows_no_navigation() {
        show(navItems = items.take(1))
        rule.onNodeWithTag(APP_NAVIGATION_RAIL_TAG).assertDoesNotExist()
        rule.onNodeWithTag(APP_NAVIGATION_BAR_TAG).assertDoesNotExist()
        rule.onNodeWithText("Content").assertExists()
    }

    @Test
    fun switching_between_bar_and_rail_keeps_the_content() {
        var rail by mutableStateOf(false)
        var created = 0
        rule.setContent {
            AppTheme(false) {
                AppNavigationSuiteLayout(items, selectedIndex = 0, onSelect = {}, rail = rail) {
                    remember { created++ }
                    Text("Content")
                }
            }
        }
        rule.runOnIdle { rail = true }
        rule.onNodeWithTag(APP_NAVIGATION_RAIL_TAG).assertExists()
        rule.runOnIdle { rail = false }
        rule.onNodeWithTag(APP_NAVIGATION_BAR_TAG).assertExists()
        assertEquals(1, created)
    }
}
