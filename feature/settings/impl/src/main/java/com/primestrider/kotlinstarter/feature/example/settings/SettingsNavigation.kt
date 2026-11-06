package com.primestrider.kotlinstarter.feature.example.settings

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.primestrider.kotlinstarter.feature.example.SettingsRoute

/** The Settings page (theme, language, developer options). */
fun NavGraphBuilder.settingsDestinations(navController: NavController) {
    composable<SettingsRoute> {
        val viewModel: SettingsViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        // The language can change outside the app (system per-app language settings).
        LifecycleResumeEffect(Unit) {
            viewModel.refreshLanguage()
            onPauseOrDispose {}
        }
        SettingsScreen(
            state = state,
            onThemeModeChange = viewModel::setThemeMode,
            onLanguageChange = viewModel::setLanguage,
            onBack = { navController.popBackStack() },
        )
    }
}
