package com.primestrider.kotlinstarter.feature.example.styling

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.primestrider.kotlinstarter.core.designsystem.theme.Radii
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing

/** A color square with its token name. Swatches are the only place raw palette colors are shown. */
@Composable
internal fun ColorSwatchRow(
    name: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
    ) {
        Swatch(color)
        Text(name, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
internal fun Swatch(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Radii.md)
    Box(
        modifier
            .size(Spacing.s8)
            .background(color, shape)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), shape),
    )
}

/** One palette step: a colored chip with its numeric label, in a row of equal-width chips. */
@Composable
internal fun RowScope.PaletteStep(
    step: Int,
    color: Color,
) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().height(Spacing.s8).background(color))
        Text(step.toString(), style = MaterialTheme.typography.labelSmall)
    }
}
