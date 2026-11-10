plugins {
    id("kongsi.feature.impl")
}

android {
    namespace = "com.primestrider.kongsi.feature.bill.impl"
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
    api(projects.feature.bill.api)
    // The user's own number (Profile) for "Use my number" on e-wallet accounts.
    implementation(projects.core.datastore)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    // Reading receipt photos on the device (ReceiptScanner).
    implementation(libs.mlkit.text.recognition)
    // Taking the receipt photo inside the app (ReceiptCamera).
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    testImplementation(libs.androidx.room.testing)
}
