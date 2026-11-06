plugins {
    id("kotlinstarter.android.library")
    id("kotlinstarter.android.compose")
    id("kotlinstarter.hilt")
}

android {
    namespace = "com.primestrider.kotlinstarter.core.ui"
}

dependencies {
    api(projects.core.designsystem)
    implementation(projects.core.common)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    testImplementation(projects.core.testing)
}
