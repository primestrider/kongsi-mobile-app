package com.primestrider.kotlinstarter.feature.example.animation.transitions

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.feature.animation.impl.R
import kotlinx.serialization.Serializable

/** Start page of the demo's own back stack. */
@Serializable
internal data object DemoListRoute

/** Detail page for one demo item. */
@Serializable
internal data class DemoDetailRoute(
    val itemId: Int,
)

private class DemoItem(
    val id: Int,
    @StringRes val title: Int,
)

private val demoItems =
    listOf(
        DemoItem(0, R.string.anim_transitions_item_mountain),
        DemoItem(1, R.string.anim_transitions_item_city),
        DemoItem(2, R.string.anim_transitions_item_ocean),
    )

private val cardShape = RoundedCornerShape(16.dp)

/** Theme container colour and its readable content colour for one demo item. */
@Composable
private fun itemColors(id: Int): Pair<Color, Color> {
    val c = MaterialTheme.colorScheme
    return when (id) {
        0 -> c.primaryContainer to c.onPrimaryContainer
        1 -> c.primary to c.onPrimary
        else -> c.tertiaryContainer to c.onTertiaryContainer
    }
}

/**
 * The demo's own [NavHost], inside a [SharedTransitionLayout] so container transform can morph a card into its
 * page. Every destination reads [pattern] when a transition starts, so a new choice applies to the next
 * navigation. The NavHost pops the detail on back (and follows the predictive back gesture); on the list its
 * handler is disabled, so back reaches the app's NavHost and leaves the page.
 */
@Composable
internal fun DemoNavHost(
    navController: NavHostController,
    pattern: State<TransitionPattern>,
    modifier: Modifier = Modifier,
) {
    val slidePx by rememberUpdatedState(with(LocalDensity.current) { MotionSpec.SHARED_AXIS_SLIDE.roundToPx() })
    val layoutDirection by rememberUpdatedState(LocalLayoutDirection.current)
    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
        { pattern.value.enter(forward = true, slidePx, layoutDirection) }
    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
        { pattern.value.exit(forward = true, slidePx, layoutDirection) }
    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
        { pattern.value.enter(forward = false, slidePx, layoutDirection) }
    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
        { pattern.value.exit(forward = false, slidePx, layoutDirection) }

    SharedTransitionLayout(modifier) {
        NavHost(navController, startDestination = DemoListRoute) {
            composable<DemoListRoute>(
                enterTransition = enter,
                exitTransition = exit,
                popEnterTransition = popEnter,
                popExitTransition = popExit,
            ) { entry ->
                DemoList(
                    morph = pattern.value == TransitionPattern.CONTAINER_TRANSFORM,
                    visibilityScope = this,
                    onOpen = { id ->
                        // Ignore taps while a transition is still running (the entry is resumed only once it settles),
                        // so a double tap cannot stack two detail pages.
                        if (entry.lifecycle.currentState == Lifecycle.State.RESUMED) {
                            navController.navigate(DemoDetailRoute(id))
                        }
                    },
                )
            }
            composable<DemoDetailRoute>(
                enterTransition = enter,
                exitTransition = exit,
                popEnterTransition = popEnter,
                popExitTransition = popExit,
            ) { entry ->
                DemoDetail(
                    itemId = entry.toRoute<DemoDetailRoute>().itemId,
                    morph = pattern.value == TransitionPattern.CONTAINER_TRANSFORM,
                    visibilityScope = this,
                    onBack = {
                        // Only the page that is on top may pop, so a double tap never pops the list as well.
                        if (navController.currentBackStackEntry == entry) navController.popBackStack()
                    },
                )
            }
        }
    }
}

/** Shared bounds for container transform; a no-op for the other patterns. */
@Composable
private fun SharedTransitionScope.morphBounds(
    itemId: Int,
    morph: Boolean,
    visibilityScope: AnimatedVisibilityScope,
): Modifier =
    if (morph) {
        Modifier.sharedBounds(rememberSharedContentState("item-$itemId"), animatedVisibilityScope = visibilityScope)
    } else {
        Modifier
    }

@Composable
private fun SharedTransitionScope.DemoList(
    morph: Boolean,
    visibilityScope: AnimatedVisibilityScope,
    onOpen: (Int) -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(Spacing.s3),
        verticalArrangement = Arrangement.spacedBy(Spacing.s3),
    ) {
        demoItems.forEach { item ->
            val (container, content) = itemColors(item.id)
            Card(
                onClick = { onOpen(item.id) },
                shape = cardShape,
                colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
                modifier =
                    morphBounds(item.id, morph, visibilityScope)
                        .fillMaxWidth()
                        .heightIn(min = 72.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(horizontal = Spacing.s4),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
                ) {
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(content.copy(alpha = 0.16f)))
                    Text(stringResource(item.title), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun SharedTransitionScope.DemoDetail(
    itemId: Int,
    morph: Boolean,
    visibilityScope: AnimatedVisibilityScope,
    onBack: () -> Unit,
) {
    val item = demoItems.first { it.id == itemId }
    val (container, content) = itemColors(item.id)
    val title = stringResource(item.title)
    Surface(morphBounds(item.id, morph, visibilityScope).fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        // Scrolls so the Back button stays reachable inside the fixed frame at large font sizes.
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .background(container)
                    .padding(Spacing.s4),
                contentAlignment = Alignment.BottomStart,
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = content,
                    modifier =
                        Modifier.semantics {
                            heading()
                            liveRegion = LiveRegionMode.Polite
                        },
                )
            }
            Text(
                stringResource(R.string.anim_transitions_detail_body, title),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = Spacing.s4),
            )
            FilledTonalButton(
                onClick = onBack,
                modifier = Modifier.padding(horizontal = Spacing.s4),
            ) { Text(stringResource(R.string.anim_transitions_back)) }
        }
    }
}
