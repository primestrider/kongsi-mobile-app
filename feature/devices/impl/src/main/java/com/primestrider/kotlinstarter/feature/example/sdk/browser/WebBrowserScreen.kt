package com.primestrider.kotlinstarter.feature.example.sdk.browser

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.primestrider.kotlinstarter.core.common.AppConfig
import com.primestrider.kotlinstarter.core.designsystem.component.AppButton
import com.primestrider.kotlinstarter.core.designsystem.component.Banner
import com.primestrider.kotlinstarter.core.designsystem.component.BannerVariant
import com.primestrider.kotlinstarter.core.designsystem.component.ButtonVariant
import com.primestrider.kotlinstarter.core.designsystem.component.InfoCard
import com.primestrider.kotlinstarter.core.designsystem.component.SelectField
import com.primestrider.kotlinstarter.core.designsystem.component.SelectOption
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.feature.devices.impl.R
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold
import com.primestrider.kotlinstarter.feature.example.sdk.SdkSection

private enum class Page(
    @StringRes val label: Int,
    val url: String,
) {
    TERMS(R.string.sdk_browser_terms, "https://expo.dev/terms"),
    PRIVACY(R.string.sdk_browser_privacy, "https://expo.dev/privacy"),
    HELP(R.string.sdk_browser_help, "https://docs.expo.dev"),
}

private val ExternalApp.label: Int
    @StringRes get() =
        when (this) {
            ExternalApp.WHATSAPP -> R.string.sdk_browser_whatsapp
            ExternalApp.CALL -> R.string.sdk_browser_call
            ExternalApp.EMAIL -> R.string.sdk_browser_email
            ExternalApp.MAPS -> R.string.sdk_browser_maps
        }

/** Custom Tabs themed with the app colors, other apps by URI, and the deep link that comes back into this app. */
@Composable
fun WebBrowserScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    var page by rememberSaveable { mutableStateOf(Page.TERMS) }
    var app by rememberSaveable { mutableStateOf(ExternalApp.WHATSAPP) }
    var tabFailed by rememberSaveable { mutableStateOf(false) }
    var failedApp by rememberSaveable { mutableStateOf<ExternalApp?>(null) }

    ExamplePageScaffold(title = R.string.sdk_browser_title, subtitle = R.string.sdk_browser_subtitle, onBack = onBack) {
        SdkSection(R.string.sdk_browser_tab_title, R.string.sdk_browser_tab_description) {
            SelectField(
                label = stringResource(R.string.sdk_browser_page),
                options = Page.entries.map { SelectOption(it, stringResource(it.label)) },
                selected = page,
                onSelect = { page = it },
            )
            AppButton(
                stringResource(R.string.sdk_browser_open_page),
                onClick = {
                    tabFailed = !openCustomTab(context, page.url, TabColors(colors.primary.toArgb(), colors.surface.toArgb()))
                },
            )
            if (tabFailed) Banner(stringResource(R.string.sdk_browser_no_browser), variant = BannerVariant.WARNING)
        }
        SdkSection(R.string.sdk_browser_apps_title, R.string.sdk_browser_apps_description) {
            SelectField(
                label = stringResource(R.string.sdk_browser_app),
                options = ExternalApp.entries.map { SelectOption(it, stringResource(it.label)) },
                selected = app,
                onSelect = {
                    app = it
                    failedApp = null
                },
            )
            AppButton(
                stringResource(R.string.sdk_browser_open),
                onClick = { failedApp = app.takeUnless { openExternal(context, it) } },
                variant = ButtonVariant.OUTLINED,
            )
            failedApp?.let {
                Banner(stringResource(R.string.sdk_browser_no_app, stringResource(it.label)), variant = BannerVariant.WARNING)
            }
        }
        SdkSection(R.string.sdk_browser_link_title, R.string.sdk_browser_link_description) {
            InfoCard(listOf(stringResource(R.string.sdk_browser_url) to paymentDeepLink(AppConfig.appScheme(context))))
        }
    }
}

@ThemePreviews
@Composable
private fun WebBrowserScreenPreview() {
    PreviewTheme {
        WebBrowserScreen(onBack = {})
    }
}
