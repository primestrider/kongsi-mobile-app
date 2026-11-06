plugins {
    id("kongsi.android.library")
    id("kongsi.hilt")
}

android {
    namespace = "com.primestrider.kongsi.core.datastore"
}

dependencies {
    api(libs.androidx.datastore.preferences)
    implementation(projects.core.common)
    implementation(projects.core.model)
    testImplementation(projects.core.testing)
}
