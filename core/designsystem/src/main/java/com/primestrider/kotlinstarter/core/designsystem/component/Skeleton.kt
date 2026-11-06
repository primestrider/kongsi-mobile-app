package com.primestrider.kotlinstarter.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.primestrider.kotlinstarter.core.designsystem.R
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Radii
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews

private const val PULSE_MS = 800
private const val SHIMMER_MS = 1_200

/** Width of the moving highlight as a fraction of the window width. */
private const val SHIMMER_BAND = 0.6f

@Composable
private fun pulseAlpha(): Float {
    val alpha by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(PULSE_MS), RepeatMode.Reverse),
        label = "skeleton-alpha",
    )
    return alpha
}

/** Progress 0..1 of the moving highlight; the infinite transition exists only while this is composed. */
@Composable
private fun shimmerProgress(): State<Float> =
    rememberInfiniteTransition(label = "skeleton-shimmer").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(SHIMMER_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "skeleton-shimmer-progress",
    )

/**
 * Placeholder block; size it with [modifier]. Pulses by default; with [shimmer] a light band (sized and timed against the window, so every bar sweeps in step) sweeps
 * across it from the start edge (the left in LTR, the right in RTL) instead.
 */
@Composable
fun Skeleton(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(Radii.sm),
    shimmer: Boolean = false,
) {
    if (shimmer) {
        ShimmerBar(modifier, shape, shimmerProgress())
    } else {
        SkeletonBar(modifier, shape, pulseAlpha())
    }
}

@Composable
private fun SkeletonBar(
    modifier: Modifier,
    shape: Shape,
    alpha: Float,
) {
    Box(modifier.alpha(alpha).background(MaterialTheme.colorScheme.surfaceContainerHigh, shape))
}

@Composable
private fun ShimmerBar(
    modifier: Modifier,
    shape: Shape,
    progress: State<Float>,
) {
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlight = MaterialTheme.colorScheme.surfaceContainerHighest
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    // Band width and travel come from the window, not from this bar, so bars of any width sweep in step.
    val span =
        LocalWindowInfo.current.containerSize.width
            .toFloat()
    Box(
        modifier
            .clip(shape)
            .background(base)
            .drawWithCache {
                val band = span * SHIMMER_BAND
                val brush = Brush.horizontalGradient(listOf(Color.Transparent, highlight, Color.Transparent), startX = 0f, endX = band)
                onDrawBehind {
                    // The band travels from just before the start edge across the window width; progress is read
                    // in draw so each frame only redraws and nothing is allocated.
                    val x = -band + (span + band) * progress.value
                    val left = if (rtl) size.width - x - band else x
                    translate(left = left) { drawRect(brush, size = Size(band, size.height)) }
                }
            },
    )
}

/** Placeholder paragraph of [lines] bars, the last one shorter; announced as "Loading". */
@Composable
fun SkeletonText(
    lines: Int = 3,
    modifier: Modifier = Modifier,
    lastLineFraction: Float = 0.6f,
    shimmer: Boolean = false,
) {
    val loading = stringResource(R.string.component_loading)
    // One shared pulse for all bars instead of an infinite transition per bar.
    val alpha = if (shimmer) 1f else pulseAlpha()
    val progress = if (shimmer) shimmerProgress() else null
    Column(
        modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = loading },
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
    ) {
        repeat(lines) { index ->
            val fraction = if (index == lines - 1) lastLineFraction else 1f
            val barModifier = Modifier.fillMaxWidth(fraction).height(Spacing.s4)
            if (progress != null) {
                ShimmerBar(barModifier, RoundedCornerShape(Radii.sm), progress)
            } else {
                SkeletonBar(barModifier, RoundedCornerShape(Radii.sm), alpha)
            }
        }
    }
}

@ThemePreviews
@Composable
private fun SkeletonPreview() {
    PreviewTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Skeleton(Modifier.height(24.dp))
            SkeletonText(lines = 3)
        }
    }
}
