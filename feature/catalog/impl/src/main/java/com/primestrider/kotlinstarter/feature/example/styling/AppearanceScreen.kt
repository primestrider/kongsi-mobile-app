package com.primestrider.kotlinstarter.feature.example.styling

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.primestrider.kotlinstarter.core.designsystem.theme.AppThemeTokens
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Radii
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.feature.catalog.impl.R
import com.primestrider.kotlinstarter.feature.example.DemoSection
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold

@Composable
private fun Tile(
    label: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier.size(Spacing.s16), contentAlignment = Alignment.Center) {
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun AppearanceScreen(onBack: () -> Unit) {
    val colors = AppThemeTokens.colors
    ExamplePageScaffold(
        title = R.string.example_styling_appearance_title,
        subtitle = R.string.example_styling_appearance_subtitle,
        onBack = onBack,
    ) {
        DemoSection(R.string.styling_section_border) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s3), verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
                listOf(1.dp to colors.border, 2.dp to colors.primary, 4.dp to colors.destructive).forEach { (w, c) ->
                    Tile("${w.value.toInt()}dp", Modifier.border(BorderStroke(w, c), RoundedCornerShape(Radii.md)))
                }
            }
        }
        DemoSection(R.string.styling_section_radius) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s3), verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
                listOf<Pair<String, Dp>>(
                    "none" to Radii.none,
                    "sm" to Radii.sm,
                    "md" to Radii.md,
                    "lg" to Radii.lg,
                    "xl" to Radii.xl,
                    "2xl" to Radii.xl2,
                    "3xl" to Radii.xl3,
                    "full" to Radii.full,
                ).forEach { (name, r) ->
                    Tile(name, Modifier.background(colors.secondary, RoundedCornerShape(r)))
                }
            }
        }
        DemoSection(R.string.styling_section_corners) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s3), verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
                Tile("top", Modifier.background(colors.secondary, RoundedCornerShape(topStart = Radii.xl, topEnd = Radii.xl)))
                Tile("bottom", Modifier.background(colors.secondary, RoundedCornerShape(bottomStart = Radii.xl, bottomEnd = Radii.xl)))
                Tile("start", Modifier.background(colors.secondary, RoundedCornerShape(topStart = Radii.xl, bottomStart = Radii.xl)))
                Tile("end", Modifier.background(colors.secondary, RoundedCornerShape(topEnd = Radii.xl, bottomEnd = Radii.xl)))
            }
        }
        DemoSection(R.string.styling_section_elevation) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s4), verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
                listOf(0.dp, 1.dp, 2.dp, 4.dp, 8.dp, 16.dp).forEach { e ->
                    Surface(shadowElevation = e, shape = RoundedCornerShape(Radii.md), color = colors.card) {
                        Tile("${e.value.toInt()}dp")
                    }
                }
            }
        }
        DemoSection(R.string.styling_section_alpha) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s3), verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
                listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { a ->
                    Tile(
                        "${(a * 100).toInt()}%",
                        Modifier.alpha(a).background(colors.primary, RoundedCornerShape(Radii.md)),
                    )
                }
            }
        }
        DemoSection(R.string.styling_section_combined) {
            Surface(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Radii.xl),
                color = colors.card,
                shadowElevation = 4.dp,
                border = BorderStroke(1.dp, colors.border),
            ) {
                Column(Modifier.padding(Spacing.s4), verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
                    Text(stringResource(R.string.styling_combined_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.styling_combined_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                    )
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun AppearanceScreenPreview() {
    PreviewTheme {
        AppearanceScreen(onBack = {})
    }
}
