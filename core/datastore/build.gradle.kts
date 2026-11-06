plugins {
    id("kotlinstarter.android.library")
    id("kotlinstarter.hilt")
}

android {
    namespace = "com.primestrider.kotlinstarter.core.datastore"
}

dependencies {
    api(libs.androidx.datastore.preferences)
    implementation(projects.core.common)
    implementation(projects.core.model)
    testImplementation(projects.core.testing)
}
