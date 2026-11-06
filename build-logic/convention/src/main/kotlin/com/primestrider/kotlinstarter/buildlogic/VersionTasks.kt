package com.primestrider.kotlinstarter.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

/** Raises the `version=` line of `gradle.properties`: `./gradlew bumpVersion -Pbump=patch|minor|major`. */
@UntrackedTask(because = "It edits gradle.properties in place")
abstract class BumpVersionTask : DefaultTask() {
    @get:Internal
    abstract val properties: RegularFileProperty

    @get:Input
    abstract val bump: Property<String>

    @TaskAction
    fun bumpVersion() {
        val file = properties.get().asFile
        val text = file.readText()
        // Keep the file's own line endings, so the diff is the version line only.
        val separator = if (text.contains("\r\n")) "\r\n" else "\n"
        val lines = text.removeSuffix(separator).split(separator)
        val index = lines.indexOfFirst { it.startsWith("version=") }
        require(index >= 0) { "No version= line in ${file.name}" }
        val current = AppVersion.parse(lines[index].substringAfter("="))
        val next = current.bump(Bump.of(bump.get()))
        val updated = lines.toMutableList().apply { set(index, "version=$next") }
        file.writeText(updated.joinToString(separator, postfix = separator))
        logger.lifecycle("Version $current -> $next (versionCode ${next.code})")
    }
}

/** Prints the version: `./gradlew -q printVersion`. */
@UntrackedTask(because = "It only prints")
abstract class PrintVersionTask : DefaultTask() {
    @get:Input
    abstract val version: Property<String>

    @TaskAction
    fun print() = println(version.get())
}
