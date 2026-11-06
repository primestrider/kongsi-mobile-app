package com.primestrider.kotlinstarter.feature.example.sdk.payment

import android.app.Activity
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.primestrider.kotlinstarter.core.designsystem.component.Banner
import com.primestrider.kotlinstarter.core.designsystem.component.BannerVariant
import com.primestrider.kotlinstarter.core.designsystem.component.SwitchRow
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.core.util.currentLocale
import com.primestrider.kotlinstarter.core.util.findActivity
import com.primestrider.kotlinstarter.core.util.formatCurrency
import com.primestrider.kotlinstarter.feature.devices.impl.R
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold
import com.primestrider.kotlinstarter.feature.example.sdk.SdkSection

private const val WALLET_BALANCE = 1_250_000.0
private const val PAYMENT_QR_SIZE = 200

/** A rotating payment code on a bright, awake, screenshot-proof screen; leaving restores the window as it was. */
@Composable
fun PaymentCodeScreen(
    onBack: () -> Unit,
    viewModel: PaymentCodeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val locale = currentLocale()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var brightness by rememberSaveable { mutableStateOf(true) }
    var keepOn by rememberSaveable { mutableStateOf(true) }
    var secure by rememberSaveable { mutableStateOf(true) }
    var screenshotTaken by rememberSaveable { mutableStateOf(false) }

    val activity = context.findActivity()
    val treatments = remember(activity) { activity?.window?.let(::WindowTreatments) }
    LaunchedEffect(treatments, brightness, keepOn, secure) { treatments?.apply(brightness, keepOn, secure) }
    DisposableEffect(treatments) { onDispose { treatments?.restore() } }
    if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        ScreenshotDetection(activity) { screenshotTaken = true }
    }

    ExamplePageScaffold(title = R.string.sdk_payment_title, subtitle = R.string.sdk_payment_subtitle, onBack = onBack) {
        if (screenshotTaken) {
            Banner(
                stringResource(R.string.sdk_payment_screenshot_title),
                message = stringResource(R.string.sdk_payment_screenshot_message),
                variant = BannerVariant.WARNING,
                onDismiss = { screenshotTaken = false },
            )
        }
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(Spacing.s4), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                Text(stringResource(R.string.sdk_payment_cashier), style = MaterialTheme.typography.titleMedium)
                val qr = remember(state.code) { paymentQrBitmap(paymentQrMatrix(state.code)) }
                Image(
                    bitmap = qr,
                    contentDescription = stringResource(R.string.sdk_payment_qr_description),
                    modifier = Modifier.size(PAYMENT_QR_SIZE.dp).align(Alignment.CenterHorizontally),
                )
                Text(
                    state.code,
                    style = MaterialTheme.typography.headlineLarge,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                viewModel.amount?.let {
                    val amount = formatCurrency(it.toDouble(), "IDR", locale, fractionDigits = 0)
                    Text(stringResource(R.string.sdk_payment_amount, amount), style = MaterialTheme.typography.bodyLarge)
                }
                val balance = formatCurrency(WALLET_BALANCE, "IDR", locale, fractionDigits = 0)
                Text(
                    stringResource(R.string.sdk_payment_refresh, state.secondsLeft, balance),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        SdkSection(R.string.sdk_payment_treatment_title, R.string.sdk_payment_treatment_description) {
            SwitchRow(stringResource(R.string.sdk_payment_brightness), brightness, { brightness = it })
            SwitchRow(stringResource(R.string.sdk_payment_keep_on), keepOn, { keepOn = it })
            SwitchRow(stringResource(R.string.sdk_payment_secure), secure, { secure = it })
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                Text(
                    stringResource(R.string.sdk_payment_detection_fallback),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Calls [onScreenshot] when the user takes a screenshot while this activity is visible (API 34+). */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Composable
private fun ScreenshotDetection(
    activity: Activity,
    onScreenshot: () -> Unit,
) {
    val latest by rememberUpdatedState(onScreenshot)
    DisposableEffect(activity) {
        val callback = Activity.ScreenCaptureCallback { latest() }
        activity.registerScreenCaptureCallback(ContextCompat.getMainExecutor(activity), callback)
        onDispose { activity.unregisterScreenCaptureCallback(callback) }
    }
}

@ThemePreviews
@Composable
private fun PaymentCodeScreenPreview() {
    PreviewTheme {
        ExamplePageScaffold(title = R.string.sdk_payment_title, subtitle = R.string.sdk_payment_subtitle, onBack = {}) {
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.sdk_payment_cashier), style = MaterialTheme.typography.titleMedium)
                    Image(
                        bitmap = paymentQrBitmap(paymentQrMatrix("482913")),
                        contentDescription = null,
                        modifier = Modifier.size(PAYMENT_QR_SIZE.dp).align(Alignment.CenterHorizontally),
                    )
                    Text(
                        "482913",
                        style = MaterialTheme.typography.headlineLarge,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
