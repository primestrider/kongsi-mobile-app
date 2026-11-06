plugins {
    id("kotlinstarter.android.library")
    id("kotlinstarter.android.compose")
    id("kotlinstarter.hilt")
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.primestrider.kotlinstarter.core.designsystem"
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
