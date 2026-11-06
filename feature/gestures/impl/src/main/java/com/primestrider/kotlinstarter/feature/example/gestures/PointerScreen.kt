package com.primestrider.kotlinstarter.feature.example.gestures

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.primestrider.kotlinstarter.core.designsystem.component.SwitchRow
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.feature.example.DemoSection
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold
import com.primestrider.kotlinstarter.feature.gestures.impl.R
import kotlin.math.roundToInt

private const val FILE = "PointerScreen.kt"

/** Points kept when the drawing is saved: 8 bytes each, so about 160 KB at most in the saved-state Bundle. */
private const val MAX_SAVED_POINTS = 20_000

/** Strokes saved as flat `x, y, x, y…` arrays, which a Bundle can hold, thinned by [compactStrokes]. */
private val StrokesSaver =
    listSaver<SnapshotStateList<List<Offset>>, FloatArray>(
        save = { strokes ->
            compactStrokes(strokes, MAX_SAVED_POINTS).map { s ->
                FloatArray(s.size * 2) { i ->
                    if (i % 2 ==
                        0
                    ) {
                        s[i / 2].x
                    } else {
                        s[i / 2].y
                    }
                }
            }
        },
        restore = { saved -> saved.map { a -> List(a.size / 2) { i -> Offset(a[2 * i], a[2 * i + 1]) } }.toMutableStateList() },
    )

/** Low-level pointer input: raw multi-touch events, a drawing canvas and how consumption settles conflicts. */
@Composable
fun PointerScreen(onBack: () -> Unit) {
    ExamplePageScaffold(R.string.gesture_pointer_title, R.string.gesture_pointer_subtitle, onBack) {
        MultiTouchExample()
        DrawingExample()
        ConflictExample()
    }
}

@Composable
private fun MultiTouchExample() {
    // Pressed pointers by id, as the last event reported them.
    var pointers by remember { mutableStateOf(emptyMap<Long, Offset>()) }
    DemoSection(
        R.string.gesture_pointer_touch_title,
        R.string.gesture_pointer_touch_usage,
        "awaitEachGesture { awaitPointerEvent() }",
        FILE,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        do {
                            val event = awaitPointerEvent()
                            pointers = event.changes.filter { it.pressed }.associate { it.id.value to it.position }
                            // Consumed so the page does not scroll while fingers are on the area.
                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })
                        pointers = emptyMap()
                    }
                }.testTag("pointerArea"),
        ) {
            if (pointers.isEmpty()) {
                Text(
                    stringResource(R.string.gesture_pointer_touch_hint),
                    Modifier.align(Alignment.Center).padding(Spacing.s2),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val ring = MaterialTheme.colorScheme.primary
            Canvas(Modifier.fillMaxSize()) {
                pointers.values.forEach { drawCircle(ring, radius = 32.dp.toPx(), center = it, style = Stroke(4.dp.toPx())) }
            }
            pointers.forEach { (id, at) ->
                Text(
                    "#$id",
                    Modifier.offset { IntOffset(at.x.roundToInt() - 12.dp.roundToPx(), at.y.roundToInt() - 52.dp.roundToPx()) },
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        Text(stringResource(R.string.gesture_pointer_touch_count, pointers.size), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DrawingExample() {
    val strokes = rememberSaveable(saver = StrokesSaver) { mutableStateListOf() }
    // The stroke being drawn; it joins [strokes] when the finger lifts.
    val current = remember { mutableStateListOf<Offset>() }
    val ink = MaterialTheme.colorScheme.primary
    DemoSection(R.string.gesture_pointer_draw_title, R.string.gesture_pointer_draw_usage, "detectDragGestures + Canvas", FILE) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            current.clear()
                            current.add(it)
                        },
                        onDragEnd = {
                            if (current.isNotEmpty()) strokes.add(current.toList())
                            current.clear()
                        },
                        onDragCancel = { current.clear() },
                    ) { change, _ ->
                        change.consume()
                        current.add(change.position)
                    }
                }.testTag("canvas"),
        ) {
            val style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            (strokes + listOf(current.toList())).filter { it.isNotEmpty() }.forEach { points ->
                val path =
                    Path().apply {
                        moveTo(points.first().x, points.first().y)
                        points.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                drawPath(path, ink, style = style)
            }
        }
        Text(stringResource(R.string.gesture_pointer_draw_count, strokes.size), style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = { strokes.clear() }) { Text(stringResource(R.string.gesture_pointer_draw_clear)) }
    }
}

@Composable
private fun ConflictExample() {
    var childConsumes by rememberSaveable { mutableStateOf(true) }
    var childTaps by rememberSaveable { mutableIntStateOf(0) }
    var parentTaps by rememberSaveable { mutableIntStateOf(0) }
    // Read inside the long-lived pointer loop, so it must follow the switch without restarting the gesture.
    val consume by rememberUpdatedState(childConsumes)
    DemoSection(
        R.string.gesture_pointer_conflict_title,
        R.string.gesture_pointer_conflict_usage,
        "PointerInputChange.consume() + detectTapGestures",
        FILE,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                // detectTapGestures ignores a tap whose up event a child already consumed.
                .pointerInput(Unit) { detectTapGestures(onTap = { parentTaps++ }) }
                .testTag("parentTarget"),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(R.string.gesture_pointer_conflict_parent),
                Modifier.align(Alignment.TopStart).padding(Spacing.s3),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Box(
                Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.tertiary)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            val up = waitForUpOrCancellation() ?: return@awaitEachGesture
                            childTaps++
                            if (consume) up.consume()
                        }
                    }.testTag("childTarget"),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.gesture_pointer_conflict_child), color = MaterialTheme.colorScheme.onTertiary)
            }
        }
        SwitchRow(
            label = stringResource(R.string.gesture_pointer_conflict_switch),
            checked = childConsumes,
            onCheckedChange = { childConsumes = it },
        )
        Text(stringResource(R.string.gesture_pointer_conflict_counts, childTaps, parentTaps), style = MaterialTheme.typography.bodyMedium)
    }
}

@ThemePreviews
@Composable
private fun PointerScreenPreview() {
    PreviewTheme {
        PointerScreen(onBack = {})
    }
}
