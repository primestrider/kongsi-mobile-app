package com.primestrider.kotlinstarter.feature.example

/**
 * The Home list after the search box and the section chips: [selectedTitle] is the string-resource id of the
 * chosen section's title (null, or an id no section has, = all), and [query] matches an entry's title or subtitle
 * anywhere, or the start of a word in its section's title (which keeps the whole section, and keeps one letter
 * from matching every section). [text] resolves string resources, so matching follows the app language.
 * Sections left without entries are dropped.
 */
fun filterSections(
    sections: List<ExampleSection>,
    query: String,
    selectedTitle: Int?,
    text: (Int) -> String,
): List<ExampleSection> {
    val scoped = sections.firstOrNull { it.title == selectedTitle }?.let(::listOf) ?: sections
    val needle = query.trim()
    if (needle.isEmpty()) return scoped

    fun contains(res: Int) = text(res).contains(needle, ignoreCase = true)

    fun startsAWord(res: Int): Boolean {
        val title = text(res)
        return title.indices.any { (it == 0 || title[it - 1] == ' ') && title.startsWith(needle, it, ignoreCase = true) }
    }

    return scoped.mapNotNull { section ->
        if (startsAWord(section.title)) {
            section
        } else {
            section.entries
                .filter { contains(it.title) || contains(it.subtitle) }
                .takeIf { it.isNotEmpty() }
                ?.let { section.copy(entries = it) }
        }
    }
}
