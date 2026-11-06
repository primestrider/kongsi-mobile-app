package com.primestrider.kotlinstarter.architecture

// remove-examples: the docs describe the example features, so this check goes with them; rewrite the docs, then restore it.

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Docs point at real files: every backticked path in README, AGENTS.md, docs/ (not plans/specs) and the project skills
 * (.agents/skills) is a file of the
 * repository, written in full or as its trailing part (docs often give package-relative paths such as
 * `sdk/share/ShareScreen.kt`). Placeholders (`<name>`, `xx`, `*`) are not checked.
 */
class DocsLinksTest {
    private val path = Regex("""`([\w.\-]+(?:/[\w.\-*]+)+\.(?:kt|kts|md|xml|pro|yml|sh))`""")

    private fun docs(): List<File> =
        listOf(File(repoRoot, "README.md"), File(repoRoot, "AGENTS.md")) +
            File(repoRoot, "docs").walkTopDown().filter { it.extension == "md" && !it.invariantSeparatorsPath.contains("/superpowers/") } +
            File(repoRoot, ".agents/skills").walkTopDown().filter { it.extension == "md" }

    /** Every repository file as a `/`-separated path, without build outputs. */
    private val files: List<String> by lazy {
        repoRoot
            .walkTopDown()
            .onEnter { it.name !in setOf("build", ".git", ".gradle", ".kotlin", ".superpowers") }
            .filter { it.isFile }
            .map { it.relativeTo(repoRoot).invariantSeparatorsPath }
            .toList()
    }

    private fun exists(path: String) = files.any { it == path || it.endsWith("/$path") }

    @Test
    fun every_code_path_in_docs_exists() {
        val missing =
            docs().flatMap { doc ->
                path
                    .findAll(doc.readText())
                    .map { it.groupValues[1] }
                    .filterNot { it.contains('*') || it.contains("/xx") || it.contains("-xx/") }
                    .filterNot { exists(it) }
                    .map { "${doc.relativeTo(repoRoot).path}: $it" }
                    .toList()
            }
        assertTrue("Paths in docs that do not exist:\n${missing.joinToString("\n")}", missing.isEmpty())
    }
}
