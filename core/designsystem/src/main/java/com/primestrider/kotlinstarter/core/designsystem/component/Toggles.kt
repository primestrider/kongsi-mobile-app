package com.primestrider.kotlinstarter.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.primestrider.kotlinstarter.core.designsystem.theme.CONTROL_ANIMATION_MS
import com.primestrider.kotlinstarter.core.designsystem.theme.ControlShapes
import com.primestrider.kotlinstarter.core.designsystem.theme.LocalAppColors
import com.primestrider.kotlinstarter.core.designsystem.theme.Palette
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.core.designsystem.theme.controlFill

/*
 * Borderless checkbox, radio and switch. Like their Material counterparts, a control given a callback is
 * its own 48dp touch target with checkbox/radio/switch semantics; given a null callback it only draws, so
 * the row around it (toggleable/selectable) owns the click and TalkBack announces the row once.
 */

private val MARK_SIZE = 22.dp
private val MIN_TOUCH = 48.dp
private const val DISABLED_ALPHA = 0.38f

/** Primary when on, neutral fill when off, animated between the two. */
@Composable
private fun controlColor(on: Boolean): Color {
    val c = LocalAppColors.current
    val color by animateColorAsState(if (on) c.primary else c.controlFill, tween(CONTROL_ANIMATION_MS), label = "control")
    return color
}

/** 22dp rounded square: neutral fill when off, primary with a white check when on, a white dash when indeterminate. */
@Composable
fun AppCheckbox(
    state: ToggleableState,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interaction =
        if (onClick == null) {
            Modifier
        } else {
            Modifier
                .sizeIn(minWidth = MIN_TOUCH, minHeight = MIN_TOUCH)
                .triStateToggleable(state = state, enabled = enabled, role = Role.Checkbox, onClick = onClick)
        }
    Box(modifier.then(interaction), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(MARK_SIZE)
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .background(controlColor(state != ToggleableState.Off), ControlShapes.checkbox),
            contentAlignment = Alignment.Center,
        ) {
            when (state) {
                ToggleableState.On ->
                    Icon(Icons.Filled.Check, contentDescription = null, tint = Palette.white, modifier = Modifier.size(16.dp))
                ToggleableState.Indeterminate ->
                    Box(Modifier.size(width = 10.dp, height = 2.dp).background(Palette.white, RoundedCornerShape(1.dp)))
                ToggleableState.Off -> Unit
            }
        }
    }
}

/** Two-state [AppCheckbox]. */
@Composable
fun AppCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    AppCheckbox(
        state = ToggleableState(checked),
        onClick = onCheckedChange?.let { change -> { change(!checked) } },
        modifier = modifier,
        enabled = enabled,
    )
}

/** 22dp circle: neutral fill when unselected, primary with a white centre dot when selected. */
@Composable
fun AppRadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interaction =
        if (onClick == null) {
            Modifier
        } else {
            Modifier
                .sizeIn(minWidth = MIN_TOUCH, minHeight = MIN_TOUCH)
                .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
        }
    Box(modifier.then(interaction), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(MARK_SIZE)
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .background(controlColor(selected), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(8.dp).background(Palette.white, CircleShape))
        }
    }
}

private val TRACK_WIDTH = 46.dp
private val TRACK_HEIGHT = 28.dp
private val THUMB_SIZE = 24.dp
private val THUMB_INSET = 2.dp

/** iOS-like pill switch: neutral track when off, primary when on; the white thumb slides and never resizes. */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interaction =
        if (onCheckedChange == null) {
            Modifier
        } else {
            Modifier
                .sizeIn(minWidth = MIN_TOUCH, minHeight = MIN_TOUCH)
                .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
        }
    val thumbOffset by animateDpAsState(
        if (checked) TRACK_WIDTH - THUMB_SIZE - THUMB_INSET * 2 else 0.dp,
        tween(CONTROL_ANIMATION_MS),
        label = "thumb",
    )
    Box(modifier.then(interaction), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(TRACK_WIDTH, TRACK_HEIGHT)
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .background(controlColor(checked), ControlShapes.pill)
                .padding(THUMB_INSET),
        ) {
            // offset (not absoluteOffset) mirrors in RTL, so "on" is always the end side.
            Box(
                Modifier
                    .offset(x = thumbOffset)
                    .size(THUMB_SIZE)
                    .shadow(2.dp, CircleShape)
                    .background(Palette.white, CircleShape),
            )
        }
    }
}

@ThemePreviews
@Composable
private fun TogglesPreview() {
    PreviewTheme {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AppCheckbox(checked = true, onCheckedChange = {})
            AppCheckbox(checked = false, onCheckedChange = {})
            AppRadioButton(selected = true, onClick = {})
            AppRadioButton(selected = false, onClick = {})
            AppSwitch(checked = true, onCheckedChange = {})
            AppSwitch(checked = false, onCheckedChange = {}, enabled = false)
        }
    }
}
