import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "com.primestrider.kongsi.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    testImplementation(libs.junit)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "kongsi.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "kongsi.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidTest") {
            id = "kongsi.android.test"
            implementationClass = "AndroidTestConventionPlugin"
        }
        register("androidCompose") {
            id = "kongsi.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("featureApi") {
            id = "kongsi.feature.api"
            implementationClass = "FeatureApiConventionPlugin"
        }
        register("featureImpl") {
            id = "kongsi.feature.impl"
            implementationClass = "FeatureImplConventionPlugin"
        }
        register("hilt") {
            id = "kongsi.hilt"
            implementationClass = "HiltConventionPlugin"
        }
    }
}
