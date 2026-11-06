package com.primestrider.kotlinstarter.feature.example

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.primestrider.kotlinstarter.core.designsystem.component.EmptyState
import com.primestrider.kotlinstarter.core.designsystem.component.SearchField
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.feature.catalog.impl.R
import com.primestrider.kotlinstarter.core.designsystem.R as DesignR

const val HOME_LIST_TAG = "home_list"

/** The "All" chip: no section chosen. String-resource ids are never 0. */
private const val ALL_SECTIONS = 0

/**
 * A titled list of example pages grouped in section cards. With more than one section it adds a search box
 * (titles and subtitles, in the app language) and a chip per section; both survive recreation. The title scrolls with
 * the list; the search box and chips stay pinned above it, so scrolling never takes focus from the field.
 */
@Composable
fun HomeScreen(
    @StringRes title: Int = R.string.catalog_home_title,
    sections: List<ExampleSection>,
    onOpen: (Any) -> Unit,
) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    // The chosen section's title id, not its index, so a reordered or removed section never selects another one.
    var selected by rememberSaveable { mutableIntStateOf(ALL_SECTIONS) }
    val filters = sections.size > 1
    val activeSelection = selected.takeIf { id -> sections.any { it.title == id } }
    val shown = filterSections(sections, query, activeSelection) { context.getString(it) }
    val totals = sections.associate { it.title to it.entries.size }
    val narrowed = query.isNotBlank() || activeSelection != null

    LazyColumn(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().testTag(HOME_LIST_TAG),
        contentPadding = PaddingValues(start = Spacing.s4, end = Spacing.s4, bottom = Spacing.s4),
        verticalArrangement = Arrangement.spacedBy(Spacing.s4),
    ) {
        item(key = "title") {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = Spacing.s4).semantics { heading() },
            )
        }
        if (filters) {
            stickyHeader(key = "filters") {
                // Opaque, so cards scrolling underneath never show through the pinned search and chips.
                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2), modifier = Modifier.padding(vertical = Spacing.s2)) {
                        SearchField(
                            query = query,
                            onQueryChange = { query = it },
                            placeholder = stringResource(R.string.home_search_placeholder),
                        )
                        SectionChips(sections, activeSelection ?: ALL_SECTIONS, onSelect = { selected = it })
                        if (narrowed && shown.isNotEmpty()) ResultsLine(shown.sumOf { it.entries.size })
                    }
                }
            }
        }
        items(shown, key = { it.title }) { section ->
            SectionCard(section, totals.getValue(section.title), onOpen)
        }
        if (shown.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    title = stringResource(R.string.home_no_match, query.trim()),
                    description = stringResource(R.string.home_no_match_hint),
                    icon = Icons.Filled.Search,
                    actionLabel = stringResource(DesignR.string.component_clear_search),
                    onAction = {
                        query = ""
                        selected = ALL_SECTIONS
                    },
                    modifier =
                        Modifier.padding(top = Spacing.s8).semantics(mergeDescendants = true) {
                            liveRegion = LiveRegionMode.Polite
                        },
                )
            }
        }
    }
}

/** A small visible line under the chips; a polite live region, so a screen reader hears how many entries are left. */
@Composable
private fun ResultsLine(count: Int) {
    Text(
        pluralStringResource(R.plurals.home_results, count, count),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

/** "All" plus one chip per section; picking one shows only that section. */
@Composable
private fun SectionChips(
    sections: List<ExampleSection>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
    ) {
        Chip(stringResource(R.string.home_filter_all), selected == ALL_SECTIONS, icon = null) { onSelect(ALL_SECTIONS) }
        sections.forEach { section ->
            Chip(stringResource(section.title), selected == section.title, section.icon) { onSelect(section.title) }
        }
    }
}

@Composable
private fun Chip(
    label: String,
    isSelected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = icon?.let { { Icon(it, contentDescription = null, modifier = Modifier.size(18.dp)) } },
        modifier = Modifier.semantics { role = Role.RadioButton },
    )
}

/** One section: an icon, title, description and example count, then its entries as rows. */
@Composable
private fun SectionCard(
    section: ExampleSection,
    total: Int,
    onOpen: (Any) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        SectionHeader(section, total)
        section.entries.forEachIndexed { index, entry ->
            HorizontalDivider(
                modifier = Modifier.padding(start = if (index == 0) 0.dp else Spacing.s4),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            EntryRow(entry, onOpen)
        }
    }
}

@Composable
private fun SectionHeader(
    section: ExampleSection,
    total: Int,
) {
    val shown = section.entries.size
    val narrowed = shown < total
    // When the section is cut down the badge reads "2/7", so a filtered count is not mistaken for the total.
    val badge = if (narrowed) stringResource(R.string.home_section_badge_of, shown, total) else shown.toString()
    val count =
        if (narrowed) {
            pluralStringResource(R.plurals.home_section_count_of, total, shown, total)
        } else {
            pluralStringResource(R.plurals.home_section_count, shown, shown)
        }
    Row(
        Modifier.fillMaxWidth().padding(Spacing.s4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                Icon(section.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(section.title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(section.description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
            Text(
                badge,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier =
                    Modifier
                        .padding(horizontal = Spacing.s2, vertical = Spacing.s1)
                        .clearAndSetSemantics { contentDescription = count },
            )
        }
    }
}

@Composable
private fun EntryRow(
    entry: ExampleEntry,
    onOpen: (Any) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button) { onOpen(entry.route) }
            .padding(horizontal = Spacing.s4, vertical = Spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(entry.title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(entry.subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@ThemePreviews
@Composable
private fun HomeScreenPreview() {
    PreviewTheme {
        HomeScreen(
            sections =
                listOf(
                    ExampleSection(
                        title = R.string.example_home_components_title,
                        description = R.string.example_home_components_description,
                        entries = componentEntries,
                        icon = Icons.Filled.Build,
                    ),
                ),
            onOpen = {},
        )
    }
}
