package com.primestrider.kotlinstarter.app.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.primestrider.kotlinstarter.BuildConfig
import com.primestrider.kotlinstarter.R
import com.primestrider.kotlinstarter.feature.example.CatalogHome
import com.primestrider.kotlinstarter.feature.example.ComponentsHomeRoute
import com.primestrider.kotlinstarter.feature.example.ProfileRoute
import com.primestrider.kotlinstarter.feature.example.animation.animationDestinations
import com.primestrider.kotlinstarter.feature.example.auth.accountDestinations
import com.primestrider.kotlinstarter.feature.example.catalogComponentsDestinations
import com.primestrider.kotlinstarter.feature.example.catalogHomeDestinations
import com.primestrider.kotlinstarter.feature.example.gestures.gestureDestinations
import com.primestrider.kotlinstarter.feature.example.products.ui.productsDestinations
import com.primestrider.kotlinstarter.feature.example.sdk.devicesDestinations
import com.primestrider.kotlinstarter.feature.example.settings.settingsDestinations
import com.primestrider.kotlinstarter.feature.example.todos.ui.todosDestinations
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

// The app wires its features here. Every `// region example:<feature>` block belongs to one example feature:
// `scripts/remove-examples.sh` deletes those blocks together with the feature's modules, and the app still runs.

@Serializable
data object HomeRoute

/** One nested graph per bottom-nav tab; each keeps its own back stack. */
@Serializable
data object HomeGraph

@Serializable
data object ComponentsGraph

@Serializable
data object ProfileGraph

/** A bottom-navigation destination: its label and icon, its nested graph and that graph's start route. */
data class TopLevelTab(
    @StringRes val label: Int,
    val icon: ImageVector,
    val graph: Any,
    val startRoute: Any,
    val graphClass: KClass<*>,
)

/** Always present: the app's Home. */
val HomeTab = TopLevelTab(R.string.nav_home, Icons.Filled.Home, HomeGraph, HomeRoute, HomeGraph::class)

/** The tabs in display order: Home, then whatever the wired features add. One tab hides the bottom bar. */
val topLevelTabs: List<TopLevelTab> =
    buildList {
        add(HomeTab)
        // region example:catalog
        add(
            TopLevelTab(
                R.string.nav_components,
                Icons.AutoMirrored.Filled.List,
                ComponentsGraph,
                ComponentsHomeRoute,
                ComponentsGraph::class,
            ),
        )
        // endregion
        // region example:account
        add(TopLevelTab(R.string.nav_profile, Icons.Filled.Person, ProfileGraph, ProfileRoute, ProfileGraph::class))
        // endregion
    }

/** Renders Home; receives the navigation callback for the routes Home opens. */
typealias HomeScreenContent = @Composable (onOpen: (Any) -> Unit) -> Unit

/** What Home shows: the first entry, or [PlaceholderHomeScreen] when no feature provides one. */
val homeScreens: List<HomeScreenContent> =
    buildList {
        // region example:catalog
        add { onOpen -> CatalogHome(onOpen = onOpen) }
        // endregion
    }

/** Home tab: Home itself and every page reachable from it. Register new Home destinations here. */
fun NavGraphBuilder.homeGraph(
    navController: NavHostController,
    screens: List<HomeScreenContent> = homeScreens,
) {
    navigation<HomeGraph>(startDestination = HomeRoute) {
        composable<HomeRoute> {
            val home = screens.firstOrNull()
            if (home == null) PlaceholderHomeScreen() else home { route -> navController.navigate(route) }
        }
        // region example:catalog
        catalogHomeDestinations(navController)
        // endregion
        // region example:products
        productsDestinations(navController)
        // endregion
        // region example:todos
        todosDestinations(navController, appScheme = BuildConfig.APP_SCHEME)
        // endregion
        // region example:devices
        devicesDestinations(navController, appScheme = BuildConfig.APP_SCHEME)
        // endregion
        // region example:animation
        animationDestinations(navController)
        // endregion
        // region example:gestures
        gestureDestinations(navController)
        // endregion
    }
}

/** The nested graphs of every tab after Home. */
fun NavGraphBuilder.featureGraphs(navController: NavHostController) {
    // region example:catalog
    navigation<ComponentsGraph>(startDestination = ComponentsHomeRoute) {
        catalogComponentsDestinations(navController)
    }
    // endregion
    // region example:account
    navigation<ProfileGraph>(startDestination = ProfileRoute) {
        accountDestinations(navController)
        // region example:settings
        settingsDestinations(navController)
        // endregion
    }
    // endregion
}
