import com.primestrider.kotlinstarter.buildlogic.lib
import com.primestrider.kotlinstarter.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** A feature's public contract (`@Serializable` routes): an Android library with kotlinx serialization only. */
class FeatureApiConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("kotlinstarter.android.library")
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
            dependencies {
                add("api", libs.lib("kotlinx-serialization-json"))
            }
        }
}

/**
 * A feature's implementation: Compose, Hilt, navigation and the core modules every screen uses. May depend on
 * `core:*` and other features' `:api`, never on another `:impl` (only `:app` sees `:impl` modules).
 */
class FeatureImplConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("kotlinstarter.android.library")
            pluginManager.apply("kotlinstarter.android.compose")
            pluginManager.apply("kotlinstarter.hilt")
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
            dependencies {
                add("implementation", project(":core:designsystem"))
                add("implementation", project(":core:ui"))
                add("implementation", project(":core:common"))
                add("implementation", libs.lib("androidx-navigation-compose"))
                add("implementation", libs.lib("hilt-navigation-compose"))
                add("implementation", libs.lib("androidx-lifecycle-runtime-compose"))
                add("implementation", libs.lib("androidx-lifecycle-viewmodel-compose"))
                add("testImplementation", project(":core:testing"))
            }
        }
}
