package com.primestrider.kotlinstarter.app.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.primestrider.kotlinstarter.core.designsystem.component.AppNavigationSuite
import com.primestrider.kotlinstarter.core.designsystem.component.LocalMessenger
import com.primestrider.kotlinstarter.core.designsystem.component.MessengerHost
import com.primestrider.kotlinstarter.core.designsystem.component.NavBarItem
import com.primestrider.kotlinstarter.core.designsystem.component.NavBarKeyboardMode

/**
 * App shell: bottom navigation over one nested graph per tab.
 *
 * - Switching tabs saves and restores each tab's back stack.
 * - Re-selecting the current tab pops it back to its root.
 * - Back at the root of a non-Home tab switches to Home, restoring Home's saved stack.
 * - The navigation is a bottom bar on phones and a rail on wider windows (AppNavigationSuite); with a single tab
 *   (no feature adds one) it is hidden.
 */
@Composable
fun MainShell(
    navController: NavHostController = rememberNavController(),
    tabs: List<TopLevelTab> = topLevelTabs,
    homeScreens: List<HomeScreenContent> = com.primestrider.kotlinstarter.app.navigation.homeScreens,
) {
    val entry by navController.currentBackStackEntryAsState()
    val destination = entry?.destination
    val atTabRoot = destination?.let { it.parent?.startDestinationId == it.id } ?: true
    val current =
        tabs.firstOrNull { tab -> destination?.hierarchy?.any { it.hasRoute(tab.graphClass) } == true }
            ?: HomeTab

    fun open(tab: TopLevelTab) {
        if (tab == current) {
            navController.popBackStack(tab.startRoute, inclusive = false)
            return
        }
        navController.navigate(tab.graph) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        snackbarHost = { MessengerHost(LocalMessenger.current, snackbarHostState) },
        // The shell only owns the navigation; screens handle the system bars with their own AppScaffold.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { inner ->
        // Bottom bar on phones, navigation rail on wider windows; hidden with a single tab.
        AppNavigationSuite(
            items = tabs.map { NavBarItem(stringResource(it.label), it.icon) },
            selectedIndex = tabs.indexOf(current),
            onSelect = { open(tabs[it]) },
            modifier = Modifier.padding(inner).consumeWindowInsets(inner),
            // Hide: the bottom bar goes away while the keyboard is up. KeepBehindKeyboard: it stays, covered by the keyboard.
            keyboardMode = NavBarKeyboardMode.Hide,
        ) {
            NavHost(navController = navController, startDestination = HomeGraph) {
                homeGraph(navController, homeScreens)
                featureGraphs(navController)
            }
        }
        // Composed after the NavHost so this callback wins over the NavHost's own back handling: back at
        // another tab's root switches to Home with Home's saved stack, instead of a plain pop that would
        // leave that saved stack behind and bring it back on a later tab switch.
        BackHandler(enabled = current != HomeTab && atTabRoot) {
            open(HomeTab)
        }
    }
}
