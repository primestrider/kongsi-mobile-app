package com.primestrider.kotlinstarter.feature.example.styling

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
private fun Cell(
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = Spacing.s8,
) {
    Box(
        modifier.size(size).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(Radii.sm)),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary)
    }
}

@Composable
private fun Track(
    caption: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
        Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(Radii.md)),
        ) { content() }
    }
}

@Composable
fun LayoutScreen(onBack: () -> Unit) {
    ExamplePageScaffold(
        title = R.string.example_styling_layout_title,
        subtitle = R.string.example_styling_layout_subtitle,
        onBack = onBack,
    ) {
        DemoSection(R.string.styling_section_row_column) {
            Track("Row") {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                    repeat(3) { Cell("${it + 1}") }
                }
            }
            Track("Column") {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                    repeat(3) { Cell("${it + 1}") }
                }
            }
        }
        DemoSection(R.string.styling_section_arrangement) {
            listOf(
                "Start" to Arrangement.Start,
                "Center" to Arrangement.Center,
                "End" to Arrangement.End,
                "SpaceBetween" to Arrangement.SpaceBetween,
                "SpaceAround" to Arrangement.SpaceAround,
                "SpaceEvenly" to Arrangement.SpaceEvenly,
            ).forEach { (name, arrangement) ->
                Track(name) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = arrangement) {
                        repeat(3) { Cell("${it + 1}") }
                    }
                }
            }
        }
        DemoSection(R.string.styling_section_alignment) {
            listOf(
                "Top" to Alignment.Top,
                "CenterVertically" to Alignment.CenterVertically,
                "Bottom" to Alignment.Bottom,
            ).forEach { (name, alignment) ->
                Track(name) {
                    Row(
                        Modifier.fillMaxWidth().height(Spacing.s16),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
                        verticalAlignment = alignment,
                    ) {
                        Cell("1", size = Spacing.s6)
                        Cell("2", size = Spacing.s8)
                        Cell("3", size = Spacing.s12)
                    }
                }
            }
        }
        DemoSection(R.string.styling_section_weight) {
            Track("weight 1 : 2 : 1") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s1)) {
                    listOf(1f, 2f, 1f).forEach { w ->
                        Box(
                            Modifier
                                .weight(w)
                                .height(Spacing.s10)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(Radii.sm)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                w.toInt().toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                }
            }
        }
        DemoSection(R.string.styling_section_position) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(Spacing.s32)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(Radii.md)),
            ) {
                Cell("TopStart", Modifier.align(Alignment.TopStart), Spacing.s14)
                Cell("Center", Modifier.align(Alignment.Center), Spacing.s14)
                Cell("BottomEnd", Modifier.align(Alignment.BottomEnd), Spacing.s14)
                Cell("offset", Modifier.align(Alignment.TopCenter).offset(x = Spacing.s16, y = Spacing.s2), Spacing.s14)
            }
        }
        DemoSection(R.string.styling_section_grid) {
            // Fixed height: a lazy grid cannot measure itself inside a vertically scrolling parent.
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxWidth().height(Spacing.s32),
                horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
                verticalArrangement = Arrangement.spacedBy(Spacing.s2),
                userScrollEnabled = false,
            ) {
                items((1..6).toList()) { n ->
                    Cell("$n", Modifier.fillMaxWidth(), Spacing.s14)
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun LayoutScreenPreview() {
    PreviewTheme {
        LayoutScreen(onBack = {})
    }
}
