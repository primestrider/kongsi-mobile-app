package com.primestrider.kotlinstarter.feature.example.styling

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Radii
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.feature.catalog.impl.R
import com.primestrider.kotlinstarter.feature.example.DemoSection
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold

private val spacingTokens: List<Pair<String, Dp>> =
    listOf(
        "s0" to Spacing.s0,
        "s0.5" to Spacing.s0_5,
        "s1" to Spacing.s1,
        "s1.5" to Spacing.s1_5,
        "s2" to Spacing.s2,
        "s2.5" to Spacing.s2_5,
        "s3" to Spacing.s3,
        "s3.5" to Spacing.s3_5,
        "s4" to Spacing.s4,
        "s5" to Spacing.s5,
        "s6" to Spacing.s6,
        "s7" to Spacing.s7,
        "s8" to Spacing.s8,
        "s9" to Spacing.s9,
        "s10" to Spacing.s10,
        "s11" to Spacing.s11,
        "s12" to Spacing.s12,
        "s14" to Spacing.s14,
        "s16" to Spacing.s16,
        "s20" to Spacing.s20,
        "s24" to Spacing.s24,
        "s28" to Spacing.s28,
        "s32" to Spacing.s32,
        "s36" to Spacing.s36,
        "s40" to Spacing.s40,
        "s44" to Spacing.s44,
        "s48" to Spacing.s48,
        "s52" to Spacing.s52,
        "s56" to Spacing.s56,
        "s60" to Spacing.s60,
        "s64" to Spacing.s64,
        "s72" to Spacing.s72,
        "s80" to Spacing.s80,
        "s96" to Spacing.s96,
    )

@Composable
fun SpacingScreen(onBack: () -> Unit) {
    ExamplePageScaffold(
        title = R.string.example_styling_spacing_title,
        subtitle = R.string.example_styling_spacing_subtitle,
        onBack = onBack,
    ) {
        DemoSection(R.string.styling_section_spacing_scale) {
            spacingTokens.forEach { (name, size) ->
                Column {
                    Text("$name · ${size.value.toInt()}dp", style = MaterialTheme.typography.labelMedium)
                    // The bar is clamped to the available width by its parent constraints.
                    Box(Modifier.fillMaxWidth()) {
                        Box(
                            Modifier
                                .width(size)
                                .height(Spacing.s2)
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                }
            }
        }
        DemoSection(R.string.styling_section_padding_gap) {
            listOf(Spacing.s1, Spacing.s2, Spacing.s4, Spacing.s6).forEach { pad ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(Radii.md)),
                ) {
                    Text(
                        "padding ${pad.value.toInt()}dp",
                        Modifier
                            .padding(pad)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            listOf(Spacing.s1, Spacing.s4).forEach { gap ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(3) {
                        Box(
                            Modifier
                                .width(Spacing.s10)
                                .height(Spacing.s10)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(Radii.sm)),
                        )
                    }
                    Text("gap ${gap.value.toInt()}dp", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        DemoSection(R.string.styling_section_margin) {
            // Compose has no margin modifier: outer spacing is padding applied before the border/background.
            listOf(Spacing.s1, Spacing.s4, Spacing.s8).forEach { margin ->
                Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant)) {
                    Text(
                        "margin ${margin.value.toInt()}dp",
                        Modifier
                            .padding(margin)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(Radii.sm))
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary), RoundedCornerShape(Radii.sm))
                            .padding(Spacing.s2),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
        DemoSection(R.string.styling_section_negative_margin) {
            // padding() rejects negative values, so a negative margin is a negative offset.
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(Radii.md))
                    .padding(Spacing.s6),
            ) {
                Text(
                    "offset y = -${Spacing.s4.value.toInt()}dp",
                    Modifier
                        .offset(y = -Spacing.s4)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(Radii.sm))
                        .padding(Spacing.s3),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    "offset x = -${Spacing.s2.value.toInt()}dp",
                    Modifier
                        .offset(x = -Spacing.s2)
                        .padding(top = Spacing.s2)
                        .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(Radii.sm))
                        .padding(Spacing.s3),
                    color = MaterialTheme.colorScheme.onSecondary,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun SpacingScreenPreview() {
    PreviewTheme {
        SpacingScreen(onBack = {})
    }
}
