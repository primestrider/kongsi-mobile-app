package com.primestrider.kotlinstarter.architecture

import java.io.File

/** Gradle runs `:app` unit tests with `app/` as the working directory, so the repository root is its parent. */
val repoRoot: File = File("..").canonicalFile

/** Every Gradle module directory (one with a build file and a `src/main`), excluding the root and build-logic. */
fun moduleDirs(): List<File> =
    repoRoot
        .walkTopDown()
        .onEnter { it.name != "build" && it.name != "build-logic" && !it.name.startsWith(".") }
        .filter { it.isDirectory && File(it, "build.gradle.kts").isFile && File(it, "src/main").isDirectory && it != repoRoot }
        .toList()
