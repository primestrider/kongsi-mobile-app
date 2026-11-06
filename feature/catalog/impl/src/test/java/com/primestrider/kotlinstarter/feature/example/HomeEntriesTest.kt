package com.primestrider.kotlinstarter.feature.example

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import com.primestrider.kotlinstarter.feature.catalog.api.CatalogEntry
import com.primestrider.kotlinstarter.feature.catalog.api.CatalogSection
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeEntriesTest {
    private data object A

    private data object B

    private data object C

    @Test
    fun home_lists_contributed_entries_by_section_order() {
        val sections =
            setOf(
                CatalogSection(id = "late", title = 2, description = 20, icon = Icons.Filled.Star, order = 20),
                CatalogSection(id = "early", title = 1, description = 10, icon = Icons.Filled.Star, order = 10),
                CatalogSection(id = "empty", title = 3, description = 30, icon = Icons.Filled.Star, order = 30),
            )
        val entries =
            setOf(
                CatalogEntry(title = 102, subtitle = 0, route = B, sectionId = "late", order = 2),
                CatalogEntry(title = 101, subtitle = 0, route = A, sectionId = "late", order = 1),
                CatalogEntry(title = 201, subtitle = 0, route = C, sectionId = "early", order = 1),
                CatalogEntry(title = 999, subtitle = 0, route = C, sectionId = "missing", order = 1),
            )
        val home = buildSections(sections, entries)
        // Sections by order, entries by order inside; a section without entries and an entry without a section are dropped.
        assertEquals(listOf(1, 2), home.map { it.title })
        assertEquals(listOf(listOf(201), listOf(101, 102)), home.map { s -> s.entries.map { it.title } })
    }
}
