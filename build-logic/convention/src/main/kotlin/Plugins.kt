import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.TestExtension
import com.primestrider.kotlinstarter.buildlogic.AndroidConfig
import com.primestrider.kotlinstarter.buildlogic.configureAndroidApplication
import com.primestrider.kotlinstarter.buildlogic.configureAndroidLibrary
import com.primestrider.kotlinstarter.buildlogic.lib
import com.primestrider.kotlinstarter.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** `com.android.application` with the shared SDK levels, JVM 17, Robolectric test setup and ktlint. */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("com.android.application")
            extensions.configure<ApplicationExtension> { configureAndroidApplication(this) }
        }
}

/** `com.android.library` with the shared SDK levels, JVM 17, Robolectric test setup and ktlint. */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("com.android.library")
            extensions.configure<LibraryExtension> { configureAndroidLibrary(this) }
        }
}

/** Compose compiler, the Compose BOM, Material 3 and Compose UI test dependencies. Apply after an Android plugin. */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            pluginManager.withPlugin("com.android.application") {
                extensions.configure<ApplicationExtension> { buildFeatures.compose = true }
            }
            pluginManager.withPlugin("com.android.library") {
                extensions.configure<LibraryExtension> { buildFeatures.compose = true }
            }
            dependencies {
                val bom = platform(libs.lib("androidx-compose-bom"))
                add("implementation", bom)
                add("implementation", libs.lib("androidx-compose-ui"))
                add("implementation", libs.lib("androidx-compose-ui-tooling-preview"))
                add("implementation", libs.lib("androidx-compose-material3"))
                add("implementation", libs.lib("androidx-compose-material-icons-core"))
                add("debugImplementation", libs.lib("androidx-compose-ui-tooling"))
                add("debugImplementation", libs.lib("androidx-compose-ui-test-manifest"))
                add("testImplementation", bom)
                add("testImplementation", libs.lib("androidx-compose-ui-test-junit4"))
            }
        }
}

/** Hilt with KSP, plus Hilt testing for Robolectric tests. */
class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("com.google.devtools.ksp")
            pluginManager.apply("com.google.dagger.hilt.android")
            dependencies {
                add("implementation", libs.lib("hilt-android"))
                add("ksp", libs.lib("hilt-compiler"))
                add("testImplementation", libs.lib("hilt-android-testing"))
                add("kspTest", libs.lib("hilt-compiler"))
            }
        }
}

/** `com.android.test` (instrumented test-only modules such as :baselineprofile) with the shared SDK levels and JVM 17. */
class AndroidTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("com.android.test")
            pluginManager.apply("org.jlleitschuh.gradle.ktlint")
            extensions.configure<TestExtension> {
                compileSdk = AndroidConfig.COMPILE_SDK
                defaultConfig.targetSdk = AndroidConfig.TARGET_SDK
                compileOptions {
                    sourceCompatibility = AndroidConfig.JAVA_VERSION
                    targetCompatibility = AndroidConfig.JAVA_VERSION
                }
            }
        }
}
