plugins {
    id("kotlinstarter.feature.impl")
}

android {
    namespace = "com.primestrider.kotlinstarter.feature.catalog.impl"
}

dependencies {
    api(projects.feature.catalog.api)
    implementation(projects.core.datastore)
}
