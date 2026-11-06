package com.primestrider.kotlinstarter.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.union
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews

const val APP_NAVIGATION_RAIL_TAG = "app_navigation_rail"

/** What the bottom bar does while the keyboard is up. The rail (wider windows) is unaffected. */
enum class NavBarKeyboardMode {
    /** The bar is removed while the keyboard shows; the screen takes its place. */
    Hide,

    /** The bar stays where it is and the keyboard covers it. */
    KeepBehindKeyboard,
}

/**
 * Top-level navigation that follows the window size: a bottom [AppNavigationBar] on compact width (phones in portrait),
 * a navigation rail at the start from medium width (tablets, foldables, phones in landscape). With one item there is
 * nothing to switch to, so only [content] shows. The type comes from Material's adaptive defaults
 * (`NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo`); the bar keeps this app's look and badges.
 */
@Composable
fun AppNavigationSuite(
    items: List<NavBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    keyboardMode: NavBarKeyboardMode = NavBarKeyboardMode.Hide,
    content: @Composable () -> Unit,
) {
    val type = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo())
    AppNavigationSuiteLayout(
        items,
        selectedIndex,
        onSelect,
        rail = type != NavigationSuiteType.NavigationBar,
        modifier,
        keyboardMode,
        content,
    )
}

/**
 * [AppNavigationSuite] with the layout given. Kept internal: its type (and Material's `NavigationSuiteType`, an inline
 * class from a dependency `:app` does not see) stays out of the public API.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AppNavigationSuiteLayout(
    items: List<NavBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    rail: Boolean,
    modifier: Modifier = Modifier,
    keyboardMode: NavBarKeyboardMode = NavBarKeyboardMode.Hide,
    content: @Composable () -> Unit,
) {
    if (items.size <= 1) {
        Box(modifier.fillMaxSize()) { content() }
        return
    }
    // One fixed tree for both layouts: [content] (the NavHost) stays at the same place in the composition, so folding,
    // unfolding or resizing the window does not rebuild it and lose the screens' remembered state.
    Row(modifier.fillMaxSize()) {
        if (rail) {
            NavigationRail(Modifier.fillMaxHeight().testTag(APP_NAVIGATION_RAIL_TAG)) {
                items.forEachIndexed { index, item ->
                    NavigationRailItem(
                        selected = index == selectedIndex,
                        onClick = { onSelect(index) },
                        icon = { NavBarIcon(item) },
                        label = { Text(item.label) },
                    )
                }
            }
        }
        Column(Modifier.weight(1f)) {
            // Screens pad for the keyboard from the window bottom, but they end above the bar, which the keyboard covers.
            // Either the bar goes away while the keyboard is up, or its height counts as already consumed; otherwise
            // the bar's height is added twice and leaves an empty gap above the keyboard.
            val showBar = !rail && !(keyboardMode == NavBarKeyboardMode.Hide && WindowInsets.isImeVisible)
            var barHeight by remember { mutableIntStateOf(0) }
            // The bar pads itself for the system navigation bar; the rail takes the start-side insets (cutout,
            // landscape navigation bar). Either way the screens must not pad for those again.
            val consumed =
                when {
                    rail -> WindowInsets.safeDrawing.only(WindowInsetsSides.Start)
                    showBar -> WindowInsets.navigationBars.union(WindowInsets(bottom = barHeight))
                    else -> WindowInsets(0, 0, 0, 0)
                }
            Box(Modifier.weight(1f).consumeWindowInsets(consumed)) { content() }
            if (showBar) {
                AppNavigationBar(
                    items = items,
                    selectedIndex = selectedIndex,
                    onSelect = onSelect,
                    modifier = Modifier.onSizeChanged { barHeight = it.height },
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun AppNavigationSuitePreview() {
    PreviewTheme {
        AppNavigationSuite(
            items = listOf(NavBarItem("Home", Icons.Filled.Home), NavBarItem("Profile", Icons.Filled.Person)),
            selectedIndex = 0,
            onSelect = {},
        ) { Text("Content") }
    }
}
