package com.primestrider.kotlinstarter.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/*
 * Colours of form controls (text fields, checkbox, radio, switch, segmented, slider). They are derived
 * from the semantic roles, never from the palette, so swapping the brand colour restyles every control.
 */

/** Resting fill of a field or an unselected control. Differs from both `background` and `card`. */
val AppColors.controlFill: Color get() = secondary

/**
 * Fill of the field being typed in: `primary` over the resting fill, so a focused field still stands out from
 * both `background` and `card` (a tint over `background` vanishes on a dark card).
 */
val AppColors.controlFocusFill: Color get() = primary.copy(alpha = 0.12f).compositeOver(controlFill)

/** Fill of a field in error: `destructive` over the resting fill, for the same reason as [controlFocusFill]. */
val AppColors.controlErrorFill: Color get() = destructive.copy(alpha = 0.10f).compositeOver(controlFill)

/** A selected segment: white in light theme, a lighter grey than the track in dark theme. */
val AppColors.controlRaised: Color
    get() = if (background.luminance() >= 0.5f) card else foreground.copy(alpha = 0.14f).compositeOver(controlFill)

/** Duration of control colour and position changes (focus tint, switch thumb, segment pill). */
const val CONTROL_ANIMATION_MS = 180

object ControlShapes {
    val field: Shape = RoundedCornerShape(Radii.xl)
    val checkbox: Shape = RoundedCornerShape(7.dp)
    val pill: Shape = RoundedCornerShape(Radii.full)
}
