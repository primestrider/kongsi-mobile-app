package com.primestrider.kotlinstarter.feature.catalog.api

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * A group of Home entries (a card with its own filter chip). A feature declares the sections it owns with
 * `@Provides @IntoSet CatalogSection`; sections without entries are not shown.
 */
data class CatalogSection(
    val id: String,
    @StringRes val title: Int,
    @StringRes val description: Int,
    val icon: ImageVector,
    /** Position on Home, ascending. */
    val order: Int,
)

/**
 * One Home row that opens [route]. A feature contributes its own rows with `@Provides @IntoSet CatalogEntry`
 * (or `@ElementsIntoSet`), so deleting the feature removes its rows. Entries for an unknown section are dropped.
 */
data class CatalogEntry(
    @StringRes val title: Int,
    @StringRes val subtitle: Int,
    val route: Any,
    val sectionId: String,
    /** Position inside the section, ascending. */
    val order: Int,
)

/** Section ids used by the example features. */
object CatalogSections {
    const val FEATURES = "features"
    const val DEVICES = "devices"
    const val ANIMATION = "animation"
    const val GESTURES = "gestures"
    const val STYLING = "styling"
}
