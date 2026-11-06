package com.primestrider.kotlinstarter.feature.example

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Star
import com.primestrider.kotlinstarter.feature.catalog.api.CatalogEntry
import com.primestrider.kotlinstarter.feature.catalog.api.CatalogSection
import com.primestrider.kotlinstarter.feature.catalog.api.CatalogSections
import com.primestrider.kotlinstarter.feature.catalog.impl.R
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import dagger.multibindings.Multibinds

/** Declares the Home sets other features contribute to, and the catalog's own sections and rows. */
@Module
@InstallIn(SingletonComponent::class)
abstract class CatalogModule {
    @Multibinds
    abstract fun sections(): Set<CatalogSection>

    @Multibinds
    abstract fun entries(): Set<CatalogEntry>

    companion object {
        @Provides
        @ElementsIntoSet
        fun catalogSections(): Set<CatalogSection> =
            setOf(
                CatalogSection(
                    CatalogSections.FEATURES,
                    R.string.example_home_features_title,
                    R.string.example_home_features_description,
                    Icons.Filled.Star,
                    order = 10,
                ),
                CatalogSection(
                    CatalogSections.STYLING,
                    R.string.example_home_styling_title,
                    R.string.example_home_styling_description,
                    Icons.Filled.Create,
                    order = 40,
                ),
            )

        @Provides
        @ElementsIntoSet
        fun catalogEntries(): Set<CatalogEntry> {
            val form =
                CatalogEntry(R.string.example_form_title, R.string.example_form_subtitle, FormExampleRoute, CatalogSections.FEATURES, 30)
            val styling =
                listOf(
                    Triple(R.string.example_styling_colors_title, R.string.example_styling_colors_subtitle, ColorsRoute),
                    Triple(R.string.example_styling_typography_title, R.string.example_styling_typography_subtitle, TypographyRoute),
                    Triple(R.string.example_styling_spacing_title, R.string.example_styling_spacing_subtitle, SpacingRoute),
                    Triple(R.string.example_styling_sizing_title, R.string.example_styling_sizing_subtitle, SizingRoute),
                    Triple(R.string.example_styling_layout_title, R.string.example_styling_layout_subtitle, LayoutRoute),
                    Triple(R.string.example_styling_appearance_title, R.string.example_styling_appearance_subtitle, AppearanceRoute),
                    Triple(R.string.example_styling_theme_title, R.string.example_styling_theme_subtitle, ThemeRoute),
                ).mapIndexed { index, (title, subtitle, route) -> CatalogEntry(title, subtitle, route, CatalogSections.STYLING, index) }
            return setOf(form) + styling
        }
    }
}
