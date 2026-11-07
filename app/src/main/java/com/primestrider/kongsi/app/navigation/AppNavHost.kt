package com.primestrider.kongsi.app.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.primestrider.kongsi.R
import com.primestrider.kongsi.core.designsystem.component.NavBarAction
import com.primestrider.kongsi.feature.bill.ui.BillHome
import com.primestrider.kongsi.feature.bill.ui.billDestinations
import com.primestrider.kongsi.feature.bill.ui.rememberNewBillAction
import com.primestrider.kongsi.feature.profile.ui.ProfileTab
import com.primestrider.kongsi.feature.profile.ui.profileDestinations
import kotlinx.serialization.Serializable

// The app wires its features here: the tabs (pages of the swipeable shell) and every other destination.

/** The shell's start destination: the swipeable tabs. Every other route opens full screen on top of it. */
@Serializable
data object TabsRoute

/** Renders a tab's root page; receives the navigation callback for the routes the page opens. */
typealias TabContent = @Composable (onOpen: (Any) -> Unit) -> Unit

/** A top-level page: its label and icon in the navigation bar, and its content. */
data class TopLevelTab(
    @StringRes val label: Int,
    val icon: ImageVector,
    val content: TabContent,
)

/** What Home shows: the first entry, or [PlaceholderHomeScreen] when no feature provides one. */
val homeScreens: List<TabContent> =
    buildList {
        add { onOpen -> BillHome(onOpen = onOpen) }
    }

/** Always present and always first: the app's Home. */
fun homeTab(screens: List<TabContent> = homeScreens): TopLevelTab =
    TopLevelTab(R.string.nav_home, Icons.Filled.Home) { onOpen ->
        val home = screens.firstOrNull()
        if (home == null) PlaceholderHomeScreen() else home(onOpen)
    }

/** The tabs in display order (swipe moves between neighbours). One tab hides the navigation bar. */
val topLevelTabs: List<TopLevelTab> =
    listOf(
        homeTab(),
        TopLevelTab(R.string.nav_profile, Icons.Filled.Person) { onOpen -> ProfileTab(onOpen = onOpen) },
    )

/** Builds the raised action in the middle of the navigation bar (a FAB on the rail); receives the navigation callback. */
typealias ShellAction = @Composable (onOpen: (Any) -> Unit) -> NavBarAction

/** The app's main action, one tap away from every tab: start a new bill. */
val shellAction: ShellAction = { onOpen -> rememberNewBillAction(onOpen) }

/** Every destination outside the tabs. Register new feature destinations here. */
fun NavGraphBuilder.featureDestinations(navController: NavHostController) {
    billDestinations(navController)
    profileDestinations(navController)
}
