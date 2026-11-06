plugins {
    id("kotlinstarter.feature.impl")
}

android {
    namespace = "com.primestrider.kotlinstarter.feature.animation.impl"
}

dependencies {
    api(projects.feature.animation.api)
    implementation(projects.feature.catalog.api)
}
