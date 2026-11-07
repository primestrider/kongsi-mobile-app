package com.primestrider.kongsi.app.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.primestrider.kongsi.core.designsystem.component.AppNavigationSuite
import com.primestrider.kongsi.core.designsystem.component.LocalMessenger
import com.primestrider.kongsi.core.designsystem.component.MessengerHost
import com.primestrider.kongsi.core.designsystem.component.NavBarAction
import com.primestrider.kongsi.core.designsystem.component.NavBarItem
import com.primestrider.kongsi.core.designsystem.component.NavBarKeyboardMode
import kotlinx.coroutines.launch

const val TABS_PAGER_TAG = "tabs_pager"

/**
 * App shell: one NavHost whose start destination is the tabs. Every other route opens full screen on top of them,
 * and back returns to the tab it came from.
 */
@Composable
fun MainShell(
    navController: NavHostController = rememberNavController(),
    tabs: List<TopLevelTab> = topLevelTabs,
    action: ShellAction? = shellAction,
    extraDestinations: NavGraphBuilder.() -> Unit = { featureDestinations(navController) },
) {
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        snackbarHost = { MessengerHost(LocalMessenger.current, snackbarHostState) },
        // The shell only owns the navigation; screens handle the system bars with their own AppScaffold.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { inner ->
        NavHost(
            navController = navController,
            startDestination = TabsRoute,
            modifier = Modifier.padding(inner).consumeWindowInsets(inner),
        ) {
            composable<TabsRoute> {
                val onOpen: (Any) -> Unit = { route -> navController.navigate(route) }
                SwipeableTabs(tabs, onOpen = onOpen, centerAction = action?.invoke(onOpen))
            }
            extraDestinations()
        }
    }
}

/**
 * The tabs as pages: swipe left or right between neighbours, or tap the navigation bar (a rail on wider windows).
 * [centerAction] is the raised button in the middle of the bar (a FAB on the rail).
 * Only the visible tab is composed (an off-screen tab could take initial focus and scroll itself into view); a tab's
 * saveable state (scroll position, drafts) and its ViewModels survive switching. Back on any tab other than the first
 * returns to the first.
 */
@Composable
fun SwipeableTabs(
    tabs: List<TopLevelTab>,
    onOpen: (Any) -> Unit,
    centerAction: NavBarAction? = null,
) {
    val pagerState = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()
    AppNavigationSuite(
        items = tabs.map { NavBarItem(stringResource(it.label), it.icon) },
        selectedIndex = pagerState.targetPage,
        onSelect = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
        // Hide: the bottom bar goes away while the keyboard is up. KeepBehindKeyboard: it stays, covered by the keyboard.
        keyboardMode = NavBarKeyboardMode.Hide,
        centerAction = centerAction,
    ) {
        HorizontalPager(
            state = pagerState,
            key = { it },
            modifier = Modifier.testTag(TABS_PAGER_TAG),
        ) { page -> tabs[page].content(onOpen) }
    }
    BackHandler(enabled = pagerState.currentPage != 0) {
        scope.launch { pagerState.animateScrollToPage(0) }
    }
}
