import com.primestrider.kotlinstarter.buildlogic.localOrEnv

plugins {
    id("kotlinstarter.feature.impl")
}

// Google Cloud project number for the Play Integrity demo; 0 (the default) shows "not configured".
val playIntegrityProject: Long =
    localOrEnv("play.integrity.project", "PLAY_INTEGRITY_PROJECT", "0").let {
        it.toLongOrNull() ?: error("play.integrity.project / PLAY_INTEGRITY_PROJECT must be a number, was '$it'")
    }

android {
    namespace = "com.primestrider.kotlinstarter.feature.devices.impl"
    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("long", "PLAY_INTEGRITY_PROJECT", "${playIntegrityProject}L")
    }
    buildFeatures {
        buildConfig = true
    }
    sourceSets {
        // MigrationTestHelper-style tests read the exported schemas as assets (debug only).
        getByName("debug").assets.directories.add("$projectDir/schemas")
    }
}

// Room writes each database version's schema here; commit it so migrations can be tested against it.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    api(projects.feature.devices.api)
    implementation(projects.feature.catalog.api)
    implementation(projects.core.network)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.browser)
    implementation(libs.play.review.ktx)
    implementation(libs.play.app.update.ktx)
    implementation(libs.play.integrity)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.play.services.location)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.biometric)
    implementation(libs.zxing.core)
    implementation(libs.coil.compose)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.okhttp.mockwebserver3)
}
