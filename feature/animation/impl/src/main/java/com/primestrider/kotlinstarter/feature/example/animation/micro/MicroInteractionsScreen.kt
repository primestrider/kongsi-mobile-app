package com.primestrider.kotlinstarter.feature.example.animation.micro

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.feature.animation.impl.R
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold
import com.primestrider.kotlinstarter.feature.example.animation.AnimExample
import com.primestrider.kotlinstarter.feature.example.animation.MinTouchTarget
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Small feedback animations: like pop, sliding counter, tab indicator and a content swap. */
@Composable
fun MicroInteractionsScreen(onBack: () -> Unit) {
    ExamplePageScaffold(R.string.anim_micro_title, R.string.anim_micro_subtitle, onBack) {
        LikeExample()
        CounterExample()
        TabIndicatorExample()
        ContentChangeExample()
    }
}

@Composable
private fun LikeExample() {
    var liked by rememberSaveable { mutableStateOf(false) }
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val tint by animateColorAsState(
        if (liked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "likeTint",
    )
    val like = stringResource(R.string.anim_micro_like)
    AnimExample(R.string.anim_micro_like_title, R.string.anim_micro_like_usage, "Animatable + spring()") {
        IconToggleButton(
            checked = liked,
            onCheckedChange = {
                liked = it
                scope.launch {
                    scale.animateTo(1.3f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
                    scale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
                }
            },
            modifier = Modifier.semantics { contentDescription = like },
        ) {
            Icon(
                if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = null,
                tint = tint,
                modifier =
                    Modifier.graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                    },
            )
        }
    }
}

@Composable
private fun CounterExample() {
    var count by rememberSaveable { mutableIntStateOf(0) }
    val increase = stringResource(R.string.anim_micro_increase)
    val decrease = stringResource(R.string.anim_micro_decrease)
    AnimExample(R.string.anim_micro_counter_title, R.string.anim_micro_counter_usage, "AnimatedContent") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s4)) {
            FilledTonalButton(onClick = { count-- }, modifier = Modifier.semantics { contentDescription = decrease }) {
                Text("−", Modifier.clearAndSetSemantics {})
            }
            AnimatedContent(
                targetState = count,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                transitionSpec = {
                    val up = targetState > initialState
                    (slideInVertically { if (up) it else -it } + fadeIn()) togetherWith
                        (slideOutVertically { if (up) -it else it } + fadeOut())
                },
                label = "counter",
            ) { value ->
                Text(value.toString(), style = MaterialTheme.typography.headlineMedium)
            }
            FilledTonalButton(onClick = { count++ }, modifier = Modifier.semantics { contentDescription = increase }) {
                Text("+", Modifier.clearAndSetSemantics {})
            }
        }
    }
}

@Composable
private fun TabIndicatorExample() {
    val labels = listOf(R.string.anim_micro_tab_photos, R.string.anim_micro_tab_videos, R.string.anim_micro_tab_albums)
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val xs = remember { mutableStateListOf(0, 0, 0) }
    val widths = remember { mutableStateListOf(0, 0, 0) }
    val x = remember { Animatable(0f) }
    val w = remember { Animatable(0f) }
    var placed by remember { mutableStateOf(false) }
    val ready = widths.all { it > 0 }
    val targetX = xs[selected]
    val targetW = widths[selected]
    // Snap to the first measured position (first display, rotation, restore); animate only later changes.
    LaunchedEffect(ready, selected, targetX, targetW) {
        if (!ready) return@LaunchedEffect
        if (!placed) {
            x.snapTo(targetX.toFloat())
            w.snapTo(targetW.toFloat())
            placed = true
        } else {
            coroutineScope {
                launch { x.animateTo(targetX.toFloat()) }
                launch { w.animateTo(targetW.toFloat()) }
            }
        }
    }
    val density = LocalDensity.current
    AnimExample(R.string.anim_micro_tabs_title, R.string.anim_micro_tabs_usage, "Animatable + Modifier.absoluteOffset { }") {
        Box(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.SpaceEvenly) {
                labels.forEachIndexed { index, label ->
                    Box(
                        modifier =
                            Modifier
                                .heightIn(min = MinTouchTarget)
                                .onGloballyPositioned {
                                    // positionInParent is absolute (left edge), so the indicator below uses an LTR frame.
                                    xs[index] = it.positionInParent().x.roundToInt()
                                    widths[index] = it.size.width
                                }.selectable(selected = index == selected, role = Role.Tab, onClick = { selected = index })
                                .padding(horizontal = Spacing.s3),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(label),
                            style = MaterialTheme.typography.titleSmall,
                            color =
                                if (index == selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    }
                }
            }
            if (placed) {
                // AbsoluteAlignment + absoluteOffset: pairs with positionInParent, which is also absolute, in LTR and RTL.
                Box(
                    Modifier
                        .align(AbsoluteAlignment.BottomLeft)
                        .absoluteOffset { IntOffset(x.value.roundToInt(), 0) }
                        .width(with(density) { w.value.toDp() })
                        .height(3.dp)
                        .testTag("tabIndicator")
                        .clip(RoundedCornerShape(percent = 50))
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}

@Composable
private fun ContentChangeExample() {
    val states = listOf(R.string.anim_micro_status_pending, R.string.anim_micro_status_paid, R.string.anim_micro_status_shipped)
    var index by rememberSaveable { mutableIntStateOf(0) }
    val next = stringResource(R.string.anim_micro_next_status)
    AnimExample(R.string.anim_micro_content_title, R.string.anim_micro_content_usage, "AnimatedContent + togetherWith") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s4)) {
            AnimatedContent(
                targetState = index,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                transitionSpec = {
                    (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut())
                },
                label = "status",
            ) { value ->
                Text(
                    stringResource(states[value]),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = Spacing.s4, vertical = Spacing.s1),
                )
            }
            FilledTonalButton(
                onClick = { index = (index + 1) % states.size },
                modifier = Modifier.semantics { contentDescription = next },
            ) { Text(stringResource(R.string.anim_micro_next), Modifier.clearAndSetSemantics {}) }
        }
    }
}

@ThemePreviews
@Composable
private fun MicroInteractionsScreenPreview() {
    PreviewTheme {
        MicroInteractionsScreen(onBack = {})
    }
}
