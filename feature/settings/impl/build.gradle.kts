plugins {
    id("kotlinstarter.feature.impl")
}

android {
    namespace = "com.primestrider.kotlinstarter.feature.settings.impl"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    api(projects.feature.settings.api)
    implementation(projects.core.datastore)
    testImplementation(libs.androidx.navigation.testing)
}
