package com.primestrider.kotlinstarter.feature.example.styling

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Radii
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.feature.catalog.impl.R
import com.primestrider.kotlinstarter.feature.example.DemoSection
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold

@Composable
private fun DemoBox(
    label: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(Radii.md)),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary)
    }
}

@Composable
fun SizingScreen(onBack: () -> Unit) {
    ExamplePageScaffold(
        title = R.string.example_styling_sizing_title,
        subtitle = R.string.example_styling_sizing_subtitle,
        onBack = onBack,
    ) {
        DemoSection(R.string.styling_section_width_fraction) {
            listOf(0.25f, 0.5f, 0.75f, 1f).forEach { fraction ->
                DemoBox("${(fraction * 100).toInt()}%", Modifier.fillMaxWidth(fraction).height(Spacing.s10))
            }
        }
        DemoSection(R.string.styling_section_fixed_sizes) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2), verticalAlignment = Alignment.Bottom) {
                listOf<Pair<String, Dp>>(
                    "s8" to Spacing.s8,
                    "s12" to Spacing.s12,
                    "s16" to Spacing.s16,
                    "s20" to Spacing.s20,
                ).forEach { (name, size) -> DemoBox(name, Modifier.size(size)) }
            }
            listOf(Spacing.s8, Spacing.s12, Spacing.s16).forEach { h ->
                DemoBox("height ${h.value.toInt()}dp", Modifier.fillMaxWidth().height(h))
            }
        }
        DemoSection(R.string.styling_section_aspect_ratio) {
            listOf("1:1" to 1f, "16:9" to 16f / 9f, "4:3" to 4f / 3f).forEach { (label, ratio) ->
                DemoBox(label, Modifier.width(Spacing.s40).aspectRatio(ratio))
            }
        }
        DemoSection(R.string.styling_section_min_max) {
            DemoBox("widthIn(max = s48)", Modifier.fillMaxWidth().widthIn(max = Spacing.s48).height(Spacing.s10))
            DemoBox("widthIn(min = s48)", Modifier.widthIn(min = Spacing.s48).height(Spacing.s10))
            DemoBox("heightIn(min = s16)", Modifier.fillMaxWidth().heightIn(min = Spacing.s16))
        }
    }
}

@ThemePreviews
@Composable
private fun SizingScreenPreview() {
    PreviewTheme {
        SizingScreen(onBack = {})
    }
}
