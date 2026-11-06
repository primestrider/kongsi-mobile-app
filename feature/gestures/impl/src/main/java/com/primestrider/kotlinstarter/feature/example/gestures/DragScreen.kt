package com.primestrider.kotlinstarter.feature.example.gestures

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.feature.example.DemoSection
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold
import com.primestrider.kotlinstarter.feature.example.rememberSaveableStateList
import com.primestrider.kotlinstarter.feature.gestures.impl.R
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val FILE = "DragScreen.kt"

/** Drag gestures: `draggable`, `detectDragGestures`, `anchoredDraggable` and long-press-then-drag reordering. */
@Composable
fun DragScreen(onBack: () -> Unit) {
    ExamplePageScaffold(R.string.gesture_drag_title, R.string.gesture_drag_subtitle, onBack) {
        AxisDragExample()
        FreeDragExample()
        AnchoredSheetExample()
        ReorderExample()
    }
}

@Composable
private fun AxisDragExample() {
    var fraction by rememberSaveable { mutableFloatStateOf(0f) }
    var trackPx by remember { mutableIntStateOf(1) }
    val handle = 40.dp
    val handlePx = with(LocalDensity.current) { handle.toPx() }
    DemoSection(R.string.gesture_drag_axis_title, R.string.gesture_drag_axis_usage, "Modifier.draggable(Orientation.Horizontal)", FILE) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .onSizeChanged { trackPx = it.width }
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
                    setProgress { target ->
                        fraction = target.coerceIn(0f, 1f)
                        true
                    }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Box(
                Modifier
                    .offset { IntOffset((fraction * (trackPx - handlePx)).roundToInt(), 0) }
                    .size(handle)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state =
                            rememberDraggableState { delta ->
                                val travel = (trackPx - handlePx).coerceAtLeast(1f)
                                fraction = (fraction + delta / travel).coerceIn(0f, 1f)
                            },
                    ).testTag("dragHandle"),
            )
        }
        Text(stringResource(R.string.gesture_drag_value, (fraction * 100).roundToInt()), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun FreeDragExample() {
    // Offset of the object from the centre of the area, in px; clamped so it stays inside.
    var x by rememberSaveable { mutableFloatStateOf(0f) }
    var y by rememberSaveable { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val objectSize = 56.dp
    DemoSection(R.string.gesture_drag_free_title, R.string.gesture_drag_free_usage, "pointerInput { detectDragGestures() }", FILE) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            val maxX = with(density) { ((maxWidth - objectSize) / 2).toPx() }
            val maxY = with(density) { ((maxHeight - objectSize) / 2).toPx() }
            Box(
                Modifier
                    .matchParentSize()
                    .pointerInput(maxX, maxY) {
                        detectDragGestures { change, drag ->
                            // Consuming the change keeps the page's vertical scroll from taking the gesture.
                            change.consume()
                            x = (x + drag.x).coerceIn(-maxX, maxX)
                            y = (y + drag.y).coerceIn(-maxY, maxY)
                        }
                    }.testTag("freeDragArea"),
            )
            Box(
                Modifier
                    .align(Alignment.Center)
                    .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                    .size(objectSize)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.tertiary),
            )
        }
        val dx = with(density) { x.toDp().value.roundToInt() }
        val dy = with(density) { y.toDp().value.roundToInt() }
        Text(stringResource(R.string.gesture_drag_position, dx, dy), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun anchorLabel(anchor: SheetAnchor): String =
    stringResource(
        when (anchor) {
            SheetAnchor.Collapsed -> R.string.gesture_drag_anchor_collapsed
            SheetAnchor.Half -> R.string.gesture_drag_anchor_half
            SheetAnchor.Expanded -> R.string.gesture_drag_anchor_expanded
        },
    )

@Composable
private fun AnchoredSheetExample() {
    val state = rememberSaveable(saver = AnchoredDraggableState.Saver()) { AnchoredDraggableState(SheetAnchor.Collapsed) }
    val scope = rememberCoroutineScope()
    val frame = 320.dp
    val peek = 64.dp
    val density = LocalDensity.current
    val framePx = with(density) { frame.toPx() }
    val peekPx = with(density) { peek.toPx() }
    // Anchors are the distance of the sheet's top from the top of the frame.
    SideEffect {
        state.updateAnchors(
            DraggableAnchors {
                SheetAnchor.Collapsed at framePx - peekPx
                SheetAnchor.Half at framePx / 2
                SheetAnchor.Expanded at 0f
            },
        )
    }
    DemoSection(
        R.string.gesture_drag_anchor_title,
        R.string.gesture_drag_anchor_usage,
        "AnchoredDraggableState + Modifier.anchoredDraggable",
        FILE,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(frame)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            // The sheet is as tall as its visible part, so its touch area is exactly what is on screen.
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .layout { measurable, constraints ->
                        val offset = if (state.offset.isNaN()) framePx - peekPx else state.offset
                        val height = (framePx - offset).roundToInt().coerceAtLeast(0)
                        val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
                        layout(placeable.width, height) { placeable.place(0, 0) }
                    }.clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .anchoredDraggable(state, Orientation.Vertical)
                    .testTag("anchoredSheet")
                    .padding(Spacing.s3),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .width(32.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.4f)),
                )
                Text(
                    stringResource(R.string.gesture_drag_sheet_hint),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(top = Spacing.s2),
                )
            }
        }
        Text(
            stringResource(R.string.gesture_drag_anchor_state, anchorLabel(state.settledValue)),
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            listOf(
                SheetAnchor.Collapsed to R.string.gesture_drag_collapse,
                SheetAnchor.Half to R.string.gesture_drag_half,
                SheetAnchor.Expanded to R.string.gesture_drag_expand,
            ).forEach { (anchor, label) ->
                TextButton(onClick = { scope.launch { state.animateTo(anchor) } }) { Text(stringResource(label)) }
            }
        }
    }
}

@Composable
private fun ReorderExample() {
    val order = rememberSaveableStateList(1, 2, 3, 4, 5)
    var dragged by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var rowPx by remember { mutableIntStateOf(1) }

    fun move(
        from: Int,
        to: Int,
    ) {
        val next = order.moved(from, to)
        if (next !== order) {
            order.clear()
            order.addAll(next)
        }
    }
    DemoSection(
        R.string.gesture_drag_reorder_title,
        R.string.gesture_drag_reorder_usage,
        "pointerInput { detectDragGesturesAfterLongPress() }",
        FILE,
    ) {
        Column(Modifier.fillMaxWidth().testTag("reorderList")) {
            order.forEachIndexed { index, id ->
                // Keyed by id so a row keeps its node, and its running drag gesture, when it moves to another index.
                key(id) {
                    val label = stringResource(R.string.gesture_drag_item, id)
                    val isDragged = dragged == index
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .onSizeChanged { rowPx = it.height.coerceAtLeast(1) }
                            .testTag("reorderRow-$id")
                            .graphicsLayer {
                                translationY = if (isDragged) dragOffset else 0f
                                shadowElevation = if (isDragged) 8.dp.toPx() else 0f
                            }.clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isDragged) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                            ).pointerInput(id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        dragged = order.indexOf(id)
                                        dragOffset = 0f
                                    },
                                    onDragEnd = { dragged = -1 },
                                    onDragCancel = { dragged = -1 },
                                ) { change, drag ->
                                    change.consume()
                                    dragOffset += drag.y
                                    val from = dragged
                                    val to = (from + (dragOffset / rowPx).roundToInt()).coerceIn(0, order.lastIndex)
                                    if (to != from) {
                                        move(from, to)
                                        dragOffset -= (to - from) * rowPx
                                        dragged = to
                                    }
                                }
                            }.padding(start = Spacing.s3),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(label, Modifier.weight(1f))
                        IconButton(onClick = { move(index, index - 1) }, enabled = index > 0) {
                            Icon(Icons.Filled.KeyboardArrowUp, stringResource(R.string.gesture_drag_move_up, label))
                        }
                        IconButton(onClick = { move(index, index + 1) }, enabled = index < order.lastIndex) {
                            Icon(Icons.Filled.KeyboardArrowDown, stringResource(R.string.gesture_drag_move_down, label))
                        }
                    }
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun DragScreenPreview() {
    PreviewTheme {
        DragScreen(onBack = {})
    }
}
