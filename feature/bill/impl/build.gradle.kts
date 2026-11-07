plugins {
    id("kongsi.feature.impl")
}

android {
    namespace = "com.primestrider.kongsi.feature.bill.impl"
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
}
