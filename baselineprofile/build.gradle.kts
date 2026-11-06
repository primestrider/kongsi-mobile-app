plugins {
    id("kotlinstarter.android.test")
    alias(libs.plugins.baselineprofile)
}

// Generates :app's baseline profile and measures startup. Needs a connected device or emulator (API 28+):
//   ./gradlew :app:generateProdReleaseBaselineProfile     writes app/src/prodRelease/generated/baselineProfiles/
//   ./gradlew :baselineprofile:connectedProdBenchmarkReleaseAndroidTest   runs StartupBenchmark
android {
    namespace = "com.primestrider.kotlinstarter.baselineprofile"
    defaultConfig {
        // Macrobenchmark and baseline profile generation need API 28+.
        minSdk = 28
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Mirrors :app's flavours, so each app variant has a matching generator.
    flavorDimensions += "environment"
    productFlavors {
        create("dev") { dimension = "environment" }
        create("prod") { dimension = "environment" }
    }

    targetProjectPath = ":app"
}

baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
