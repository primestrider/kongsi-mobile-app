package com.primestrider.kotlinstarter.app.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.primestrider.kotlinstarter.R
import com.primestrider.kotlinstarter.core.designsystem.component.AppScaffold
import com.primestrider.kotlinstarter.core.designsystem.component.EmptyState
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews

/** Home when no feature provides one (e.g. after removing the examples): replace it with your app's Home. */
@Composable
fun PlaceholderHomeScreen() {
    AppScaffold(title = stringResource(R.string.app_name)) { padding ->
        EmptyState(
            title = stringResource(R.string.app_placeholder_title),
            description = stringResource(R.string.app_placeholder_body),
            icon = Icons.Filled.Home,
            modifier = Modifier.fillMaxSize().padding(padding),
        )
    }
}

@ThemePreviews
@Composable
private fun PlaceholderHomeScreenPreview() {
    PreviewTheme {
        PlaceholderHomeScreen()
    }
}
