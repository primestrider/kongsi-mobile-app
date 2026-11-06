plugins {
    id("kotlinstarter.android.library")
    id("kotlinstarter.android.compose")
    id("kotlinstarter.hilt")
}

android {
    namespace = "com.primestrider.kotlinstarter.core.common"
}

dependencies {
    implementation(libs.androidx.appcompat)
    testImplementation(projects.core.testing)
}
