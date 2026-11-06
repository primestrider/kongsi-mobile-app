plugins {
    id("kotlinstarter.android.library")
    id("kotlinstarter.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.primestrider.kotlinstarter.core.auth"
}

dependencies {
    api(projects.core.network)
    implementation(projects.core.common)
    implementation(projects.core.datastore)
    implementation(projects.core.designsystem)
    testImplementation(projects.core.testing)
    testImplementation(libs.okhttp.mockwebserver3)
    testImplementation(libs.retrofit.converter.kotlinx.serialization)
}
