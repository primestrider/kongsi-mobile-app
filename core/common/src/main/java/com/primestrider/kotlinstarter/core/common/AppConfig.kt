package com.primestrider.kotlinstarter.core.common

import android.content.Context
import android.content.pm.PackageManager

/** Values the app sets per build flavour, readable from any module. */
object AppConfig {
    /** Manifest `<meta-data>` key under which `:app` publishes its deep-link scheme. */
    const val DEEP_LINK_SCHEME_KEY = "com.primestrider.kotlinstarter.DEEP_LINK_SCHEME"

    /** Used when the running app does not publish a scheme (e.g. a library module's own unit tests). */
    const val DEFAULT_SCHEME = "kotlinstarter"

    /**
     * The deep-link scheme of the running app (`<scheme>://example/...`). `:app` sets it per flavour (the dev flavour
     * has its own, so dev and prod can be installed side by side) from the same value as its intent filter.
     */
    fun appScheme(context: Context): String {
        @Suppress("DEPRECATION")
        val info = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        return info.metaData?.getString(DEEP_LINK_SCHEME_KEY)?.takeIf { it.isNotBlank() } ?: DEFAULT_SCHEME
    }
}
