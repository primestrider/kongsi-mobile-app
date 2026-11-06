plugins {
    id("kotlinstarter.feature.impl")
}

android {
    namespace = "com.primestrider.kotlinstarter.feature.todos.impl"
    buildFeatures {
        buildConfig = true
    }
    sourceSets {
        // MigrationTestHelper reads the exported schemas as assets; Robolectric only sees the variant's merged
        // assets, so they are added to debug (never release).
        getByName("debug").assets.directories.add("$projectDir/schemas")
    }
}

// Room writes each database version's schema here; commit it so migrations can be tested against it.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    api(projects.feature.todos.api)
    implementation(projects.feature.catalog.api)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.glance.appwidget.testing)
}
