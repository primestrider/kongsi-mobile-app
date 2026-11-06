plugins {
    id("kongsi.android.library")
    id("kongsi.android.compose")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.primestrider.kongsi.core.testing"
}

dependencies {
    api(libs.androidx.datastore.preferences)
    api(libs.kotlinx.coroutines.test)
    implementation(projects.core.network)
    implementation(projects.core.auth)
}
