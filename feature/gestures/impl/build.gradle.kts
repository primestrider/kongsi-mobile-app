plugins {
    id("kotlinstarter.feature.impl")
}

android {
    namespace = "com.primestrider.kotlinstarter.feature.gestures.impl"
}

dependencies {
    api(projects.feature.gestures.api)
    implementation(projects.feature.catalog.api)
}
