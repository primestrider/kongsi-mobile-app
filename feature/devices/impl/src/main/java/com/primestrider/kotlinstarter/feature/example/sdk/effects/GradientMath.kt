package com.primestrider.kotlinstarter.feature.example.sdk.effects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Start and end of a linear gradient across [size] at [angleDegrees]: a line through the center of the box
 * with a direction of (cos θ, sin θ) and a length of one box side along that direction. 0° runs left to right,
 * 90° top to bottom.
 */
fun gradientOffsets(
    angleDegrees: Float,
    size: Size,
): Pair<Offset, Offset> {
    val radians = Math.toRadians(angleDegrees.toDouble())
    val dx = (cos(radians) / 2).toFloat()
    val dy = (sin(radians) / 2).toFloat()
    return Offset((0.5f - dx) * size.width, (0.5f - dy) * size.height) to
        Offset((0.5f + dx) * size.width, (0.5f + dy) * size.height)
}

private const val MAX_BLUR_DP = 24f

/** Blur radius for an intensity slider of 0..100: `intensity / 100 × 24 dp`. */
fun blurRadius(intensity: Float): Dp = (intensity * MAX_BLUR_DP / 100f).dp
