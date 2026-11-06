package com.primestrider.kotlinstarter.feature.example

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.runtime.Composable
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.feature.catalog.impl.R

/** Root of the Components tab: the component example pages, in the same list style as Home. */
@Composable
fun ComponentsHomeScreen(
    entries: List<ExampleEntry>,
    onOpen: (Any) -> Unit,
) {
    HomeScreen(
        title = R.string.catalog_components_title,
        sections =
            listOf(
                ExampleSection(
                    title = R.string.example_home_components_title,
                    description = R.string.example_home_components_description,
                    entries = entries,
                    icon = Icons.Filled.Build,
                ),
            ),
        onOpen = onOpen,
    )
}

@ThemePreviews
@Composable
private fun ComponentsHomeScreenPreview() {
    PreviewTheme {
        ComponentsHomeScreen(entries = componentEntries, onOpen = {})
    }
}
