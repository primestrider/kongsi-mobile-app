package com.primestrider.kotlinstarter.feature.example

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.primestrider.kotlinstarter.feature.catalog.api.CatalogEntry
import com.primestrider.kotlinstarter.feature.catalog.api.CatalogSection
import com.primestrider.kotlinstarter.feature.catalog.impl.R

data class ExampleEntry(
    @StringRes val title: Int,
    @StringRes val subtitle: Int,
    val route: Any,
)

data class ExampleSection(
    @StringRes val title: Int,
    @StringRes val description: Int,
    val entries: List<ExampleEntry>,
    /** Shown in the section's header and on its filter chip. */
    val icon: ImageVector,
)

/** Root of the Components tab. */
val componentEntries: List<ExampleEntry> =
    listOf(
        ExampleEntry(R.string.example_components_actions_title, R.string.example_components_actions_subtitle, ActionsRoute),
        ExampleEntry(R.string.example_components_inputs_title, R.string.example_components_inputs_subtitle, InputsRoute),
        ExampleEntry(R.string.example_components_display_title, R.string.example_components_display_subtitle, DisplayRoute),
        ExampleEntry(R.string.example_components_overlays_title, R.string.example_components_overlays_subtitle, OverlaysRoute),
        ExampleEntry(R.string.example_components_feedback_title, R.string.example_components_feedback_subtitle, FeedbackRoute),
        ExampleEntry(R.string.example_components_carousel_title, R.string.example_components_carousel_subtitle, CarouselRoute),
        ExampleEntry(R.string.example_components_app_bars_title, R.string.example_components_app_bars_subtitle, AppBarsRoute),
        ExampleEntry(R.string.example_components_pickers_title, R.string.example_components_pickers_subtitle, PickersRoute),
    )

/**
 * Home content from what the wired features contribute: sections in [CatalogSection.order], their entries in
 * [CatalogEntry.order]. A section without entries and an entry whose section nobody declares are left out.
 */
fun buildSections(
    sections: Set<CatalogSection>,
    entries: Set<CatalogEntry>,
): List<ExampleSection> {
    val bySection = entries.groupBy { it.sectionId }
    return sections
        .sortedBy { it.order }
        .mapNotNull { section ->
            val rows = bySection[section.id].orEmpty().sortedBy { it.order }
            if (rows.isEmpty()) {
                null
            } else {
                ExampleSection(
                    title = section.title,
                    description = section.description,
                    entries = rows.map { ExampleEntry(it.title, it.subtitle, it.route) },
                    icon = section.icon,
                )
            }
        }
}
