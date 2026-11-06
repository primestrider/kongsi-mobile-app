package com.primestrider.kotlinstarter.feature.example.animation.cards

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.feature.animation.impl.R
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold
import com.primestrider.kotlinstarter.feature.example.animation.AnimExample
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private val sampleCards =
    listOf(
        R.string.anim_cards_item_mountain,
        R.string.anim_cards_item_city,
        R.string.anim_cards_item_ocean,
        R.string.anim_cards_item_forest,
        R.string.anim_cards_item_desert,
    ).mapIndexed { index, title -> DemoCard(index, title, index) }

private val cardShape = RoundedCornerShape(16.dp)

/** How much of the neighbouring pages peeks in beside the centred carousel card. */
private val CarouselPeek = 56.dp

/** Card motion: press feedback, a scaling carousel, expand, swipe to archive with undo, and a flip. */
@Composable
fun CardsScreen(onBack: () -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    ExamplePageScaffold(
        R.string.anim_cards_title,
        R.string.anim_cards_subtitle,
        onBack,
        snackbarHost = { SnackbarHost(snackbar) },
    ) {
        PressExample()
        CarouselExample()
        ExpandExample()
        SwipeExample(snackbar)
        FlipExample()
    }
}

/** Gradient and the text colour that is readable on it, both from theme roles. */
private class CardLook(
    val brush: Brush,
    val onColor: Color,
)

@Composable
private fun cardLook(colorIndex: Int): CardLook {
    val c = MaterialTheme.colorScheme
    return remember(c, colorIndex) {
        when (colorIndex.mod(5)) {
            0 -> CardLook(Brush.linearGradient(listOf(c.primary, c.tertiary)), c.onPrimary)
            1 -> CardLook(Brush.linearGradient(listOf(c.secondary, c.primary)), c.onPrimary)
            2 -> CardLook(Brush.linearGradient(listOf(c.tertiary, c.secondary)), c.onPrimary)
            3 -> CardLook(Brush.linearGradient(listOf(c.primaryContainer, c.tertiaryContainer)), c.onPrimaryContainer)
            else -> CardLook(Brush.linearGradient(listOf(c.secondaryContainer, c.primaryContainer)), c.onPrimaryContainer)
        }
    }
}

@Composable
private fun PressExample() {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    var count by rememberSaveable { mutableIntStateOf(0) }
    val scale =
        animateFloatAsState(
            if (pressed) 0.96f else 1f,
            spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
            label = "press",
        )
    AnimExample(R.string.anim_cards_press_title, R.string.anim_cards_press_usage, "collectIsPressedAsState + spring()") {
        Card(
            onClick = { count++ },
            interactionSource = source,
            modifier =
                Modifier.fillMaxWidth().heightIn(min = 72.dp).graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                },
        ) {
            Column(Modifier.padding(Spacing.s4)) {
                Text(stringResource(R.string.anim_cards_press_label))
                Text(
                    pluralStringResource(R.plurals.anim_cards_press_count, count, count),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CarouselExample() {
    val pagerState = rememberPagerState { sampleCards.size }
    AnimExample(R.string.anim_cards_carousel_title, R.string.anim_cards_carousel_usage, "HorizontalPager + graphicsLayer") {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.testTag("carousel"),
            contentPadding = PaddingValues(horizontal = CarouselPeek),
            pageSpacing = Spacing.s2,
        ) { page ->
            val look = cardLook(sampleCards[page].colorIndex)
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp)
                    .graphicsLayer {
                        val offset = pagerState.getOffsetDistanceInPages(page).absoluteValue
                        val s = carouselScale(offset)
                        scaleX = s
                        scaleY = s
                        alpha = carouselAlpha(offset)
                    }.clip(cardShape)
                    .background(look.brush)
                    .padding(Spacing.s4),
                contentAlignment = Alignment.BottomStart,
            ) {
                Text(stringResource(sampleCards[page].title), style = MaterialTheme.typography.titleLarge, color = look.onColor)
            }
        }
        Text(
            stringResource(R.string.anim_cards_carousel_position, pagerState.currentPage + 1, sampleCards.size),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

@Composable
private fun ExpandExample() {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val rotation = animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    val stateText = stringResource(if (expanded) R.string.anim_cards_expanded else R.string.anim_cards_collapsed)
    AnimExample(R.string.anim_cards_expand_title, R.string.anim_cards_expand_usage, "Modifier.animateContentSize()") {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag("expandCard")
                    .animateContentSize()
                    .semantics { stateDescription = stateText }
                    .toggleable(value = expanded, role = Role.Switch, onValueChange = { expanded = it }),
        ) {
            Column(Modifier.padding(horizontal = Spacing.s4)) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(stringResource(R.string.anim_cards_expand_header), style = MaterialTheme.typography.titleMedium)
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.graphicsLayer { rotationZ = rotation.value },
                    )
                }
                if (expanded) {
                    Text(
                        stringResource(R.string.anim_cards_expand_body),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = Spacing.s4),
                    )
                }
            }
        }
    }
}

@Composable
private fun SwipeExample(snackbar: SnackbarHostState) {
    val archive = rememberSaveable(saver = ArchiveState.Saver) { ArchiveState(sampleCards) }
    val context = LocalContext.current
    val undoLabel = stringResource(R.string.anim_cards_undo)
    val scope = rememberCoroutineScope()

    // "Archived <title>" with Undo. A newer archive replaces it, so Undo only ever applies to the latest one.
    suspend fun offerUndo(card: DemoCard) {
        snackbar.currentSnackbarData?.dismiss()
        val result =
            snackbar.showSnackbar(
                message = context.getString(R.string.anim_cards_archived, context.getString(card.title)),
                actionLabel = undoLabel,
                duration = SnackbarDuration.Long,
            )
        if (result == SnackbarResult.ActionPerformed) archive.undo()
    }

    fun archiveCard(card: DemoCard) {
        archive.archive(card.id)
        scope.launch { offerUndo(card) }
    }

    // After rotation or process death the archive stack is restored but the snackbar is not: offer Undo again.
    LaunchedEffect(Unit) { archive.lastArchived?.let { offerUndo(it) } }

    AnimExample(
        R.string.anim_cards_swipe_title,
        R.string.anim_cards_swipe_usage,
        "SwipeToDismissBox + rememberSwipeToDismissBoxState",
    ) {
        // Always available: it brings back every archived card, including those Undo can no longer reach.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            FilledTonalButton(
                onClick = {
                    archive.reset()
                    snackbar.currentSnackbarData?.dismiss()
                },
            ) {
                Text(stringResource(R.string.anim_cards_reset))
            }
        }
        if (archive.cards.isEmpty()) {
            Text(
                stringResource(R.string.anim_cards_empty),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        Column(
            Modifier.fillMaxWidth().animateContentSize().testTag("swipeList"),
            verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        ) {
            archive.cards.forEach { card ->
                key(card.id) { SwipeCard(card, onArchive = { archiveCard(card) }) }
            }
        }
    }
}

@Composable
private fun SwipeCard(
    card: DemoCard,
    onArchive: () -> Unit,
) {
    val state = rememberSwipeToDismissBoxState()
    val look = cardLook(card.colorIndex)
    val title = stringResource(card.title)
    SwipeToDismissBox(
        state = state,
        onDismiss = { onArchive() },
        backgroundContent = {
            // Drawn only while the card is moving, so no tinted fringe shows around its rounded corners at rest.
            if (state.dismissDirection != SwipeToDismissBoxValue.Settled) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(cardShape)
                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                        .padding(horizontal = Spacing.s4),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.anim_cards_archive_hint), color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
        },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(look.brush)
                .padding(start = Spacing.s4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = look.onColor,
                modifier = Modifier.weight(1f).padding(vertical = Spacing.s4),
            )
            IconButton(onClick = onArchive) {
                Icon(
                    ArchiveIcon,
                    contentDescription = stringResource(R.string.anim_cards_archive_action, title),
                    tint = look.onColor,
                )
            }
        }
    }
}

@Composable
private fun FlipExample() {
    var flipped by rememberSaveable { mutableStateOf(false) }
    val rotation = animateFloatAsState(if (flipped) 180f else 0f, spring(Spring.DampingRatioLowBouncy), label = "flip")
    val density = LocalDensity.current.density
    val stateText = stringResource(if (flipped) R.string.anim_cards_showing_back else R.string.anim_cards_showing_front)
    // Recomposes only when the face changes, not on every animation frame.
    val showBack by remember { derivedStateOf { rotation.value > 90f } }
    val look = cardLook(if (showBack) 4 else 0)
    AnimExample(R.string.anim_cards_flip_title, R.string.anim_cards_flip_usage, "graphicsLayer { rotationY; cameraDistance }") {
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 140.dp)
                .testTag("flipCard")
                .graphicsLayer {
                    rotationY = rotation.value
                    cameraDistance = 12f * density
                }.clip(cardShape)
                .background(look.brush)
                .semantics { stateDescription = stateText }
                .toggleable(value = flipped, role = Role.Switch, onValueChange = { flipped = it })
                .padding(Spacing.s4),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(if (showBack) R.string.anim_cards_flip_back else R.string.anim_cards_flip_front),
                style = MaterialTheme.typography.titleLarge,
                color = look.onColor,
                // Past 90 degrees the whole layer is mirrored, so turn the text back the right way round.
                modifier = Modifier.graphicsLayer { if (showBack) rotationY = 180f },
            )
        }
        FilledTonalButton(onClick = { flipped = !flipped }) {
            Text(stringResource(R.string.anim_cards_flip_button))
        }
    }
}

@ThemePreviews
@Composable
private fun CardsScreenPreview() {
    PreviewTheme {
        CardsScreen(onBack = {})
    }
}
