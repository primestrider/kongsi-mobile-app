package com.primestrider.kotlinstarter.feature.example.animation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.primestrider.kotlinstarter.feature.animation.impl.R
import com.primestrider.kotlinstarter.feature.catalog.api.CatalogEntry
import com.primestrider.kotlinstarter.feature.catalog.api.CatalogSection
import com.primestrider.kotlinstarter.feature.catalog.api.CatalogSections
import com.primestrider.kotlinstarter.feature.example.AnimCardsRoute
import com.primestrider.kotlinstarter.feature.example.AnimListsRoute
import com.primestrider.kotlinstarter.feature.example.AnimMicroRoute
import com.primestrider.kotlinstarter.feature.example.AnimTransitionsRoute
import com.primestrider.kotlinstarter.feature.example.animation.cards.CardsScreen
import com.primestrider.kotlinstarter.feature.example.animation.lists.ListsScreen
import com.primestrider.kotlinstarter.feature.example.animation.micro.MicroInteractionsScreen
import com.primestrider.kotlinstarter.feature.example.animation.transitions.TransitionsScreen
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import dagger.multibindings.IntoSet

/** The four animation example pages. */
fun NavGraphBuilder.animationDestinations(navController: NavController) {
    val back: () -> Unit = { navController.popBackStack() }
    composable<AnimTransitionsRoute> { TransitionsScreen(onBack = back) }
    composable<AnimCardsRoute> { CardsScreen(onBack = back) }
    composable<AnimListsRoute> { ListsScreen(onBack = back) }
    composable<AnimMicroRoute> { MicroInteractionsScreen(onBack = back) }
}

/** The Animations section on Home and its four rows. */
@Module
@InstallIn(SingletonComponent::class)
object AnimationCatalogModule {
    @Provides
    @IntoSet
    fun section(): CatalogSection =
        CatalogSection(
            CatalogSections.ANIMATION,
            R.string.example_home_anim_title,
            R.string.example_home_anim_description,
            Icons.Filled.PlayArrow,
            order = 30,
        )

    @Provides
    @ElementsIntoSet
    fun entries(): Set<CatalogEntry> = animationEntries().toSet()
}
