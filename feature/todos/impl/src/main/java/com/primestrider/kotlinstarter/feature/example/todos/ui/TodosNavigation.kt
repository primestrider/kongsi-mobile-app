package com.primestrider.kotlinstarter.feature.example.todos.ui

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.primestrider.kotlinstarter.feature.example.TodosRoute

/** The todos page; `<appScheme>://example/todos` (the home-screen widget's tap) opens it too. */
fun NavGraphBuilder.todosDestinations(
    navController: NavController,
    appScheme: String,
) {
    composable<TodosRoute>(deepLinks = listOf(navDeepLink { uriPattern = "$appScheme://example/todos" })) {
        val viewModel: TodosViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        TodosScreen(
            state = state,
            onInputChange = viewModel::onInputChange,
            onAdd = viewModel::add,
            onToggle = viewModel::toggle,
            onRemove = viewModel::remove,
            onClearCompleted = viewModel::clearCompleted,
            onFilterChange = viewModel::setFilter,
            onBack = { navController.popBackStack() },
        )
    }
}
