package com.primestrider.kotlinstarter.feature.example.settings

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.createGraph
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.feature.example.SettingsRoute
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class SettingsNavigationTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun settings_destination_is_registered_by_feature() {
        rule.runOnUiThread {
            val navController = TestNavHostController(ApplicationProvider.getApplicationContext())
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            val graph = navController.createGraph(startDestination = SettingsRoute) { settingsDestinations(navController) }
            assertNotNull(graph.findNode<SettingsRoute>())
        }
    }
}
