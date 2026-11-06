import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.HostTestBuilder
import com.primestrider.kotlinstarter.buildlogic.AppVersion
import com.primestrider.kotlinstarter.buildlogic.BumpVersionTask
import com.primestrider.kotlinstarter.buildlogic.PrintVersionTask
import com.primestrider.kotlinstarter.buildlogic.VerifyKeptClassesTask
import com.primestrider.kotlinstarter.buildlogic.appScheme
import com.primestrider.kotlinstarter.buildlogic.javaString
import com.primestrider.kotlinstarter.buildlogic.localOrEnv

plugins {
    id("kotlinstarter.android.application")
    id("kotlinstarter.android.compose")
    id("kotlinstarter.hilt")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.baselineprofile)
}

/**
 * API base URL per environment. There is no real backend, so both default to DummyJSON; a real app changes the
 * `prod` default. `local.properties` `api.url` or env `API_URL` override either (keep the trailing slash).
 */
fun apiUrl(default: String): String = localOrEnv("api.url", "API_URL", default)

// Certificate pins of the API host (comma-separated sha256/...); empty = no pinning. See docs/architecture/networking.md.
val apiPins: String = localOrEnv("api.pins", "API_PINS", "")

/** Release signing from env (CI) or `local.properties` (never committed); absent values fall back to debug signing. */
val releaseKeystore: String? = localOrEnv("android.keystore.path", "ANDROID_KEYSTORE_PATH", "").takeIf { it.isNotBlank() }

/** One source: `version=` in gradle.properties. versionCode = major * 10000 + minor * 100 + patch. */
val appVersion = AppVersion.parse(providers.gradleProperty("version").get())

android {
    namespace = "com.primestrider.kotlinstarter"

    defaultConfig {
        applicationId = "com.primestrider.kotlinstarter"
        versionCode = appVersion.code
        versionName = appVersion.toString()
    }

    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            // Installs next to prod.
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            // Its own deep-link scheme too, or a link would open prod (or a chooser) when both are installed.
            manifestPlaceholders["appScheme"] = "$appScheme-dev"
            buildConfigField("String", "APP_SCHEME", javaString("$appScheme-dev"))
            buildConfigField("String", "API_URL", javaString(apiUrl("https://dummyjson.com/")))
            buildConfigField("String", "API_PINS", javaString(apiPins))
        }
        create("prod") {
            dimension = "environment"
            manifestPlaceholders["appScheme"] = appScheme
            buildConfigField("String", "APP_SCHEME", javaString(appScheme))
            buildConfigField("String", "API_URL", javaString(apiUrl("https://dummyjson.com/")))
            buildConfigField("String", "API_PINS", javaString(apiPins))
        }
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = localOrEnv("android.keystore.password", "ANDROID_KEYSTORE_PASSWORD", "")
                keyAlias = localOrEnv("android.key.alias", "ANDROID_KEY_ALIAS", "")
                keyPassword = localOrEnv("android.key.password", "ANDROID_KEY_PASSWORD", "")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Without a release keystore (fresh clone, CI without secrets) the release build is debug-signed so it
            // still installs; never ship that build.
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }
    androidResources {
        generateLocaleConfig = true
    }
    buildFeatures {
        buildConfig = true
    }
}

// R8 must keep every navigation route (`@Serializable` classes in the feature `:api` modules) under its own name.
val verifyReleaseKeeps by tasks.registering(VerifyKeptClassesTask::class) {
    apiSources.from(fileTree(rootProject.file("feature")) { include("*/api/src/main/java/**/*.kt") })
}

androidComponents {
    // :app's unit tests do not depend on the environment: run them once, on dev.
    beforeVariants(selector().withFlavor("environment" to "prod")) { variant ->
        variant.hostTests[HostTestBuilder.UNIT_TEST_TYPE]?.enable = false
    }
    onVariants(selector().withName("prodRelease")) { variant ->
        variant.artifacts.get(SingleArtifact.OBFUSCATION_MAPPING_FILE)?.let { mappingFile ->
            verifyReleaseKeeps.configure { mapping.set(mappingFile) }
        }
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.model)
    implementation(projects.core.datastore)
    implementation(projects.core.designsystem)
    implementation(projects.core.network)
    implementation(projects.core.auth)
    implementation(projects.core.ui)

    // region example:account
    implementation(projects.feature.account.api)
    implementation(projects.feature.account.impl)
    // endregion
    // region example:animation
    implementation(projects.feature.animation.api)
    implementation(projects.feature.animation.impl)
    // endregion
    // region example:catalog
    implementation(projects.feature.catalog.api)
    implementation(projects.feature.catalog.impl)
    // endregion
    // region example:devices
    implementation(projects.feature.devices.api)
    implementation(projects.feature.devices.impl)
    // endregion
    // region example:gestures
    implementation(projects.feature.gestures.api)
    implementation(projects.feature.gestures.impl)
    // endregion
    // region example:products
    implementation(projects.feature.products.api)
    implementation(projects.feature.products.impl)
    // endregion
    // region example:settings
    implementation(projects.feature.settings.api)
    implementation(projects.feature.settings.impl)
    // endregion
    // region example:todos
    implementation(projects.feature.todos.api)
    implementation(projects.feature.todos.impl)
    // endregion
    testImplementation(projects.core.testing)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.splashscreen)
    // Installs the committed baseline profile (src/main/generated/baselineProfiles) on devices without Play.
    implementation(libs.androidx.profileinstaller)
    baselineProfile(projects.baselineprofile)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.navigation.compose)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    testImplementation(libs.okhttp.mockwebserver3)
    testImplementation(libs.androidx.work.testing)
}

// ./gradlew bumpVersion -Pbump=patch|minor|major rewrites version= in gradle.properties (the release workflow uses it).
tasks.register<BumpVersionTask>("bumpVersion") {
    properties.set(rootProject.layout.projectDirectory.file("gradle.properties"))
    bump.set(providers.gradleProperty("bump").orElse("patch"))
}

// ./gradlew -q printVersion
tasks.register<PrintVersionTask>("printVersion") {
    version.set(appVersion.toString())
}

// The architecture tests (app/src/test/.../architecture) also read files outside this module. Declare them, so editing
// only a doc, a workflow or a skill reruns the tests instead of taking them from the up-to-date check or the cache.
tasks.withType<Test>().configureEach {
    listOf("docs", ".github", ".agents", "scripts").forEach { dir ->
        inputs.dir(rootProject.layout.projectDirectory.dir(dir)).withPathSensitivity(PathSensitivity.RELATIVE).optional()
    }
    inputs.files(rootProject.file("README.md"), rootProject.file("AGENTS.md")).withPathSensitivity(PathSensitivity.RELATIVE)
}
