package com.primestrider.kotlinstarter.core.designsystem.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.primestrider.kotlinstarter.core.designsystem.theme.CONTROL_ANIMATION_MS
import com.primestrider.kotlinstarter.core.designsystem.theme.LocalAppColors
import com.primestrider.kotlinstarter.core.designsystem.theme.Palette
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.core.designsystem.theme.controlFill
import com.primestrider.kotlinstarter.core.designsystem.theme.controlRaised

internal const val SEGMENTED_PILL_TAG = "segmented_pill"

private val TRACK_SHAPE = RoundedCornerShape(14.dp)
private val SEGMENT_SHAPE = RoundedCornerShape(10.dp)
private val TRACK_PADDING = 4.dp
private val SEGMENT_MIN_HEIGHT = 48.dp
private const val DISABLED_ALPHA = 0.38f

/** Neutral filled track that holds the segments of [SegmentedChoice] and [MultiSegmentedChoice]. */
private fun Modifier.segmentTrack(
    fill: androidx.compose.ui.graphics.Color,
    enabled: Boolean,
): Modifier =
    fillMaxWidth()
        .alpha(if (enabled) 1f else DISABLED_ALPHA)
        .background(fill, TRACK_SHAPE)
        .padding(horizontal = TRACK_PADDING)

@Composable
private fun SegmentLabel(
    text: String,
    selected: Boolean,
) {
    val c = LocalAppColors.current
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        color = if (selected) c.foreground else c.muted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * One segment: the whole 48dp-tall cell is the touch target; the visible segment (and its raised fill when
 * [raised]) is inset by the track padding, so the track reads 40dp tall inside a 48dp target.
 */
@Composable
private fun RowScope.Segment(
    clickable: Modifier,
    raised: Boolean,
    content: @Composable () -> Unit,
) {
    val c = LocalAppColors.current
    Box(
        Modifier
            .weight(1f)
            .heightIn(min = SEGMENT_MIN_HEIGHT)
            .then(clickable)
            .padding(vertical = TRACK_PADDING)
            .then(if (raised) Modifier.shadow(1.dp, SEGMENT_SHAPE).background(c.controlRaised, SEGMENT_SHAPE) else Modifier)
            .clip(SEGMENT_SHAPE)
            .padding(horizontal = Spacing.s2),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/**
 * One-of-N segmented control for up to four short options (use [SelectField] for more): a neutral track
 * with a raised pill that slides under the selected option. Mirrors in RTL.
 */
@Composable
fun <T> SegmentedChoice(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = LocalAppColors.current
    BoxWithConstraints(modifier.segmentTrack(c.controlFill, enabled)) {
        val segmentWidth = maxWidth / options.size.coerceAtLeast(1)
        val index = options.indexOf(selected)
        val pillOffset by animateDpAsState(segmentWidth * index.coerceAtLeast(0), tween(CONTROL_ANIMATION_MS), label = "pill")
        if (index >= 0) {
            Box(Modifier.matchParentSize()) {
                // offset (not absoluteOffset) mirrors in RTL, so the pill follows the visual order of the segments.
                Box(
                    Modifier
                        .offset(x = pillOffset)
                        .width(segmentWidth)
                        .fillMaxHeight()
                        .padding(vertical = TRACK_PADDING)
                        .shadow(1.dp, SEGMENT_SHAPE)
                        .background(c.controlRaised, SEGMENT_SHAPE)
                        .testTag(SEGMENTED_PILL_TAG),
                )
            }
        }
        Row(Modifier.fillMaxWidth().selectableGroup()) {
            options.forEach { option ->
                val isSelected = option == selected
                Segment(
                    Modifier.selectable(selected = isSelected, enabled = enabled, role = Role.RadioButton, onClick = { onSelect(option) }),
                    raised = false,
                ) { SegmentLabel(label(option), isSelected) }
            }
        }
    }
}

/** Many-of-N segmented control: same track as [SegmentedChoice], every selected option raised. */
@Composable
fun <T> MultiSegmentedChoice(
    options: List<T>,
    selected: Set<T>,
    onToggle: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = LocalAppColors.current
    Row(modifier.segmentTrack(c.controlFill, enabled)) {
        options.forEach { option ->
            val isSelected = option in selected
            Segment(
                Modifier.toggleable(value = isSelected, enabled = enabled, role = Role.Checkbox, onValueChange = { onToggle(option) }),
                raised = isSelected,
            ) { SegmentLabel(label(option), isSelected) }
        }
    }
}

/** Label with a trailing switch; the whole row is the touch target and reports [Role.Switch]. */
@Composable
fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        AppSwitch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

private val SLIDER_TRACK_HEIGHT = 6.dp
private val SLIDER_THUMB_SIZE = 22.dp

@Composable
internal fun appSliderColors(): SliderColors {
    val c = LocalAppColors.current
    return SliderDefaults.colors(
        thumbColor = Palette.white,
        activeTrackColor = c.primary,
        inactiveTrackColor = c.controlFill,
        // Step marks must contrast with the track they sit on.
        activeTickColor = Palette.white,
        inactiveTickColor = c.muted,
    )
}

/** White 22dp thumb with a soft shadow; Material keeps the 48dp touch area around it. */
@Composable
private fun SliderThumb() {
    Box(
        Modifier
            .size(SLIDER_THUMB_SIZE)
            .shadow(2.dp, CircleShape)
            .background(Palette.white, CircleShape),
    )
}

@Composable
private fun SliderHeader(
    label: String,
    valueText: String,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(valueText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Thin slider (6dp track, white thumb) with its [label] and a [valueText] readout above it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabeledSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    valueText: String,
    modifier: Modifier = Modifier,
) {
    val colors = appSliderColors()
    Column(modifier.fillMaxWidth()) {
        SliderHeader(label, valueText)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = colors,
            thumb = { SliderThumb() },
            track = { state ->
                SliderDefaults.Track(
                    sliderState = state,
                    colors = colors,
                    drawStopIndicator = null,
                    thumbTrackGapSize = 0.dp,
                    modifier = Modifier.height(SLIDER_TRACK_HEIGHT),
                )
            },
        )
    }
}

/** Range version of [LabeledSlider]: same track, two thumbs. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabeledRangeSlider(
    label: String,
    value: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    valueText: String,
    modifier: Modifier = Modifier,
    steps: Int = 0,
) {
    val colors = appSliderColors()
    Column(modifier.fillMaxWidth()) {
        SliderHeader(label, valueText)
        RangeSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = colors,
            startThumb = { SliderThumb() },
            endThumb = { SliderThumb() },
            track = { state ->
                SliderDefaults.Track(
                    rangeSliderState = state,
                    colors = colors,
                    drawStopIndicator = null,
                    thumbTrackGapSize = 0.dp,
                    modifier = Modifier.height(SLIDER_TRACK_HEIGHT),
                )
            },
        )
    }
}

@ThemePreviews
@Composable
private fun ChoiceControlsPreview() {
    PreviewTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SegmentedChoice(options = listOf("Day", "Week", "Month"), selected = "Week", onSelect = {}, label = { it })
            MultiSegmentedChoice(options = listOf("Mon", "Tue", "Wed"), selected = setOf("Mon", "Wed"), onToggle = {}, label = { it })
            SwitchRow(label = "Notifications", checked = true, onCheckedChange = {})
            LabeledSlider(label = "Volume", value = 0.4f, onValueChange = {}, valueRange = 0f..1f, valueText = "40%")
            LabeledRangeSlider(label = "Price", value = 20f..60f, onValueChange = {}, valueRange = 0f..100f, valueText = "20 - 60")
        }
    }
}
