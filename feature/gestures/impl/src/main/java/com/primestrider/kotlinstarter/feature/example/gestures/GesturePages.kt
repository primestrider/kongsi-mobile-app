package com.primestrider.kotlinstarter.feature.example.gestures

import androidx.annotation.StringRes
import com.primestrider.kotlinstarter.feature.catalog.api.CatalogEntry
import com.primestrider.kotlinstarter.feature.catalog.api.CatalogSections
import com.primestrider.kotlinstarter.feature.example.GestureDragRoute
import com.primestrider.kotlinstarter.feature.example.GesturePointerRoute
import com.primestrider.kotlinstarter.feature.example.GestureScrollRoute
import com.primestrider.kotlinstarter.feature.example.GestureSwipeRoute
import com.primestrider.kotlinstarter.feature.example.GestureTapRoute
import com.primestrider.kotlinstarter.feature.example.GestureTransformRoute
import com.primestrider.kotlinstarter.feature.gestures.impl.R

/** One page of the "Gestures" Home section. */
data class GesturePage(
    val route: Any,
    @StringRes val title: Int,
    @StringRes val subtitle: Int,
)

/**
 * Pages in the order they appear on Home. To add one: a route in `GestureRoutes.kt`, an entry here, a `composable`
 * in [gestureDestinations] and its strings.
 */
val gesturePages: List<GesturePage> =
    listOf(
        GesturePage(GestureTapRoute, R.string.gesture_tap_title, R.string.gesture_tap_subtitle),
        GesturePage(GestureDragRoute, R.string.gesture_drag_title, R.string.gesture_drag_subtitle),
        GesturePage(GestureTransformRoute, R.string.gesture_transform_title, R.string.gesture_transform_subtitle),
        GesturePage(GestureSwipeRoute, R.string.gesture_swipe_title, R.string.gesture_swipe_subtitle),
        GesturePage(GestureScrollRoute, R.string.gesture_scroll_title, R.string.gesture_scroll_subtitle),
        GesturePage(GesturePointerRoute, R.string.gesture_pointer_title, R.string.gesture_pointer_subtitle),
    )

/** Home entries for the "Gestures" section, built from [gesturePages]. */
fun gestureEntries(): List<CatalogEntry> =
    gesturePages.mapIndexed { index, page -> CatalogEntry(page.title, page.subtitle, page.route, CatalogSections.GESTURES, index) }
