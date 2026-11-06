package com.primestrider.kotlinstarter.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Fails when R8 renamed a navigation route. Routes are the `@Serializable` classes in feature `:api` modules: their
 * serial names (the class names) are stored in the saved back stack and matched by deep links, so a minified
 * release must keep them. Reads R8's `mapping.txt`, where a kept class maps to itself (`a.B -> a.B:`).
 */
abstract class VerifyKeptClassesTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val mapping: RegularFileProperty

    /** Kotlin sources of the `:api` modules. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val apiSources: ConfigurableFileCollection

    @TaskAction
    fun verify() {
        val routes =
            apiSources.files
                .filter { it.isFile && it.extension == "kt" }
                .flatMap { file ->
                    val text = file.readText()
                    val pkg = Regex("""^package\s+([\w.]+)""", RegexOption.MULTILINE).find(text)?.groupValues?.get(1) ?: return@flatMap emptyList()
                    Regex("""@Serializable\s+data\s+(?:object|class)\s+(\w+)""").findAll(text).map { "$pkg.${it.groupValues[1]}" }.toList()
                }
        if (routes.isEmpty()) {
            // No feature modules (e.g. after scripts/remove-examples.sh): nothing to keep.
            logger.lifecycle("verifyReleaseKeeps: no feature routes found, nothing to check")
            return
        }
        val classLines =
            mapping.get().asFile.readLines().filter { !it.startsWith(" ") && !it.startsWith("#") && it.endsWith(":") }
        val renamed = classLines.associate { line -> line.substringBefore(" -> ") to line.substringAfter(" -> ").removeSuffix(":") }
        val bad = routes.filter { route -> renamed[route]?.let { it != route } ?: false }
        if (bad.isNotEmpty()) throw GradleException("R8 renamed navigation routes (add keep rules): $bad")
        logger.lifecycle("verifyReleaseKeeps: ${routes.size} routes kept by R8")
    }
}
