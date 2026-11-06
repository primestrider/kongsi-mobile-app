plugins {
    id("kotlinstarter.android.library")
    id("kotlinstarter.android.compose")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.primestrider.kotlinstarter.core.testing"
}

dependencies {
    api(libs.androidx.datastore.preferences)
    api(libs.kotlinx.coroutines.test)
    implementation(projects.core.network)
    implementation(projects.core.auth)
}
