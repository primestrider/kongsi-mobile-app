package com.primestrider.kotlinstarter.feature.example.styling

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.primestrider.kotlinstarter.core.designsystem.component.ThemeModeSelector
import com.primestrider.kotlinstarter.core.designsystem.theme.AppThemeTokens
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Radii
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.core.model.ThemeMode
import com.primestrider.kotlinstarter.feature.catalog.impl.R
import com.primestrider.kotlinstarter.feature.example.DemoSection
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold

@Composable
fun ThemeScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onBack: () -> Unit,
) {
    val colors = AppThemeTokens.colors
    ExamplePageScaffold(
        title = R.string.example_styling_theme_title,
        subtitle = R.string.example_styling_theme_subtitle,
        onBack = onBack,
    ) {
        DemoSection(R.string.styling_section_mode) {
            ThemeModeSelector(selected = themeMode, onSelect = onThemeModeChange)
        }
        DemoSection(R.string.styling_section_surfaces) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                SurfaceTile(R.string.styling_surface_background, colors.background, colors.foreground)
                SurfaceTile(R.string.styling_surface_card, colors.card, colors.cardForeground)
                SurfaceTile(R.string.styling_surface_secondary, colors.secondary, colors.secondaryForeground)
            }
        }
        DemoSection(R.string.styling_section_tokens) {
            semanticColorEntries(colors).forEach { (name, color) -> ColorSwatchRow(name, color) }
        }
    }
}

@Composable
private fun RowScope.SurfaceTile(
    @StringRes label: Int,
    background: Color,
    foreground: Color,
) {
    val shape = RoundedCornerShape(Radii.md)
    Box(
        Modifier
            .weight(1f)
            .height(Spacing.s20)
            .background(background, shape)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = foreground)
    }
}

@ThemePreviews
@Composable
private fun ThemeScreenPreview() {
    PreviewTheme {
        ThemeScreen(themeMode = ThemeMode.SYSTEM, onThemeModeChange = {}, onBack = {})
    }
}
