plugins {
    id("kotlinstarter.feature.impl")
}

android {
    namespace = "com.primestrider.kotlinstarter.feature.products.impl"
    sourceSets {
        // Exported Room schemas as debug assets, for MigrationTestHelper (never in release).
        getByName("debug").assets.directories.add("$projectDir/schemas")
    }
}

// Room writes each database version's schema here; commit it.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    api(projects.feature.products.api)
    implementation(projects.feature.catalog.api)
    implementation(projects.core.network)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.compose.adaptive.layout)
    implementation(libs.androidx.compose.adaptive.navigation)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    testImplementation(libs.androidx.paging.testing)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.okhttp.mockwebserver3)
    testImplementation(libs.retrofit.converter.kotlinx.serialization)
}
