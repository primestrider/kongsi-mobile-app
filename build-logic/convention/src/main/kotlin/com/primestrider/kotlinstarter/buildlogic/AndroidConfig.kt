package com.primestrider.kotlinstarter.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/** SDK levels and JVM target shared by every Android module. */
object AndroidConfig {
    const val COMPILE_SDK = 37
    const val MIN_SDK = 24
    const val TARGET_SDK = 37
    val JAVA_VERSION = JavaVersion.VERSION_17
}

val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun VersionCatalog.lib(alias: String) = findLibrary(alias).get()

fun Project.configureAndroidApplication(extension: ApplicationExtension) {
    extension.apply {
        compileSdk = AndroidConfig.COMPILE_SDK
        defaultConfig {
            minSdk = AndroidConfig.MIN_SDK
            targetSdk = AndroidConfig.TARGET_SDK
        }
        compileOptions {
            sourceCompatibility = AndroidConfig.JAVA_VERSION
            targetCompatibility = AndroidConfig.JAVA_VERSION
        }
        testOptions.unitTests.robolectric()
        lint {
            // New findings fail the build; existing ones are listed in each module's lint-baseline.xml.
            abortOnError = true
            warningsAsErrors = false
            baseline = file("lint-baseline.xml")
        }
    }
    configureKotlinAndTests()
}

fun Project.configureAndroidLibrary(extension: LibraryExtension) {
    extension.apply {
        compileSdk = AndroidConfig.COMPILE_SDK
        defaultConfig {
            minSdk = AndroidConfig.MIN_SDK
        }
        compileOptions {
            sourceCompatibility = AndroidConfig.JAVA_VERSION
            targetCompatibility = AndroidConfig.JAVA_VERSION
        }
        testOptions.unitTests.robolectric()
        lint {
            // New findings fail the build; existing ones are listed in each module's lint-baseline.xml.
            abortOnError = true
            warningsAsErrors = false
            baseline = file("lint-baseline.xml")
        }
    }
    configureKotlinAndTests()
}

/** Robolectric needs merged resources, and its SDK 36 shadows reach into JDK internals. */
private fun com.android.build.api.dsl.UnitTestOptions.robolectric() {
    isIncludeAndroidResources = true
    all { it.jvmArgs("--add-opens=java.base/jdk.internal.access=ALL-UNNAMED") }
}

private fun Project.configureKotlinAndTests() {
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
    }
    pluginManager.apply("org.jlleitschuh.gradle.ktlint")
    dependencies {
        add("testImplementation", libs.lib("junit"))
        add("testImplementation", libs.lib("robolectric"))
        add("testImplementation", libs.lib("androidx-test-core"))
        add("testImplementation", libs.lib("androidx-test-ext-junit"))
        add("testImplementation", libs.lib("kotlinx-coroutines-test"))
        add("testImplementation", libs.lib("turbine"))
    }
}

/** `local.properties` [key], else environment variable [env], else [default]; blank values count as absent. */
fun Project.localOrEnv(
    key: String,
    env: String,
    default: String,
): String {
    val local =
        java.util.Properties().apply {
            rootProject.file("local.properties").takeIf { it.exists() }?.reader()?.use { load(it) }
        }
    return local.getProperty(key)?.takeIf { it.isNotBlank() }
        ?: providers.environmentVariable(env).orNull?.takeIf { it.isNotBlank() }
        ?: default
}

/** Deep link scheme (`<scheme>://example/...`): `app.scheme`, else APP_SCHEME, else "kotlinstarter". */
val Project.appScheme: String get() = localOrEnv("app.scheme", "APP_SCHEME", "kotlinstarter")

/** Escapes [value] as a Java string literal for `buildConfigField`. */
fun javaString(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
