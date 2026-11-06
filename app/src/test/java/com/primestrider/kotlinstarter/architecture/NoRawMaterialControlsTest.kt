package com.primestrider.kotlinstarter.architecture

import org.junit.Assert.assertTrue
import org.junit.Test

/** Form controls come from core:designsystem's component package; no module uses the stock Material ones. */
class NoRawMaterialControlsTest {
    private val controls =
        "Checkbox|TriStateCheckbox|RadioButton|Switch|Slider|RangeSlider|SegmentedButton|" +
            "SingleChoiceSegmentedButtonRow|MultiChoiceSegmentedButtonRow|TextField|OutlinedTextField"

    /** A single import, a wildcard import, or a fully-qualified call of a stock control. */
    private val rawControl =
        Regex("""import androidx\.compose\.material3\.(?:($controls)\b|\*)|androidx\.compose\.material3\.($controls)\s*\(""")

    private fun scannedModules() = moduleDirs().filter { it.resolve("src/main/java").isDirectory }

    // :app and the 8 core modules always exist (also after removing the examples).
    @Test
    fun scans_every_module() {
        assertTrue("scanned ${scannedModules().map { it.name }}", scannedModules().size >= 9)
    }

    @Test
    fun flags_wildcard_and_qualified_use() {
        assertTrue(rawControl.containsMatchIn("import androidx.compose.material3.*"))
        assertTrue(rawControl.containsMatchIn("androidx.compose.material3.Switch(checked = true)"))
        assertTrue(rawControl.containsMatchIn("import androidx.compose.material3.Checkbox"))
        assertTrue(!rawControl.containsMatchIn("import androidx.compose.material3.Text"))
    }

    @Test
    fun feature_code_uses_design_system_controls_only() {
        val offenders =
            scannedModules()
                .flatMap {
                    it
                        .resolve("src/main/java")
                        .walkTopDown()
                        .filter { f -> f.isFile && f.extension == "kt" }
                        .toList()
                }.filterNot { it.invariantSeparatorsPath.contains("core/designsystem/component/") }
                .filter { rawControl.containsMatchIn(it.readText()) }
                .map { it.relativeTo(repoRoot).invariantSeparatorsPath }
        assertTrue("Use the design-system controls instead of Material ones in:\n${offenders.joinToString("\n")}", offenders.isEmpty())
    }
}
