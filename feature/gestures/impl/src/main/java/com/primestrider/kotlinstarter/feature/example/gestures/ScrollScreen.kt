package com.primestrider.kotlinstarter.feature.example.gestures

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.feature.example.DemoSection
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold
import com.primestrider.kotlinstarter.feature.gestures.impl.R
import kotlin.math.roundToInt

private const val FILE = "ScrollScreen.kt"

/** Dial value change per pixel scrolled. */
private const val DIAL_PER_PX = 0.25f

/** Step of the dial's − / + buttons. */
private const val DIAL_STEP = 5f

/** Scroll gestures: a custom `scrollable`, a `nestedScroll` collapsing header and overscroll. */
@Composable
fun ScrollScreen(onBack: () -> Unit) {
    ExamplePageScaffold(R.string.gesture_scroll_title, R.string.gesture_scroll_subtitle, onBack) {
        DialExample()
        NestedScrollExample()
        OverscrollExample()
    }
}

@Composable
private fun DialExample() {
    var value by rememberSaveable { mutableFloatStateOf(0f) }
    val state =
        rememberScrollableState { delta ->
            // Finger up (negative delta) turns the dial up. Returning only what was used lets the page scroll on
            // once the dial is at 0 or 100.
            val next = (value - delta * DIAL_PER_PX).coerceIn(0f, 100f)
            val used = (value - next) / DIAL_PER_PX
            value = next
            used
        }
    val increase = stringResource(R.string.gesture_scroll_dial_increase)
    val decrease = stringResource(R.string.gesture_scroll_dial_decrease)
    DemoSection(
        R.string.gesture_scroll_dial_title,
        R.string.gesture_scroll_dial_usage,
        "Modifier.scrollable(rememberScrollableState())",
        FILE,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s4), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .scrollable(state, Orientation.Vertical)
                    .semantics {
                        progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..100f)
                        setProgress { target ->
                            value = target.coerceIn(0f, 100f)
                            true
                        }
                    }.testTag("dial"),
                contentAlignment = Alignment.Center,
            ) {
                // The marker turns 270° over the range, like a volume knob.
                Box(
                    Modifier
                        .size(120.dp)
                        .graphicsLayer { rotationZ = value * 2.7f - 135f },
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                }
                Text(value.roundToInt().toString(), style = MaterialTheme.typography.headlineMedium)
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                FilledTonalIconButton(
                    onClick = { value = (value + DIAL_STEP).coerceAtMost(100f) },
                    modifier = Modifier.semantics { contentDescription = increase },
                ) { Text("+") }
                FilledTonalIconButton(
                    onClick = { value = (value - DIAL_STEP).coerceAtLeast(0f) },
                    modifier = Modifier.semantics { contentDescription = decrease },
                ) { Text("−") }
            }
        }
        Text(stringResource(R.string.gesture_scroll_dial_value, value.roundToInt()), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun NestedScrollExample() {
    val density = LocalDensity.current
    val minPx = with(density) { 56.dp.toPx() }
    val maxPx = with(density) { 160.dp.toPx() }
    var headerPx by rememberSaveable { mutableFloatStateOf(maxPx) }
    val connection =
        remember(minPx, maxPx) {
            object : NestedScrollConnection {
                // Scrolling up: the header shrinks first, the list only gets what is left.
                override fun onPreScroll(
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (available.y >= 0) return Offset.Zero
                    val (next, used) = headerHeightAfter(available.y, headerPx, minPx, maxPx)
                    headerPx = next
                    return Offset(0f, used)
                }

                // Scrolling down: the list scrolls back to its top first, then the header grows with what is left.
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (available.y <= 0) return Offset.Zero
                    val (next, used) = headerHeightAfter(available.y, headerPx, minPx, maxPx)
                    headerPx = next
                    return Offset(0f, used)
                }
            }
        }
    DemoSection(
        R.string.gesture_scroll_nested_title,
        R.string.gesture_scroll_nested_usage,
        "Modifier.nestedScroll(NestedScrollConnection)",
        FILE,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .height(360.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .nestedScroll(connection),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    // Read in the layout and draw phases only, so a scroll frame re-measures the header without recomposing.
                    .layout { measurable, constraints ->
                        val height = headerPx.roundToInt()
                        val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
                        layout(placeable.width, height) { placeable.place(0, 0) }
                    }.background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)))
                    .testTag("nestedHeader")
                    .padding(Spacing.s4),
                contentAlignment = Alignment.BottomStart,
            ) {
                Text(
                    stringResource(R.string.gesture_scroll_nested_header),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.graphicsLayer { alpha = 1f - ((maxPx - headerPx) / (maxPx - minPx)).coerceIn(0f, 1f) * 0.4f },
                )
            }
            LazyColumn(Modifier.fillMaxWidth().testTag("nestedList")) {
                items(30) { index ->
                    Text(stringResource(R.string.gesture_scroll_row, index + 1), Modifier.fillMaxWidth().padding(Spacing.s4))
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun OverscrollExample() {
    DemoSection(
        R.string.gesture_scroll_overscroll_title,
        R.string.gesture_scroll_overscroll_usage,
        "LazyColumn(overscrollEffect = …)",
        FILE,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            listOf(true, false).forEach { withEffect ->
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
                    Text(
                        stringResource(if (withEffect) R.string.gesture_scroll_overscroll_on else R.string.gesture_scroll_overscroll_off),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    LazyColumn(
                        Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .testTag(if (withEffect) "overscrollList" else "noOverscrollList"),
                        overscrollEffect = if (withEffect) rememberOverscrollEffect() else null,
                    ) {
                        items(20) { index ->
                            Text(stringResource(R.string.gesture_scroll_row, index + 1), Modifier.padding(Spacing.s3))
                        }
                    }
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun ScrollScreenPreview() {
    PreviewTheme {
        ScrollScreen(onBack = {})
    }
}
