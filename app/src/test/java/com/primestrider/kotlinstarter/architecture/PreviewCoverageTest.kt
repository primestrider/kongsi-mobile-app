package com.primestrider.kotlinstarter.architecture

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Every screen and every design-system component file has a Compose preview, so it can be looked at in Android
 * Studio (light and dark, `@ThemePreviews`) without running the app.
 */
class PreviewCoverageTest {
    private val publicScreen = Regex("""^fun <?\w*>? ?\w+Screen\(""", RegexOption.MULTILINE)
    private val publicComposable = Regex("""@Composable\s*\n\s*fun """)

    private fun candidates(): List<File> =
        moduleDirs()
            .flatMap { module ->
                module
                    .resolve("src/main/java")
                    .walkTopDown()
                    .filter { it.isFile && it.extension == "kt" }
                    .toList()
            }.filter { file ->
                val text = file.readText()
                val isComponent =
                    file.invariantSeparatorsPath.contains("core/designsystem/component/") && publicComposable.containsMatchIn(text)
                isComponent || publicScreen.containsMatchIn(text)
            }

    @Test
    fun every_screen_and_component_has_a_preview() {
        val files = candidates()
        // Sanity check that the scan found the sources: 21 files remain after scripts/remove-examples.sh.
        assertTrue("scanned only ${files.size} files", files.size > 15)
        val missing =
            files
                .filterNot { it.readText().let { text -> text.contains("@ThemePreviews") || text.contains("@Preview") } }
                .map { it.relativeTo(repoRoot).invariantSeparatorsPath }
        assertTrue("No @ThemePreviews in:\n${missing.joinToString("\n")}", missing.isEmpty())
    }
}
