package com.primestrider.kotlinstarter.feature.example

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.primestrider.kotlinstarter.core.datastore.ThemePreferenceRepository
import com.primestrider.kotlinstarter.core.model.ThemeMode
import com.primestrider.kotlinstarter.feature.catalog.api.CatalogEntry
import com.primestrider.kotlinstarter.feature.catalog.api.CatalogSection
import com.primestrider.kotlinstarter.feature.example.components.ActionsScreen
import com.primestrider.kotlinstarter.feature.example.components.AppBarsScreen
import com.primestrider.kotlinstarter.feature.example.components.CarouselScreen
import com.primestrider.kotlinstarter.feature.example.components.DisplayScreen
import com.primestrider.kotlinstarter.feature.example.components.FeedbackScreen
import com.primestrider.kotlinstarter.feature.example.components.FeedbackViewModel
import com.primestrider.kotlinstarter.feature.example.components.InputsScreen
import com.primestrider.kotlinstarter.feature.example.components.OverlaysScreen
import com.primestrider.kotlinstarter.feature.example.components.PickersScreen
import com.primestrider.kotlinstarter.feature.example.components.componentDemos
import com.primestrider.kotlinstarter.feature.example.form.FormExampleScreen
import com.primestrider.kotlinstarter.feature.example.form.FormExampleViewModel
import com.primestrider.kotlinstarter.feature.example.styling.AppearanceScreen
import com.primestrider.kotlinstarter.feature.example.styling.ColorsScreen
import com.primestrider.kotlinstarter.feature.example.styling.LayoutScreen
import com.primestrider.kotlinstarter.feature.example.styling.SizingScreen
import com.primestrider.kotlinstarter.feature.example.styling.SpacingScreen
import com.primestrider.kotlinstarter.feature.example.styling.ThemeScreen
import com.primestrider.kotlinstarter.feature.example.styling.TypographyScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/** Home content built from every wired feature's contributions. */
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        sections: Set<@JvmSuppressWildcards CatalogSection>,
        entries: Set<@JvmSuppressWildcards CatalogEntry>,
    ) : ViewModel() {
        val sections: List<ExampleSection> = buildSections(sections, entries)
    }

/** The Home tab's root screen; `:app` shows it as its Home destination. */
@Composable
fun CatalogHome(
    onOpen: (Any) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    HomeScreen(sections = viewModel.sections, onOpen = onOpen)
}

/** State holder of the Theme styling page: reads and writes the stored theme preference. */
@HiltViewModel
class ThemeViewModel
    @Inject
    constructor(
        private val repository: ThemePreferenceRepository,
    ) : ViewModel() {
        val themeMode: StateFlow<ThemeMode> = repository.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

        fun setThemeMode(mode: ThemeMode) {
            viewModelScope.launch {
                try {
                    repository.setThemeMode(mode)
                } catch (_: IOException) {
                    // Persisting failed; the stored preference (and so the state) is unchanged.
                }
            }
        }
    }

/** Catalog pages reachable from Home: the form example and the styling pages. */
fun NavGraphBuilder.catalogHomeDestinations(navController: NavController) {
    val back: () -> Unit = { navController.popBackStack() }
    composable<FormExampleRoute> {
        val viewModel: FormExampleViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        FormExampleScreen(
            state = state,
            onChange = viewModel::onChange,
            onSubmit = viewModel::submit,
            onClear = viewModel::clear,
            onBack = back,
        )
    }
    composable<ColorsRoute> { ColorsScreen(onBack = back) }
    composable<TypographyRoute> { TypographyScreen(onBack = back) }
    composable<SpacingRoute> { SpacingScreen(onBack = back) }
    composable<SizingRoute> { SizingScreen(onBack = back) }
    composable<LayoutRoute> { LayoutScreen(onBack = back) }
    composable<AppearanceRoute> { AppearanceScreen(onBack = back) }
    composable<ThemeRoute> {
        val viewModel: ThemeViewModel = hiltViewModel()
        val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
        ThemeScreen(themeMode = themeMode, onThemeModeChange = viewModel::setThemeMode, onBack = back)
    }
}

/** The Components tab: its root list and the component pages. Register inside the tab's nested graph. */
fun NavGraphBuilder.catalogComponentsDestinations(navController: NavController) {
    val back: () -> Unit = { navController.popBackStack() }
    composable<ComponentsHomeRoute> {
        ComponentsHomeScreen(entries = componentEntries, onOpen = { route -> navController.navigate(route) })
    }
    composable<ActionsRoute> { ActionsScreen(onBack = back) }
    composable<InputsRoute> { InputsScreen(onBack = back) }
    composable<DisplayRoute> { DisplayScreen(onBack = back) }
    composable<OverlaysRoute> { OverlaysScreen(onBack = back) }
    composable<FeedbackRoute> {
        val viewModel: FeedbackViewModel = hiltViewModel()
        FeedbackScreen(onBack = back, onSaveSucceeded = viewModel::saveSucceeded, onSaveFailed = viewModel::saveFailed)
    }
    composable<PickersRoute> { PickersScreen(onBack = back) }
    composable<AppBarsRoute> { AppBarsScreen(onBack = back, onOpenDemo = { navController.navigate(ComponentDemoRoute(it)) }) }
    composable<ComponentDemoRoute> { entry ->
        val demo = componentDemos.firstOrNull { it.id == entry.toRoute<ComponentDemoRoute>().id }
        if (demo != null) demo.content(back) else LaunchedEffect(Unit) { back() }
    }
    composable<CarouselRoute> { CarouselScreen(onBack = back) }
}
