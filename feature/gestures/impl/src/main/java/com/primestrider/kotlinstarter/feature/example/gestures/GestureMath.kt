package com.primestrider.kotlinstarter.feature.example.gestures

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

/** This list with the item at [from] moved to [to]; unchanged when either index is outside the list. */
fun <T> List<T>.moved(
    from: Int,
    to: Int,
): List<T> {
    if (from !in indices || to !in indices || from == to) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}

/** The resting positions of the anchored sheet on the Drag page. */
enum class SheetAnchor { Collapsed, Half, Expanded }

/** Zoom limits of the Transform page. */
const val MIN_ZOOM = 1f
const val MAX_ZOOM = 4f

/** The zoom a double tap jumps to from the fit size. */
const val DOUBLE_TAP_ZOOM = 2.5f

/**
 * [pan] limited so content of [size] scaled by [scale] (around its centre) never leaves a gap at the edges: at most
 * `size * (scale - 1) / 2` per axis, so at scale 1 there is nothing to pan.
 */
fun clampPan(
    pan: Offset,
    scale: Float,
    size: Size,
): Offset {
    val maxX = (size.width * (scale - 1) / 2).coerceAtLeast(0f)
    val maxY = (size.height * (scale - 1) / 2).coerceAtLeast(0f)
    return Offset(pan.x.coerceIn(-maxX, maxX), pan.y.coerceIn(-maxY, maxY))
}

/** Double tap: zoom in to [DOUBLE_TAP_ZOOM] from below it, otherwise back to [MIN_ZOOM]. */
fun toggleZoom(scale: Float): Float = if (scale < DOUBLE_TAP_ZOOM - 0.01f) DOUBLE_TAP_ZOOM else MIN_ZOOM

/** Where a released card goes on the Swipe & fling page. */
enum class SwipeOutcome { Left, Right, Return }

/** Share of the width a card must travel to leave the stack. */
private const val SWIPE_DISTANCE_FRACTION = 0.4f

/** A release faster than this (px/s) leaves the stack whatever the distance: a quick flick is enough. */
private const val SWIPE_VELOCITY = 1500f

/** The card leaves to the side it was dragged or flung towards, or springs back when neither went far or fast enough. */
fun swipeOutcome(
    offsetX: Float,
    velocityX: Float,
    widthPx: Float,
): SwipeOutcome =
    when {
        kotlin.math.abs(velocityX) > SWIPE_VELOCITY -> if (velocityX > 0) SwipeOutcome.Right else SwipeOutcome.Left
        kotlin.math.abs(offsetX) > widthPx * SWIPE_DISTANCE_FRACTION -> if (offsetX > 0) SwipeOutcome.Right else SwipeOutcome.Left
        else -> SwipeOutcome.Return
    }

/**
 * A collapsing header taking its share of a scroll: [scrollDelta] < 0 (content moving up) shrinks it towards [min],
 * > 0 grows it towards [max]. Returns the new height and the part of the delta it consumed; the rest is left for
 * the list. The caller decides when growing is allowed (only once the list is back at its top).
 */
fun headerHeightAfter(
    scrollDelta: Float,
    current: Float,
    min: Float,
    max: Float,
): Pair<Float, Float> {
    val next = (current + scrollDelta).coerceIn(min, max)
    return next to (next - current)
}

/**
 * [strokes] thinned to at most [maxPoints] points in all, so a long drawing session still fits in the saved-state
 * Bundle (the binder transaction limit is about 1 MB). Every stroke keeps its first and last point; the points in
 * between are kept every n-th. With more strokes than the budget allows, only the newest ones are kept.
 */
fun compactStrokes(
    strokes: List<List<Offset>>,
    maxPoints: Int,
): List<List<Offset>> {
    val total = strokes.sumOf { it.size }
    if (total <= maxPoints) return strokes
    // Endpoints alone cost up to 2 points per stroke.
    val kept = if (strokes.size * 2 > maxPoints / 2) strokes.takeLast(maxPoints / 4) else strokes
    val budget = (maxPoints - kept.size * 2).coerceAtLeast(1)
    val stride = (kept.sumOf { it.size } + budget - 1) / budget
    return kept.map { stroke ->
        if (stroke.size <= 2 || stride <= 1) return@map stroke
        val thinned = stroke.filterIndexed { index, _ -> index % stride == 0 }
        if ((stroke.size - 1) % stride == 0) thinned else thinned + stroke.last()
    }
}
