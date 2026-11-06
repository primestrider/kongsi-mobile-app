package com.primestrider.kotlinstarter.feature.example.sdk.browser

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.annotation.ColorInt
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri

/** Other apps the demo hands a URI to. */
enum class ExternalApp(
    val uri: String,
) {
    WHATSAPP("https://wa.me/6281234567890?text=Halo"),
    CALL("tel:+6281234567890"),
    EMAIL("mailto:support@example.com?subject=Help"),
    MAPS("geo:0,0?q=Monas%20Jakarta"),
}

/** Opens [app] in whichever app handles its URI. Returns false when no installed app can. */
fun openExternal(
    context: Context,
    app: ExternalApp,
): Boolean = startView(context, Intent(Intent.ACTION_VIEW, app.uri.toUri()))

/** Toolbar and navigation bar colors of the Custom Tab, from the app theme. */
data class TabColors(
    @ColorInt val toolbar: Int,
    @ColorInt val navigationBar: Int,
)

/**
 * Opens [url] in a Custom Tab themed with [colors]. With no Custom Tabs browser the tab intent still resolves
 * to a plain browser; if nothing at all can open it, returns false.
 */
fun openCustomTab(
    context: Context,
    url: String,
    colors: TabColors,
): Boolean {
    val tab =
        CustomTabsIntent
            .Builder()
            .setDefaultColorSchemeParams(
                CustomTabColorSchemeParams
                    .Builder()
                    .setToolbarColor(colors.toolbar)
                    .setNavigationBarColor(colors.navigationBar)
                    .build(),
            ).setShowTitle(true)
            .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
            .build()
    tab.intent.data = url.toUri()
    return startView(context, tab.intent) || startView(context, Intent(Intent.ACTION_VIEW, url.toUri()))
}

/** The URL of the Payment Code deep link for [scheme] (the app scheme from build config). */
fun paymentDeepLink(scheme: String): String = "$scheme://example/sdk/payment-code?amount=50000"

private fun startView(
    context: Context,
    intent: Intent,
): Boolean {
    if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
