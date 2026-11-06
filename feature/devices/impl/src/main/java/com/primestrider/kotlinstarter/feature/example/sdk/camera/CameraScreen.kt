package com.primestrider.kotlinstarter.feature.example.sdk.camera

import android.Manifest
import android.content.Context
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import com.primestrider.kotlinstarter.core.designsystem.component.AppBottomSheet
import com.primestrider.kotlinstarter.core.designsystem.component.AppButton
import com.primestrider.kotlinstarter.core.designsystem.component.Banner
import com.primestrider.kotlinstarter.core.designsystem.component.BannerVariant
import com.primestrider.kotlinstarter.core.designsystem.component.ButtonVariant
import com.primestrider.kotlinstarter.core.designsystem.component.LabeledSlider
import com.primestrider.kotlinstarter.core.designsystem.component.SegmentedChoice
import com.primestrider.kotlinstarter.core.designsystem.component.SwitchRow
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Radii
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.core.permission.PermissionGate
import com.primestrider.kotlinstarter.core.util.currentLocale
import com.primestrider.kotlinstarter.feature.devices.impl.R
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold
import com.primestrider.kotlinstarter.feature.example.sdk.SdkSection
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject

/** Keeps the [QrResultGate] across recomposition and rotation, so one QR code seen continuously opens one sheet. */
@HiltViewModel
class CameraViewModel
    @Inject
    constructor() : ViewModel() {
        val gate = QrResultGate()
    }

/** CameraX preview with QR scanning (opens a sheet with the value) and photo capture; the whole screen needs the camera permission. */
@Composable
fun CameraScreen(
    onBack: () -> Unit,
    viewModel: CameraViewModel = hiltViewModel(),
) {
    ExamplePageScaffold(
        title = R.string.sdk_camera_title,
        subtitle = R.string.sdk_camera_subtitle,
        onBack = onBack,
    ) {
        PermissionGate(
            permissions = listOf(Manifest.permission.CAMERA),
            reason = stringResource(R.string.sdk_camera_reason),
        ) {
            CameraContent(viewModel.gate)
        }
    }
}

/**
 * Height of the camera preview: 380dp, but at most 55% of the window height. The preview view consumes touches, so
 * in landscape a full-height preview left nowhere to start a scroll and the controls below were unreachable.
 */
internal fun cameraPreviewHeight(windowHeightDp: Int): Dp = minOf(380.dp, (windowHeightDp * 0.55f).dp)

@Composable
private fun CameraContent(gate: QrResultGate) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember(context, gate) { CameraController(context, gate) }
    var mode by rememberSaveable { mutableStateOf(CameraMode.SCAN) }
    var flash by rememberSaveable { mutableStateOf(FlashSetting.OFF) }
    var torch by rememberSaveable { mutableStateOf(false) }
    var zoom by rememberSaveable { mutableFloatStateOf(0f) }
    var front by rememberSaveable { mutableStateOf(false) }
    var lastPhoto by rememberSaveable { mutableStateOf<String?>(null) }
    var captureFailed by remember { mutableStateOf(false) }
    val previewView = remember(context) { cameraPreviewView(context) }

    DisposableEffect(controller) { onDispose(controller::release) }
    DeviceRotationEffect(controller::setTargetRotation)
    // The gate only accepts codes in the scan mode; leaving the screen also stops it.
    DisposableEffect(mode) {
        gate.scanning = mode == CameraMode.SCAN
        onDispose { gate.scanning = false }
    }
    LaunchedEffect(mode, front) { controller.bind(lifecycleOwner, previewView, mode, front) }
    LaunchedEffect(flash) { controller.setFlash(flash) }
    LaunchedEffect(torch) { controller.setTorch(torch) }
    LaunchedEffect(zoom) { controller.setZoom(zoom) }

    val previewLabel = stringResource(R.string.sdk_camera_preview_title)
    val previewHeight = cameraPreviewHeight(LocalConfiguration.current.screenHeightDp)
    SdkSection(R.string.sdk_camera_preview_title, previewDescription(mode)) {
        AndroidView(
            factory = { previewView },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(previewHeight)
                    .clip(RoundedCornerShape(Radii.lg))
                    .semantics { contentDescription = previewLabel },
        )
        if (controller.failed) Banner(title = stringResource(R.string.sdk_camera_error), variant = BannerVariant.ERROR)
    }

    val modeLabels =
        CameraMode.entries.associateWith {
            stringResource(if (it == CameraMode.SCAN) R.string.sdk_camera_mode_scan else R.string.sdk_camera_mode_photo)
        }
    val flashLabels = FlashSetting.entries.associateWith { stringResource(flashLabel(it)) }
    SdkSection(R.string.sdk_camera_controls_title, R.string.sdk_camera_controls_description) {
        SegmentedChoice(options = CameraMode.entries, selected = mode, onSelect = { mode = it }, label = { modeLabels.getValue(it) })
        SegmentedChoice(
            options = FlashSetting.entries,
            selected = flash,
            onSelect = { flash = it },
            label = { flashLabels.getValue(it) },
            enabled = mode == CameraMode.PHOTO,
        )
        SwitchRow(label = stringResource(R.string.sdk_camera_torch), checked = torch, onCheckedChange = { torch = it })
        LabeledSlider(
            label = stringResource(R.string.sdk_camera_zoom),
            value = zoom,
            onValueChange = { zoom = it },
            valueRange = 0f..1f,
            valueText = "%.1f".format(currentLocale(), zoom),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            AppButton(stringResource(R.string.sdk_camera_flip), onClick = { front = !front }, variant = ButtonVariant.OUTLINED)
            if (mode == CameraMode.PHOTO) {
                AppButton(
                    stringResource(R.string.sdk_camera_capture),
                    enabled = controller.bound,
                    onClick = {
                        captureFailed = false
                        controller.capture { file ->
                            if (file == null) captureFailed = true else lastPhoto = file.absolutePath
                        }
                    },
                )
            }
        }
        if (captureFailed) Banner(title = stringResource(R.string.sdk_camera_capture_failed), variant = BannerVariant.ERROR)
    }

    lastPhoto?.let { path ->
        SdkSection(R.string.sdk_camera_last_photo_title, R.string.sdk_camera_last_photo_description) {
            AsyncImage(
                model = File(path),
                contentDescription = stringResource(R.string.sdk_camera_last_photo_title),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(Radii.lg)),
            )
        }
    }

    QrResultHost(gate)
}

/**
 * The preview draws into a TextureView. The default SurfaceView is a separate window layer that ignores the
 * alpha of Navigation's page fade, so it stayed solid over the next page and then vanished abruptly.
 */
internal fun cameraPreviewView(context: Context): PreviewView =
    PreviewView(context).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE }

private fun previewDescription(mode: CameraMode): Int =
    if (mode == CameraMode.SCAN) R.string.sdk_camera_preview_scan else R.string.sdk_camera_preview_photo

private fun flashLabel(flash: FlashSetting): Int =
    when (flash) {
        FlashSetting.OFF -> R.string.sdk_camera_flash_off
        FlashSetting.ON -> R.string.sdk_camera_flash_on
        FlashSetting.AUTO -> R.string.sdk_camera_flash_auto
    }

/** Shows the scanned value in a sheet while [gate] holds one; closing the sheet clears the gate so scanning resumes. */
@Composable
internal fun QrResultHost(gate: QrResultGate) {
    val value = gate.current ?: return
    AppBottomSheet(onDismiss = gate::clear, title = stringResource(R.string.sdk_camera_result_title)) {
        SelectionContainer {
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.s4),
            )
        }
        Text(
            stringResource(R.string.sdk_camera_result_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.s4, vertical = Spacing.s2),
        )
        AppButton(
            stringResource(R.string.sdk_camera_close),
            onClick = gate::clear,
            variant = ButtonVariant.OUTLINED,
            modifier = Modifier.padding(horizontal = Spacing.s4, vertical = Spacing.s2),
        )
    }
}

@ThemePreviews
@Composable
private fun CameraScreenPreview() {
    PreviewTheme {
        ExamplePageScaffold(title = R.string.sdk_camera_title, subtitle = R.string.sdk_camera_subtitle, onBack = {}) {
            PermissionGate(
                permissions = listOf(Manifest.permission.CAMERA),
                reason = stringResource(R.string.sdk_camera_reason),
            ) {}
        }
    }
}
