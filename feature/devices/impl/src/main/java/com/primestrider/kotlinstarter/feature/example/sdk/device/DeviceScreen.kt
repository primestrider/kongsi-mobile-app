package com.primestrider.kotlinstarter.feature.example.sdk.device

import android.app.ActivityManager
import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.primestrider.kotlinstarter.core.designsystem.component.AppButton
import com.primestrider.kotlinstarter.core.designsystem.component.Banner
import com.primestrider.kotlinstarter.core.designsystem.component.BannerVariant
import com.primestrider.kotlinstarter.core.designsystem.component.ButtonVariant
import com.primestrider.kotlinstarter.core.designsystem.component.InfoCard
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.core.util.currentLocale
import com.primestrider.kotlinstarter.feature.devices.impl.R
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold
import com.primestrider.kotlinstarter.feature.example.sdk.SdkSection
import java.net.Inet4Address
import java.net.InetAddress
import java.util.Locale

/** Live connectivity from a network callback, plus build constants read once. */
@Composable
fun DeviceScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val connectivity = remember(context) { context.connectivityManager() }
    val snapshot by remember(connectivity) { connectivity.snapshots() }
        .collectAsStateWithLifecycle(initialValue = connectivity.currentSnapshot())
    var ip by remember { mutableStateOf<String?>(null) }
    var airplane by remember { mutableStateOf<String?>(null) }
    val on = stringResource(R.string.sdk_device_on)
    val off = stringResource(R.string.sdk_device_off)

    ExamplePageScaffold(
        title = R.string.sdk_device_title,
        subtitle = R.string.sdk_device_subtitle,
        onBack = onBack,
    ) {
        SdkSection(R.string.sdk_device_network_title, R.string.sdk_device_network_description) {
            if (snapshot.isOffline()) {
                Banner(
                    title = stringResource(R.string.sdk_device_offline_title),
                    message = stringResource(R.string.sdk_device_offline_message),
                    variant = BannerVariant.WARNING,
                )
            }
            InfoCard(
                listOf(
                    stringResource(R.string.sdk_device_type) to snapshot.type,
                    stringResource(R.string.sdk_device_connected) to yesNo(context, snapshot.connected),
                    stringResource(R.string.sdk_device_reachable) to yesNo(context, snapshot.validated),
                    stringResource(R.string.sdk_device_ip) to ip,
                    stringResource(R.string.sdk_device_airplane) to airplane,
                ),
            )
            AppButton(
                stringResource(R.string.sdk_device_read_ip),
                onClick = {
                    ip = connectivity.ipAddress()
                    airplane = if (isAirplaneModeOn(context)) on else off
                },
                variant = ButtonVariant.OUTLINED,
            )
        }
        SdkSection(R.string.sdk_device_info_title, R.string.sdk_device_info_description) {
            InfoCard(deviceRows(context, currentLocale()))
        }
    }
}

private fun yesNo(
    context: Context,
    value: Boolean,
): String = context.getString(if (value) R.string.sdk_device_yes else R.string.sdk_device_no)

private fun deviceRows(
    context: Context,
    locale: Locale,
): List<Pair<String, String?>> {
    val memory =
        ActivityManager.MemoryInfo().also {
            (
                context.getSystemService(
                    Context.ACTIVITY_SERVICE,
                ) as ActivityManager
            ).getMemoryInfo(it)
        }
    // MEDIA_PERFORMANCE_CLASS exists from API 31; 0 means the device does not declare one.
    val performanceClass =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.VERSION.MEDIA_PERFORMANCE_CLASS
                .takeIf { it > 0 }
                ?.toString()
        } else {
            null
        }
    return listOf(
        context.getString(R.string.sdk_device_brand) to Build.BRAND,
        context.getString(R.string.sdk_device_model) to Build.MODEL,
        context.getString(R.string.sdk_device_os) to
            context.getString(R.string.sdk_device_os_value, Build.VERSION.RELEASE, Build.VERSION.SDK_INT),
        context.getString(R.string.sdk_device_kind) to deviceKind(context),
        context.getString(R.string.sdk_device_physical) to
            context.getString(if (isEmulator()) R.string.sdk_device_physical_no else R.string.sdk_device_yes),
        context.getString(R.string.sdk_device_memory) to String.format(locale, "%.1f GB", memory.totalMem / BYTES_PER_GB),
        context.getString(R.string.sdk_device_performance_class) to performanceClass,
    )
}

private const val BYTES_PER_GB = 1024.0 * 1024.0 * 1024.0

private fun deviceKind(context: Context): String {
    val uiMode = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    val resource =
        when {
            uiMode.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION -> R.string.sdk_device_kind_tv
            context.resources.configuration.smallestScreenWidthDp >= TABLET_MIN_DP -> R.string.sdk_device_kind_tablet
            else -> R.string.sdk_device_kind_phone
        }
    return context.getString(resource)
}

private const val TABLET_MIN_DP = 600

private fun isEmulator(): Boolean =
    Build.FINGERPRINT.startsWith("generic") ||
        Build.FINGERPRINT.contains("emulator") ||
        Build.HARDWARE.contains("ranchu") ||
        Build.HARDWARE.contains("goldfish") ||
        Build.PRODUCT.contains("sdk_gphone")

private fun isAirplaneModeOn(context: Context): Boolean =
    Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1

/** The active network's first non-loopback address, IPv4 preferred. */
private fun android.net.ConnectivityManager.ipAddress(): String? {
    val addresses: List<InetAddress> =
        getLinkProperties(activeNetwork)
            ?.linkAddresses
            .orEmpty()
            .map {
                it.address
            }.filter { !it.isLoopbackAddress }
    return (addresses.firstOrNull { it is Inet4Address } ?: addresses.firstOrNull())?.hostAddress
}

@ThemePreviews
@Composable
private fun DeviceScreenPreview() {
    PreviewTheme {
        ExamplePageScaffold(title = R.string.sdk_device_title, subtitle = R.string.sdk_device_subtitle, onBack = {}) {
            SdkSection(R.string.sdk_device_info_title, R.string.sdk_device_info_description) {
                InfoCard(
                    listOf(
                        stringResource(R.string.sdk_device_brand) to "Google",
                        stringResource(R.string.sdk_device_model) to "Pixel 8",
                        stringResource(R.string.sdk_device_memory) to "8.0 GB",
                    ),
                )
            }
        }
    }
}
