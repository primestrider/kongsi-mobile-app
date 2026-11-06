plugins {
    id("kotlinstarter.feature.api")
}

android {
    namespace = "com.primestrider.kotlinstarter.feature.catalog.api"
}

dependencies {
    // CatalogSection carries its icon as an ImageVector.
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
}
