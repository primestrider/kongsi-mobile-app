import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "com.primestrider.kotlinstarter.buildlogic"

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
            id = "kotlinstarter.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "kotlinstarter.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidTest") {
            id = "kotlinstarter.android.test"
            implementationClass = "AndroidTestConventionPlugin"
        }
        register("androidCompose") {
            id = "kotlinstarter.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("featureApi") {
            id = "kotlinstarter.feature.api"
            implementationClass = "FeatureApiConventionPlugin"
        }
        register("featureImpl") {
            id = "kotlinstarter.feature.impl"
            implementationClass = "FeatureImplConventionPlugin"
        }
        register("hilt") {
            id = "kotlinstarter.hilt"
            implementationClass = "HiltConventionPlugin"
        }
    }
}
