package com.primestrider.kotlinstarter.feature.example.gestures

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.primestrider.kotlinstarter.feature.catalog.api.CatalogEntry
import com.primestrider.kotlinstarter.feature.catalog.api.CatalogSection
import com.primestrider.kotlinstarter.feature.catalog.api.CatalogSections
import com.primestrider.kotlinstarter.feature.example.GestureDragRoute
import com.primestrider.kotlinstarter.feature.example.GesturePointerRoute
import com.primestrider.kotlinstarter.feature.example.GestureScrollRoute
import com.primestrider.kotlinstarter.feature.example.GestureSwipeRoute
import com.primestrider.kotlinstarter.feature.example.GestureTapRoute
import com.primestrider.kotlinstarter.feature.example.GestureTransformRoute
import com.primestrider.kotlinstarter.feature.gestures.impl.R
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import dagger.multibindings.IntoSet

/** The gesture example pages. */
fun NavGraphBuilder.gestureDestinations(navController: NavController) {
    val back: () -> Unit = { navController.popBackStack() }
    composable<GestureTapRoute> { TapScreen(onBack = back) }
    composable<GestureDragRoute> { DragScreen(onBack = back) }
    composable<GestureTransformRoute> { TransformScreen(onBack = back) }
    composable<GestureSwipeRoute> { SwipeScreen(onBack = back) }
    composable<GestureScrollRoute> { ScrollScreen(onBack = back) }
    composable<GesturePointerRoute> { PointerScreen(onBack = back) }
}

/** The Gestures section on Home and its rows. */
@Module
@InstallIn(SingletonComponent::class)
object GesturesCatalogModule {
    @Provides
    @IntoSet
    fun section(): CatalogSection =
        CatalogSection(
            CatalogSections.GESTURES,
            R.string.gesture_home_title,
            R.string.gesture_home_description,
            Icons.Filled.ThumbUp,
            order = 35,
        )

    @Provides
    @ElementsIntoSet
    fun entries(): Set<CatalogEntry> = gestureEntries().toSet()
}
