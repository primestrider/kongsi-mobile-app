package com.primestrider.kotlinstarter.architecture

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** Every module carries its strings in values/ (English) and values-in/ (Indonesian) with the same keys. */
class TranslationCompletenessTest {
    private val resDirs = moduleDirs().map { File(it, "src/main/res") }.filter { File(it, "values/strings.xml").isFile }
    private val placeholder = Regex("""%(\d+\$)?[sdf]""")

    private fun load(
        resDir: File,
        path: String,
    ): Map<String, String> {
        val file = File(resDir, path)
        if (!file.isFile) return emptyMap()
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = doc.getElementsByTagName("string")
        return (0 until nodes.length)
            .map { nodes.item(it) }
            .filter { it.attributes.getNamedItem("translatable")?.nodeValue != "false" }
            .associate { it.attributes.getNamedItem("name").nodeValue to it.textContent }
    }

    /** Plural resources by name, to the text of their items, so a plurals entry cannot be missing in one language. */
    private fun loadPlurals(
        resDir: File,
        path: String,
    ): Map<String, List<String>> {
        val file = File(resDir, path)
        if (!file.isFile) return emptyMap()
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = doc.getElementsByTagName("plurals")
        return (0 until nodes.length).associate { index ->
            val node = nodes.item(index)
            val items = node.childNodes
            node.attributes.getNamedItem("name").nodeValue to
                (0 until items.length).map { items.item(it) }.filter { it.nodeName == "item" }.map { it.textContent }
        }
    }

    /** English and Indonesian strings of every module, merged; names are unique across modules (see below). */
    private val en get() = resDirs.map { load(it, "values/strings.xml") }.fold(emptyMap<String, String>()) { a, b -> a + b }
    private val id get() = resDirs.map { load(it, "values-in/strings.xml") }.fold(emptyMap<String, String>()) { a, b -> a + b }

    @Test
    fun scans_every_module_with_strings() {
        assertTrue("found ${resDirs.map { it.parentFile.parentFile.name }}", resDirs.size >= 2)
    }

    /** Resource merging silently lets one module override another's string of the same name. */
    @Test
    fun string_names_are_unique_across_modules() {
        val owners =
            resDirs.flatMap { dir ->
                (load(dir, "values/strings.xml").keys + loadPlurals(dir, "values/strings.xml").keys).map { it to dir.parentFile.parentFile }
            }
        val duplicates = owners.groupBy({ it.first }, { it.second.relativeTo(repoRoot).path }).filterValues { it.size > 1 }
        assertTrue("Defined in more than one module: $duplicates", duplicates.isEmpty())
    }

    @Test
    fun keySetsAreEqual() {
        val en = en
        val id = id
        val missingInId = en.keys - id.keys
        val extraInId = id.keys - en.keys
        assertTrue(
            "Missing in values-in: $missingInId; not in values: $extraInId",
            missingInId.isEmpty() && extraInId.isEmpty(),
        )
    }

    @Test
    fun placeholdersMatch() {
        val en = en
        val id = id
        val bad =
            en.keys.intersect(id.keys).filter { key ->
                placeholders(en.getValue(key)) != placeholders(id.getValue(key))
            }
        assertTrue("Placeholder mismatch for keys: $bad", bad.isEmpty())
    }

    @Test
    fun pluralsKeySetsAndPlaceholdersMatch() {
        val en = resDirs.map { loadPlurals(it, "values/strings.xml") }.fold(emptyMap<String, List<String>>()) { a, b -> a + b }
        val id = resDirs.map { loadPlurals(it, "values-in/strings.xml") }.fold(emptyMap<String, List<String>>()) { a, b -> a + b }
        val bad =
            en.keys.intersect(id.keys).filter { key ->
                en.getValue(key).flatMap(::placeholders).toSet() != id.getValue(key).flatMap(::placeholders).toSet()
            }
        assertTrue(
            "Plurals missing in values-in: ${en.keys - id.keys}; not in values: ${id.keys - en.keys}; placeholder mismatch: $bad",
            en.keys == id.keys && bad.isEmpty(),
        )
    }

    private fun placeholders(text: String) =
        placeholder
            .findAll(text)
            .map { it.value }
            .toList()
            .sorted()
}
