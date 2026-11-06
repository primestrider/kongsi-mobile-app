package com.primestrider.kotlinstarter.feature.example.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.primestrider.kotlinstarter.core.designsystem.component.AppButton
import com.primestrider.kotlinstarter.core.designsystem.component.ButtonVariant
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.feature.catalog.impl.R
import com.primestrider.kotlinstarter.feature.example.DemoSection
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold

/**
 * App bars, navigation drawers, search and the bottom-sheet scaffold. Each needs the whole screen, so the page lists
 * them and [onOpenDemo] opens one full screen by its [ComponentDemo.id].
 */
@Composable
fun AppBarsScreen(
    onBack: () -> Unit,
    onOpenDemo: (String) -> Unit,
) {
    ExamplePageScaffold(R.string.example_components_app_bars_title, R.string.example_components_app_bars_subtitle, onBack) {
        componentDemos.forEach { demo ->
            DemoSection(demo.title, demo.usage, demo.api, "ComponentDemos.kt") {
                AppButton(
                    text = stringResource(R.string.components_demo_open),
                    onClick = { onOpenDemo(demo.id) },
                    variant = ButtonVariant.SECONDARY,
                    modifier = Modifier.testTag("open-${demo.id}"),
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun AppBarsScreenPreview() {
    PreviewTheme {
        AppBarsScreen(onBack = {}, onOpenDemo = {})
    }
}
