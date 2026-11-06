package com.primestrider.kotlinstarter.architecture

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Project skills (`.agents/skills/<name>/SKILL.md`) are found by their frontmatter: `name` must match the folder and
 * `description` must say when to use the skill. AGENTS.md lists every one. Paths inside them are checked by
 * DocsLinksTest.
 */
class SkillsTest {
    private val skills: List<File> by lazy {
        File(repoRoot, ".agents/skills").listFiles { file -> file.isDirectory }.orEmpty().sortedBy { it.name }
    }

    @Test
    fun all_nine_skills_exist() {
        assertTrue("found ${skills.map { it.name }}", skills.size >= 9)
    }

    @Test
    fun every_skill_has_frontmatter_matching_its_folder() {
        val problems =
            skills.mapNotNull { dir ->
                val file = File(dir, "SKILL.md")
                if (!file.isFile) return@mapNotNull "${dir.name}: no SKILL.md"
                val lines = file.readLines()
                val end = lines.drop(1).indexOf("---") + 1
                if (lines.firstOrNull() != "---" || end < 1) return@mapNotNull "${dir.name}: no --- frontmatter block"
                val front = lines.subList(1, end)
                when {
                    "name: ${dir.name}" !in front -> "${dir.name}: frontmatter name is not ${dir.name}"
                    front.none { it.startsWith("description: ") && it.length > "description: ".length + 20 } ->
                        "${dir.name}: description missing or too short"
                    else -> null
                }
            }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun agents_md_lists_every_skill() {
        val agents = File(repoRoot, "AGENTS.md").readText()
        val missing = skills.map { it.name }.filterNot { agents.contains("`$it`") }
        assertTrue("AGENTS.md does not list $missing", missing.isEmpty())
    }
}
