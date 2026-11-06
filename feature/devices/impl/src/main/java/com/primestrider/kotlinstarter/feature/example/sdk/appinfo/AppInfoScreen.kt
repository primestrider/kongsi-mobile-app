package com.primestrider.kotlinstarter.feature.example.sdk.appinfo

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.pm.PackageInfoCompat
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory
import com.primestrider.kotlinstarter.core.designsystem.component.AppButton
import com.primestrider.kotlinstarter.core.designsystem.component.Banner
import com.primestrider.kotlinstarter.core.designsystem.component.BannerVariant
import com.primestrider.kotlinstarter.core.designsystem.component.ButtonVariant
import com.primestrider.kotlinstarter.core.designsystem.component.InfoCard
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.core.util.findActivity
import com.primestrider.kotlinstarter.feature.devices.impl.R
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold
import com.primestrider.kotlinstarter.feature.example.sdk.SdkSection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

private enum class ReviewOutcome { REQUESTED, UNAVAILABLE }

/** Package info read from the installed app, and the Play in-app review flow with a store-page fallback. */
@Composable
fun AppInfoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val info = remember(context) { context.packageInfo() }
    var outcome by remember { mutableStateOf<ReviewOutcome?>(null) }

    ExamplePageScaffold(
        title = R.string.sdk_app_info_title,
        subtitle = R.string.sdk_app_info_subtitle,
        onBack = onBack,
    ) {
        SdkSection(R.string.sdk_app_info_application_title, R.string.sdk_app_info_application_description) {
            InfoCard(
                listOf(
                    stringResource(R.string.sdk_app_info_name) to context.applicationInfo.loadLabel(context.packageManager).toString(),
                    stringResource(R.string.sdk_app_info_package) to context.packageName,
                    stringResource(R.string.sdk_app_info_version) to info.versionName,
                    stringResource(R.string.sdk_app_info_build) to PackageInfoCompat.getLongVersionCode(info).toString(),
                    stringResource(R.string.sdk_app_info_installed) to
                        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(info.firstInstallTime)),
                    stringResource(R.string.sdk_app_info_platform) to
                        stringResource(R.string.sdk_app_info_platform_value, Build.VERSION.RELEASE),
                ),
            )
        }
        SdkSection(R.string.sdk_app_info_rate_title, R.string.sdk_app_info_rate_description) {
            AppButton(
                stringResource(R.string.sdk_app_info_rate),
                onClick = {
                    val activity = context.findActivity()
                    scope.launch {
                        outcome =
                            try {
                                val manager = ReviewManagerFactory.create(context)
                                val reviewInfo = manager.requestReview()
                                if (activity == null) {
                                    ReviewOutcome.UNAVAILABLE
                                } else {
                                    manager.launchReview(activity, reviewInfo)
                                    ReviewOutcome.REQUESTED
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                // Play Store missing (emulator without Play, sideloaded build) or quota reached.
                                ReviewOutcome.UNAVAILABLE
                            }
                    }
                },
            )
            AppButton(
                stringResource(R.string.sdk_app_info_store),
                onClick = { if (!openStorePage(context)) outcome = ReviewOutcome.UNAVAILABLE },
                variant = ButtonVariant.OUTLINED,
            )
            when (outcome) {
                ReviewOutcome.REQUESTED ->
                    Banner(
                        title = stringResource(R.string.sdk_app_info_review_requested),
                        message = stringResource(R.string.sdk_app_info_review_requested_message),
                        variant = BannerVariant.INFO,
                        onDismiss = { outcome = null },
                    )
                ReviewOutcome.UNAVAILABLE ->
                    Banner(
                        title = stringResource(R.string.sdk_app_info_review_unavailable),
                        variant = BannerVariant.INFO,
                        onDismiss = { outcome = null },
                    )
                null -> Unit
            }
        }
    }
}

private fun Context.packageInfo(): PackageInfo =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, 0)
    }

/** Opens the Play listing (store app, else the browser); false when nothing on the device can open either. */
private fun openStorePage(context: Context): Boolean {
    val id = context.packageName
    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$id")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(market)
        return true
    } catch (_: ActivityNotFoundException) {
        val web =
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$id"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(web)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}

@ThemePreviews
@Composable
private fun AppInfoScreenPreview() {
    PreviewTheme {
        ExamplePageScaffold(title = R.string.sdk_app_info_title, subtitle = R.string.sdk_app_info_subtitle, onBack = {}) {
            SdkSection(R.string.sdk_app_info_application_title, R.string.sdk_app_info_application_description) {
                InfoCard(
                    listOf(
                        stringResource(R.string.sdk_app_info_name) to "Kotlin Starter",
                        stringResource(R.string.sdk_app_info_package) to "com.primestrider.kotlinstarter",
                        stringResource(R.string.sdk_app_info_version) to "1.0.0",
                        stringResource(R.string.sdk_app_info_build) to "1",
                    ),
                )
            }
        }
    }
}
