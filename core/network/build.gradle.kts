plugins {
    id("kongsi.android.library")
    id("kongsi.android.compose")
    id("kongsi.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.primestrider.kongsi.core.network"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    api(libs.retrofit)
    api(libs.okhttp)
    api(libs.kotlinx.serialization.json)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp.logging.interceptor)
    testImplementation(libs.okhttp.mockwebserver3)
    testImplementation(libs.okhttp.tls)
    testImplementation(projects.core.testing)
}
