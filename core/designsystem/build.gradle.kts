plugins {
    id("kongsi.android.library")
    id("kongsi.android.compose")
    id("kongsi.hilt")
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.primestrider.kongsi.core.designsystem"
}

dependencies {
    api(projects.core.model)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    testImplementation(projects.core.testing)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
}

// Reference images: recorded only by the manual CI job on Linux (fonts render differently on Windows), committed here.
roborazzi {
    outputDir.set(file("src/test/screenshots"))
}
