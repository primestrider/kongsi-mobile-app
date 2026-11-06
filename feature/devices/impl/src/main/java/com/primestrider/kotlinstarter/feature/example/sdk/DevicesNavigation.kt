package com.primestrider.kotlinstarter.feature.example.sdk

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.primestrider.kotlinstarter.feature.catalog.api.CatalogEntry
import com.primestrider.kotlinstarter.feature.catalog.api.CatalogSection
import com.primestrider.kotlinstarter.feature.catalog.api.CatalogSections
import com.primestrider.kotlinstarter.feature.devices.impl.R
import com.primestrider.kotlinstarter.feature.example.SdkDemoRoute
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import dagger.multibindings.IntoSet

/**
 * Every Device APIs demo, reachable from Home and from `<appScheme>://example/sdk/...` deep links. [appScheme] is the
 * app's flavour scheme (`BuildConfig.APP_SCHEME` of `:app`).
 */
fun NavGraphBuilder.devicesDestinations(
    navController: NavController,
    appScheme: String,
) {
    composable<SdkDemoRoute>(
        deepLinks = listOf(navDeepLink<SdkDemoRoute>(basePath = "$appScheme://example/sdk")),
    ) { entry -> SdkDemoScreen(id = entry.toRoute<SdkDemoRoute>().id, onBack = { navController.popBackStack() }) }
}

/** The Device APIs section on Home and one row per demo. */
@Module
@InstallIn(SingletonComponent::class)
object DevicesCatalogModule {
    @Provides
    @IntoSet
    fun section(): CatalogSection =
        CatalogSection(
            CatalogSections.DEVICES,
            R.string.example_home_sdk_title,
            R.string.example_home_sdk_description,
            Icons.Filled.Phone,
            order = 20,
        )

    @Provides
    @ElementsIntoSet
    fun entries(): Set<CatalogEntry> = sdkEntries().toSet()
}
