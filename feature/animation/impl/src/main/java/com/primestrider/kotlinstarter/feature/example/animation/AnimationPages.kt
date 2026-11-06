package com.primestrider.kotlinstarter.feature.example.animation

import androidx.annotation.StringRes
import com.primestrider.kotlinstarter.feature.animation.impl.R
import com.primestrider.kotlinstarter.feature.catalog.api.CatalogEntry
import com.primestrider.kotlinstarter.feature.catalog.api.CatalogSections
import com.primestrider.kotlinstarter.feature.example.AnimCardsRoute
import com.primestrider.kotlinstarter.feature.example.AnimListsRoute
import com.primestrider.kotlinstarter.feature.example.AnimMicroRoute
import com.primestrider.kotlinstarter.feature.example.AnimTransitionsRoute

/** One page of the "Animations" Home section. */
data class AnimationPage(
    val route: Any,
    @StringRes val title: Int,
    @StringRes val subtitle: Int,
)

/**
 * Pages in the order they appear on Home: Page transitions, Cards, Lists & loading, Micro-interactions.
 * To add one: a route, an entry here, a `composable` in `homeGraph` and its strings.
 */
val animationPages: List<AnimationPage> =
    listOf(
        AnimationPage(AnimTransitionsRoute, R.string.anim_transitions_title, R.string.anim_transitions_subtitle),
        AnimationPage(AnimCardsRoute, R.string.anim_cards_title, R.string.anim_cards_subtitle),
        AnimationPage(AnimListsRoute, R.string.anim_lists_title, R.string.anim_lists_subtitle),
        AnimationPage(AnimMicroRoute, R.string.anim_micro_title, R.string.anim_micro_subtitle),
    )

/** Home entries for the "Animations" section, built from [animationPages]. */
fun animationEntries(): List<CatalogEntry> =
    animationPages.mapIndexed { index, page -> CatalogEntry(page.title, page.subtitle, page.route, CatalogSections.ANIMATION, index) }
