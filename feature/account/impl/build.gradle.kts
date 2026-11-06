plugins {
    id("kotlinstarter.feature.impl")
}

android {
    namespace = "com.primestrider.kotlinstarter.feature.account.impl"
}

dependencies {
    api(projects.feature.account.api)
    implementation(projects.feature.settings.api)
    implementation(projects.core.auth)
    implementation(projects.core.network)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    testImplementation(libs.okhttp.mockwebserver3)
}
