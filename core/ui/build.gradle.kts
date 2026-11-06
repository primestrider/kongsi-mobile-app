plugins {
    id("kongsi.android.library")
    id("kongsi.android.compose")
    id("kongsi.hilt")
}

android {
    namespace = "com.primestrider.kongsi.core.ui"
}

dependencies {
    api(projects.core.designsystem)
    implementation(projects.core.common)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    testImplementation(projects.core.testing)
}
