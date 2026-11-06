package com.primestrider.kotlinstarter.feature.example.animation.lists

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.primestrider.kotlinstarter.core.designsystem.component.Skeleton
import com.primestrider.kotlinstarter.core.designsystem.component.SkeletonText
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.feature.animation.impl.R
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold
import com.primestrider.kotlinstarter.feature.example.animation.AnimExample
import com.primestrider.kotlinstarter.feature.example.animation.MinTouchTarget
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random
import com.primestrider.kotlinstarter.core.designsystem.R as DesignR

private const val RELOAD_DELAY_MS = 1_500L
private const val LOADED_ROWS = 3
private const val SCROLL_ROWS = 14
private val ListFrameHeight = 280.dp
private val HeaderFrameHeight = 260.dp
private val HeroMaxHeight = 160.dp
private val HeroMinHeight = 56.dp

/** Loading and list motion: a placeholder that gives way to content, animated list changes and a collapsing header. */
@Composable
fun ListsScreen(onBack: () -> Unit) {
    ExamplePageScaffold(R.string.anim_lists_title, R.string.anim_lists_subtitle, onBack) {
        LoadingExample()
        AnimatedListExample()
        CollapsingHeaderExample()
    }
}

@Composable
private fun LoadingExample() {
    // Saveable: after rotation mid-load the effect below restarts the wait; once loaded it stays loaded.
    var loading by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(loading) {
        if (loading) {
            delay(RELOAD_DELAY_MS)
            loading = false
        }
    }
    AnimExample(
        R.string.anim_lists_shimmer_title,
        R.string.anim_lists_shimmer_usage,
        "rememberInfiniteTransition + Brush.horizontalGradient",
    ) {
        FilledTonalButton(
            onClick = { loading = true },
            enabled = !loading,
        ) {
            Text(stringResource(R.string.anim_lists_reload))
        }
        // The placeholder (and its infinite transition) leaves composition as soon as the content shows.
        Column(
            Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(Spacing.s3),
        ) {
            if (loading) {
                val loadingText = stringResource(DesignR.string.component_loading)
                Column(
                    Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = loadingText },
                    verticalArrangement = Arrangement.spacedBy(Spacing.s3),
                ) {
                    repeat(LOADED_ROWS) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s3), verticalAlignment = Alignment.CenterVertically) {
                            Skeleton(Modifier.size(40.dp), RoundedCornerShape(20.dp), shimmer = true)
                            SkeletonText(lines = 2, modifier = Modifier.weight(1f), shimmer = true)
                        }
                    }
                }
            } else {
                repeat(LOADED_ROWS) { index ->
                    LoadedRow(stringResource(R.string.anim_lists_loaded_row, index + 1))
                }
            }
        }
    }
}

@Composable
private fun LoadedRow(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s3), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(20.dp)))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun AnimatedListExample() {
    val list = rememberSaveable(saver = DemoListState.Saver) { DemoListState((1..3).map { DemoRow(it) }) }
    AnimExample(R.string.anim_lists_list_title, R.string.anim_lists_list_usage, "LazyColumn + Modifier.animateItem()") {
        Row(Modifier.testTag("listActions"), horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            FilledTonalButton(onClick = { list.add() }) {
                Text(stringResource(R.string.anim_lists_add))
            }
            FilledTonalButton(
                onClick = { list.shuffle(Random.Default) },
                enabled = list.rows.size > 1,
            ) {
                Text(stringResource(R.string.anim_lists_shuffle))
            }
        }
        OutlinedCard(Modifier.fillMaxWidth().height(ListFrameHeight)) {
            // The list stays composed even when empty, so the last row's removal animation can finish.
            Box(Modifier.fillMaxSize()) {
                LazyColumn(Modifier.fillMaxSize().testTag("demoList")) {
                    itemsIndexed(list.rows, key = { _, row -> row.id }) { index, row ->
                        val label = stringResource(R.string.anim_lists_item, row.id)
                        Row(
                            Modifier
                                .animateItem(placementSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow))
                                .fillMaxWidth()
                                .padding(start = Spacing.s4),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            RowButton(
                                Icons.Filled.KeyboardArrowUp,
                                stringResource(R.string.anim_lists_move_up, label),
                                enabled = index > 0,
                            ) { list.moveUp(row.id) }
                            RowButton(
                                Icons.Filled.KeyboardArrowDown,
                                stringResource(R.string.anim_lists_move_down, label),
                                enabled = index < list.rows.lastIndex,
                            ) { list.moveDown(row.id) }
                            RowButton(Icons.Filled.Close, stringResource(R.string.anim_lists_remove, label)) { list.remove(row.id) }
                        }
                    }
                }
                if (list.rows.isEmpty()) {
                    Column(
                        Modifier.fillMaxSize().testTag("emptyState").semantics { liveRegion = LiveRegionMode.Polite },
                        verticalArrangement = Arrangement.spacedBy(Spacing.s2, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(stringResource(R.string.anim_lists_empty), style = MaterialTheme.typography.titleMedium)
                        FilledTonalButton(onClick = { list.add() }) {
                            Text(stringResource(R.string.anim_lists_add))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(icon, contentDescription = description)
    }
}

@Composable
private fun CollapsingHeaderExample() {
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    val maxPx = with(density) { HeroMaxHeight.toPx() }
    val minPx = with(density) { HeroMinHeight.toPx() }
    val rangePx = (maxPx - minPx).roundToInt()
    AnimExample(R.string.anim_lists_header_title, R.string.anim_lists_header_usage, "ScrollState + graphicsLayer + layout") {
        // A fixed-height frame with its own scroll, so it does not fight the page's scroll.
        Box(
            Modifier
                .fillMaxWidth()
                .height(HeaderFrameHeight)
                .testTag("headerFrame")
                .clipToBounds()
                .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(12.dp)),
        ) {
            Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = Spacing.s4)) {
                Spacer(Modifier.height(HeroMaxHeight))
                repeat(SCROLL_ROWS) { index ->
                    Text(
                        stringResource(R.string.anim_lists_scroll_row, index + 1),
                        Modifier.fillMaxWidth().heightIn(min = MinTouchTarget).padding(vertical = Spacing.s3),
                    )
                }
            }
            HeroHeader({ scroll.value }, maxPx, minPx, rangePx)
        }
    }
}

@Composable
private fun HeroHeader(
    scrollPx: () -> Int,
    maxPx: Float,
    minPx: Float,
    rangePx: Int,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                // Height follows the scroll; read here so scrolling re-measures instead of recomposing.
                val fraction = collapseFraction(scrollPx(), rangePx)
                val h = (maxPx + (minPx - maxPx) * fraction).roundToInt()
                val placeable = measurable.measure(constraints.copy(minHeight = h, maxHeight = h))
                layout(placeable.width, h) { placeable.place(IntOffset.Zero) }
            }.clipToBounds()
            .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary))),
    ) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(Spacing.s4)
                .size(72.dp)
                .graphicsLayer { translationY = parallaxOffset(scrollPx()) }
                .background(colors.onPrimary.copy(alpha = 0.25f), RoundedCornerShape(16.dp)),
        )
        Text(
            stringResource(R.string.anim_lists_hero_title),
            Modifier
                .align(Alignment.BottomStart)
                .padding(Spacing.s4)
                .graphicsLayer { alpha = 1f - collapseFraction(scrollPx(), rangePx) },
            style = MaterialTheme.typography.headlineSmall,
            color = colors.onPrimary,
        )
        // Same title, shown small once collapsed; hidden from accessibility so it is not read twice.
        Text(
            stringResource(R.string.anim_lists_hero_title),
            Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = Spacing.s4)
                .graphicsLayer { alpha = collapseFraction(scrollPx(), rangePx) }
                .clearAndSetSemantics {},
            style = MaterialTheme.typography.titleMedium,
            color = colors.onPrimary,
        )
    }
}

@ThemePreviews
@Composable
private fun ListsScreenPreview() {
    PreviewTheme {
        ListsScreen(onBack = {})
    }
}
