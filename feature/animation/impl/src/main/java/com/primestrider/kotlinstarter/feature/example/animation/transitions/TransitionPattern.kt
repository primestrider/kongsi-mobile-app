package com.primestrider.kotlinstarter.feature.example.animation.transitions

import androidx.annotation.StringRes
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.primestrider.kotlinstarter.feature.animation.impl.R

/** Material Motion durations, distances and scales used by [TransitionPattern]. */
object MotionSpec {
    const val SHARED_AXIS_MS = 300
    const val FADE_THROUGH_MS = 300
    const val FADE_THROUGH_OUT_MS = 90
    const val FADE_IN_MS = 150
    const val FADE_OUT_MS = 75
    const val CONTAINER_MS = 300
    val SHARED_AXIS_SLIDE = 30.dp
    const val SHARED_AXIS_Z_SCALE_IN = 0.8f
    const val SHARED_AXIS_Z_SCALE_OUT = 1.1f
    const val FADE_THROUGH_SCALE_IN = 0.92f
}

/** The Material page transition patterns shown on the Page transitions page, with the Compose API behind each. */
enum class TransitionPattern(
    @StringRes val label: Int,
    @StringRes val usage: Int,
    val api: String,
) {
    CONTAINER_TRANSFORM(
        R.string.anim_transitions_container,
        R.string.anim_transitions_container_usage,
        "SharedTransitionLayout + Modifier.sharedBounds",
    ),
    SHARED_AXIS_X(R.string.anim_transitions_axis_x, R.string.anim_transitions_axis_x_usage, "slideInHorizontally + fadeIn"),
    SHARED_AXIS_Y(R.string.anim_transitions_axis_y, R.string.anim_transitions_axis_y_usage, "slideInVertically + fadeIn"),
    SHARED_AXIS_Z(R.string.anim_transitions_axis_z, R.string.anim_transitions_axis_z_usage, "scaleIn + fadeIn"),
    FADE_THROUGH(
        R.string.anim_transitions_fade_through,
        R.string.anim_transitions_fade_through_usage,
        "fadeIn(delayMillis) + scaleIn",
    ),
    FADE(R.string.anim_transitions_fade, R.string.anim_transitions_fade_usage, "fadeIn + fadeOut"),
}

// The incoming page waits for the outgoing one to fade, as in Material's shared axis and fade through.
private val fadeOutFirst = tween<Float>(MotionSpec.FADE_THROUGH_OUT_MS, easing = FastOutLinearInEasing)

private fun fadeInAfter(totalMs: Int) =
    tween<Float>(totalMs - MotionSpec.FADE_THROUGH_OUT_MS, delayMillis = MotionSpec.FADE_THROUGH_OUT_MS, easing = LinearOutSlowInEasing)

/**
 * Sign of the horizontal slide for the incoming page: forward comes from the end side, back from the start side,
 * so in right-to-left layouts the motion mirrors.
 */
private fun axisSign(
    forward: Boolean,
    layoutDirection: LayoutDirection,
): Int = if (forward == (layoutDirection == LayoutDirection.Ltr)) 1 else -1

/**
 * Transition for the page that appears. [forward] is true when navigating deeper and false when going back.
 * [slidePx] is [MotionSpec.SHARED_AXIS_SLIDE] in pixels; [layoutDirection] mirrors shared axis X.
 */
fun TransitionPattern.enter(
    forward: Boolean,
    slidePx: Int,
    layoutDirection: LayoutDirection,
): EnterTransition =
    when (this) {
        // The morph itself comes from sharedBounds; the page only fades in on top of the list.
        TransitionPattern.CONTAINER_TRANSFORM ->
            if (forward) fadeIn(tween(MotionSpec.CONTAINER_MS)) else EnterTransition.None
        TransitionPattern.SHARED_AXIS_X -> {
            val sign = axisSign(forward, layoutDirection)
            slideInHorizontally(tween(MotionSpec.SHARED_AXIS_MS, easing = FastOutSlowInEasing)) { sign * slidePx } +
                fadeIn(fadeInAfter(MotionSpec.SHARED_AXIS_MS))
        }
        TransitionPattern.SHARED_AXIS_Y -> {
            val sign = if (forward) 1 else -1
            slideInVertically(tween(MotionSpec.SHARED_AXIS_MS, easing = FastOutSlowInEasing)) { sign * slidePx } +
                fadeIn(fadeInAfter(MotionSpec.SHARED_AXIS_MS))
        }
        TransitionPattern.SHARED_AXIS_Z ->
            scaleIn(
                tween(MotionSpec.SHARED_AXIS_MS, easing = FastOutSlowInEasing),
                initialScale = if (forward) MotionSpec.SHARED_AXIS_Z_SCALE_IN else MotionSpec.SHARED_AXIS_Z_SCALE_OUT,
            ) + fadeIn(fadeInAfter(MotionSpec.SHARED_AXIS_MS))
        TransitionPattern.FADE_THROUGH ->
            fadeIn(fadeInAfter(MotionSpec.FADE_THROUGH_MS)) +
                scaleIn(fadeInAfter(MotionSpec.FADE_THROUGH_MS), initialScale = MotionSpec.FADE_THROUGH_SCALE_IN)
        TransitionPattern.FADE -> fadeIn(tween(MotionSpec.FADE_IN_MS, easing = LinearOutSlowInEasing))
    }

/** Transition for the page that leaves; the mirror of [enter]. */
fun TransitionPattern.exit(
    forward: Boolean,
    slidePx: Int,
    layoutDirection: LayoutDirection,
): ExitTransition =
    when (this) {
        // Going in, the list stays put (None keeps it until the transition ends) under the growing card;
        // going back, the page fades as the card shrinks.
        TransitionPattern.CONTAINER_TRANSFORM ->
            if (forward) ExitTransition.None else fadeOut(tween(MotionSpec.CONTAINER_MS))
        TransitionPattern.SHARED_AXIS_X -> {
            val sign = axisSign(forward, layoutDirection)
            slideOutHorizontally(tween(MotionSpec.SHARED_AXIS_MS, easing = FastOutSlowInEasing)) { -sign * slidePx } +
                fadeOut(fadeOutFirst)
        }
        TransitionPattern.SHARED_AXIS_Y -> {
            val sign = if (forward) 1 else -1
            slideOutVertically(tween(MotionSpec.SHARED_AXIS_MS, easing = FastOutSlowInEasing)) { -sign * slidePx } +
                fadeOut(fadeOutFirst)
        }
        TransitionPattern.SHARED_AXIS_Z ->
            scaleOut(
                tween(MotionSpec.SHARED_AXIS_MS, easing = FastOutSlowInEasing),
                targetScale = if (forward) MotionSpec.SHARED_AXIS_Z_SCALE_OUT else MotionSpec.SHARED_AXIS_Z_SCALE_IN,
            ) + fadeOut(fadeOutFirst)
        TransitionPattern.FADE_THROUGH -> fadeOut(fadeOutFirst)
        TransitionPattern.FADE -> fadeOut(tween(MotionSpec.FADE_OUT_MS, easing = FastOutLinearInEasing))
    }
