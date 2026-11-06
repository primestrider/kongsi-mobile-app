package com.primestrider.kotlinstarter.core.designsystem.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class AppNavigationBarTest {
    @get:Rule
    val rule = createComposeRule()

    private val items =
        listOf(
            NavBarItem("Home", Icons.Filled.Home),
            NavBarItem("Inbox", Icons.AutoMirrored.Filled.List, badgeCount = 120),
            NavBarItem("Profile", Icons.Filled.Person),
        )

    @Test
    fun selection_and_clicks() {
        var selected = -1
        rule.setContent { AppTheme(false) { AppNavigationBar(items, selectedIndex = 1, onSelect = { selected = it }) } }
        rule.onNodeWithText("Inbox").assertIsSelected()
        rule.onNodeWithText("Profile").performClick()
        assertEquals(2, selected)
    }

    @Test
    fun badge_caps_at_99_plus() {
        rule.setContent { AppTheme(false) { AppNavigationBar(items, selectedIndex = 0, onSelect = {}) } }
        rule.onNodeWithText("99+", useUnmergedTree = true).assertExists()
    }
}
