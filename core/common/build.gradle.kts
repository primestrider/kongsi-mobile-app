plugins {
    id("kongsi.android.library")
    id("kongsi.android.compose")
    id("kongsi.hilt")
}

android {
    namespace = "com.primestrider.kongsi.core.common"
}

dependencies {
    implementation(libs.androidx.appcompat)
    testImplementation(projects.core.testing)
}
