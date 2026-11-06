package com.primestrider.kotlinstarter.feature.example.gestures

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.feature.example.DemoSection
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold
import com.primestrider.kotlinstarter.feature.gestures.impl.R
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val FILE = "SwipeScreen.kt"

/** Cards in the swipe stack. */
private const val STACK_SIZE = 5

/** Degrees a card tilts when dragged across its full width. */
private const val CARD_TILT = 15f

private val cardColors = listOf(Color(0xFF5B8DEF), Color(0xFFE5866B), Color(0xFF55B48C), Color(0xFFB07CD8), Color(0xFFE0B54A))

private enum class RevealAnchor { Closed, Open }

/** The three menus of the swipeable menu example: tab label and page text. */
private val menus =
    listOf(
        R.string.gesture_swipe_menu_food to R.string.gesture_swipe_menu_food_content,
        R.string.gesture_swipe_menu_drinks to R.string.gesture_swipe_menu_drinks_content,
        R.string.gesture_swipe_menu_desserts to R.string.gesture_swipe_menu_desserts_content,
    )

/** Swipes and flings: swipeable menus, reveal actions, a card stack and a decaying fling. */
@Composable
fun SwipeScreen(onBack: () -> Unit) {
    ExamplePageScaffold(R.string.gesture_swipe_title, R.string.gesture_swipe_subtitle, onBack) {
        SwipeMenuExample()
        RevealRowExample()
        CardStackExample()
        FlingExample()
    }
}

/**
 * Menus side by side: swipe left for the next one, right for the previous one. The pager owns the current page; the
 * tab row only reads it and asks the pager to scroll, so a swipe and a tap can never disagree. The tabs are also the
 * non-gesture alternative for TalkBack. `rememberPagerState` is saveable, so the page survives rotation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeMenuExample() {
    val pagerState = rememberPagerState { menus.size }
    val scope = rememberCoroutineScope()
    DemoSection(
        R.string.gesture_swipe_menu_title,
        R.string.gesture_swipe_menu_usage,
        "PrimaryTabRow + HorizontalPager(rememberPagerState())",
        FILE,
    ) {
        PrimaryTabRow(selectedTabIndex = pagerState.currentPage, modifier = Modifier.testTag("menuTabs")) {
            menus.forEachIndexed { index, (label, _) ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(stringResource(label)) },
                )
            }
        }
        HorizontalPager(pagerState, Modifier.fillMaxWidth().height(160.dp).testTag("menuPager")) { page ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing.s1)
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardColors[page % cardColors.size]),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(menus[page].second), style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
        }
    }
}

@Composable
private fun RevealRowExample() {
    val state = rememberSaveable(saver = AnchoredDraggableState.Saver()) { AnchoredDraggableState(RevealAnchor.Closed) }
    val scope = rememberCoroutineScope()
    val actionsWidth = 176.dp
    val actionsPx = with(LocalDensity.current) { actionsWidth.toPx() }
    SideEffect {
        state.updateAnchors(
            DraggableAnchors {
                RevealAnchor.Closed at 0f
                RevealAnchor.Open at -actionsPx
            },
        )
    }
    val open = state.settledValue == RevealAnchor.Open
    val showLabel = stringResource(R.string.gesture_swipe_reveal_show)
    val hideLabel = stringResource(R.string.gesture_swipe_reveal_hide)
    DemoSection(
        R.string.gesture_swipe_reveal_title,
        R.string.gesture_swipe_reveal_usage,
        "AnchoredDraggableState + Modifier.anchoredDraggable",
        FILE,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.errorContainer),
        ) {
            Row(Modifier.align(Alignment.CenterEnd).width(actionsWidth).fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                listOf(R.string.gesture_swipe_reveal_archive, R.string.gesture_swipe_reveal_delete).forEach { label ->
                    TextButton(onClick = { scope.launch { state.animateTo(RevealAnchor.Closed) } }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(label), color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
            Box(
                Modifier
                    .offset { IntOffset(state.offset.takeUnless { it.isNaN() }?.roundToInt() ?: 0, 0) }
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .anchoredDraggable(state, Orientation.Horizontal)
                    .semantics {
                        customActions =
                            listOf(
                                CustomAccessibilityAction(if (open) hideLabel else showLabel) {
                                    scope.launch { state.animateTo(if (open) RevealAnchor.Closed else RevealAnchor.Open) }
                                    true
                                },
                            )
                    }.testTag("revealRow")
                    .padding(horizontal = Spacing.s4),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(stringResource(R.string.gesture_swipe_reveal_hint))
            }
        }
        Text(
            stringResource(if (open) R.string.gesture_swipe_reveal_shown else R.string.gesture_swipe_reveal_hidden),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun CardStackExample() {
    // Index of the card on top; STACK_SIZE means the stack is empty.
    var top by rememberSaveable { mutableIntStateOf(0) }
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var widthPx by remember { mutableFloatStateOf(1f) }

    fun leave(outcome: SwipeOutcome) {
        scope.launch {
            when (outcome) {
                SwipeOutcome.Return -> offsetX.animateTo(0f)
                else -> {
                    offsetX.animateTo(if (outcome == SwipeOutcome.Right) widthPx * 1.5f else -widthPx * 1.5f)
                    top++
                    offsetX.snapTo(0f)
                }
            }
        }
    }
    DemoSection(
        R.string.gesture_swipe_stack_title,
        R.string.gesture_swipe_stack_usage,
        "detectDragGestures + VelocityTracker + Animatable",
        FILE,
    ) {
        Box(
            Modifier.fillMaxWidth().height(240.dp).onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) },
            contentAlignment = Alignment.Center,
        ) {
            if (top >= STACK_SIZE) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                    Text(stringResource(R.string.gesture_swipe_stack_empty))
                    TextButton(onClick = { top = 0 }) { Text(stringResource(R.string.gesture_swipe_stack_restart)) }
                }
            } else {
                if (top + 1 < STACK_SIZE) {
                    // The next card waits underneath, slightly smaller, without a label so only the top card is read.
                    StackCard(
                        top + 1,
                        label = false,
                        Modifier.graphicsLayer {
                            scaleX = 0.94f
                            scaleY = 0.94f
                        },
                    )
                }
                StackCard(
                    top,
                    label = true,
                    Modifier
                        .graphicsLayer {
                            translationX = offsetX.value
                            rotationZ = offsetX.value / widthPx * CARD_TILT
                        }.pointerInput(top) {
                            val tracker = VelocityTracker()
                            // This input sits inside the card's moving, rotating layer, so change.position barely changes
                            // while the card follows the finger. The summed drag deltas track the finger itself.
                            var travelled = Offset.Zero
                            detectDragGestures(
                                onDragStart = {
                                    tracker.resetTracking()
                                    travelled = Offset.Zero
                                },
                                onDragEnd = { leave(swipeOutcome(offsetX.value, tracker.calculateVelocity().x, widthPx)) },
                                onDragCancel = { leave(SwipeOutcome.Return) },
                            ) { change, drag ->
                                change.consume()
                                travelled += drag
                                tracker.addPosition(change.uptimeMillis, travelled)
                                scope.launch { offsetX.snapTo(offsetX.value + drag.x) }
                            }
                        }.testTag("cardStack"),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s4, Alignment.CenterHorizontally)) {
            val enabled = top < STACK_SIZE
            FilledTonalIconButton(onClick = { leave(SwipeOutcome.Left) }, enabled = enabled) {
                Icon(Icons.Filled.Close, stringResource(R.string.gesture_swipe_stack_skip))
            }
            FilledTonalIconButton(onClick = { leave(SwipeOutcome.Right) }, enabled = enabled) {
                Icon(Icons.Filled.Check, stringResource(R.string.gesture_swipe_stack_keep))
            }
        }
    }
}

@Composable
private fun StackCard(
    index: Int,
    label: Boolean,
    modifier: Modifier,
) {
    Box(
        modifier
            .fillMaxWidth(0.8f)
            .height(200.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(cardColors[index % cardColors.size]),
        contentAlignment = Alignment.Center,
    ) {
        if (label) {
            Text(
                stringResource(R.string.gesture_swipe_stack_card, index + 1),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun FlingExample() {
    val x = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val puck = 56.dp
    DemoSection(
        R.string.gesture_swipe_fling_title,
        R.string.gesture_swipe_fling_usage,
        "VelocityTracker + Animatable.animateDecay(exponentialDecay())",
        FILE,
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            val maxPx = with(density) { (maxWidth - puck).toPx() }.coerceAtLeast(0f)
            SideEffect { x.updateBounds(0f, maxPx) }
            Box(
                Modifier
                    .matchParentSize()
                    .pointerInput(maxPx) {
                        val tracker = VelocityTracker()
                        detectDragGestures(
                            onDragStart = {
                                tracker.resetTracking()
                                scope.launch { x.stop() }
                            },
                            onDragEnd = {
                                val velocity = tracker.calculateVelocity().x
                                scope.launch { x.animateDecay(velocity, exponentialDecay()) }
                            },
                        ) { change, drag ->
                            change.consume()
                            tracker.addPosition(change.uptimeMillis, change.position)
                            scope.launch { x.snapTo(x.value + drag.x) }
                        }
                    }.testTag("flingArea"),
            )
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset { IntOffset(x.value.roundToInt(), 0) }
                    .size(puck)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
        Text(
            stringResource(
                R.string.gesture_swipe_fling_position,
                with(density) {
                    x.value
                        .toDp()
                        .value
                        .roundToInt()
                },
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@ThemePreviews
@Composable
private fun SwipeScreenPreview() {
    PreviewTheme {
        SwipeScreen(onBack = {})
    }
}
