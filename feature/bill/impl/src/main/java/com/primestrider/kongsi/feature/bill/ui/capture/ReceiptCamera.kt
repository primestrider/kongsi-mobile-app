package com.primestrider.kongsi.feature.bill.ui.capture

import android.Manifest
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.primestrider.kongsi.core.designsystem.component.AppScaffold
import com.primestrider.kongsi.core.designsystem.component.Banner
import com.primestrider.kongsi.core.designsystem.component.BannerVariant
import com.primestrider.kongsi.core.designsystem.component.LocalMessenger
import com.primestrider.kongsi.core.designsystem.component.UiText
import com.primestrider.kongsi.core.designsystem.theme.PreviewTheme
import com.primestrider.kongsi.core.designsystem.theme.Spacing
import com.primestrider.kongsi.core.designsystem.theme.ThemePreviews
import com.primestrider.kongsi.core.permission.PermissionGate
import com.primestrider.kongsi.feature.bill.impl.R

/**
 * The camera inside the app, for photographing a receipt without leaving Kongsi. It asks for the camera permission the
 * first time; [onPhoto] gets the photo, saved in `cache/receipts/` ([ReceiptPhotos]). Back or the arrow calls [onClose].
 */
@Composable
internal fun ReceiptCamera(
    onPhoto: (Uri) -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    AppScaffold(title = stringResource(R.string.bill_capture_take_photo), onBack = onClose) { padding ->
        PermissionGate(
            permissions = listOf(Manifest.permission.CAMERA),
            reason = stringResource(R.string.bill_camera_reason),
            modifier = Modifier.padding(padding).padding(Spacing.s4),
        ) {
            Viewfinder(onPhoto, Modifier.padding(padding))
        }
    }
}

/**
 * The live preview, the receipt frame and the shutter. CameraX's [LifecycleCameraController] runs the camera while the
 * screen is shown, focuses where the preview is tapped, zooms on pinch, and saves photos the right way up however the
 * phone is held.
 */
@Composable
private fun Viewfinder(
    onPhoto: (Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val messenger = LocalMessenger.current
    val latestOnPhoto by rememberUpdatedState(onPhoto)
    var ready by remember { mutableStateOf(false) }
    var unavailable by remember { mutableStateOf(false) }
    var taking by remember { mutableStateOf(false) }
    val controller =
        remember(context) {
            LifecycleCameraController(context).apply {
                setEnabledUseCases(CameraController.IMAGE_CAPTURE)
                // A sharp photo reads better than a fast one.
                imageCaptureMode = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
            }
        }
    DisposableEffect(controller, lifecycleOwner) {
        controller.bindToLifecycle(lifecycleOwner)
        val init = controller.initializationFuture
        init.addListener({
            val opened = runCatching { init.get() }.isSuccess && controller.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
            ready = opened
            unavailable = !opened
        }, ContextCompat.getMainExecutor(context))
        onDispose { controller.unbind() }
    }

    val takePhoto = {
        taking = true
        val file = ReceiptPhotos.newPhotoFile(context)
        controller.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) = latestOnPhoto(Uri.fromFile(file))

                override fun onError(exception: ImageCaptureException) {
                    taking = false
                    file.delete()
                    messenger.error(UiText.Resource(R.string.bill_camera_capture_failed))
                }
            },
        )
    }

    Box(modifier.fillMaxSize().background(Color.Black)) {
        val previewLabel = stringResource(R.string.bill_camera_preview)
        AndroidView(
            factory = {
                PreviewView(it).apply {
                    // A TextureView, so the preview fades with the page instead of staying on top of it.
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    this.controller = controller
                }
            },
            modifier = Modifier.fillMaxSize().semantics { contentDescription = previewLabel },
        )
        Column(Modifier.fillMaxSize()) {
            ReceiptFrame(Modifier.weight(1f))
            Controls(taking = taking, enabled = ready && !taking, onShutter = takePhoto)
        }
        if (unavailable) {
            Banner(
                title = stringResource(R.string.bill_camera_unavailable),
                variant = BannerVariant.ERROR,
                modifier = Modifier.align(Alignment.TopCenter).padding(Spacing.s4),
            )
        }
    }
}

/**
 * The frame to fit the receipt in, as in the QR scanners of e-wallet apps: the camera image outside it is dimmed and
 * its corners are marked. Tall, since receipts are: it fills the space above the shutter, less a margin. It only
 * guides; the photo is the whole camera image, so a receipt slightly past the frame is still read.
 */
@Composable
private fun ReceiptFrame(modifier: Modifier = Modifier) {
    // Offscreen, so the BlendMode.Clear below cuts through the dim instead of punching a black hole.
    Canvas(modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        val inset = FRAME_INSET.toPx()
        val frame = Size(size.width - 2 * inset, size.height - 2 * inset)
        if (frame.width <= 0f || frame.height <= 0f) return@Canvas
        val radius = FRAME_RADIUS.toPx()
        val l = inset
        val t = inset
        val r = l + frame.width
        val b = t + frame.height
        drawRect(Color.Black.copy(alpha = SCRIM_ALPHA))
        drawRoundRect(Color.Transparent, Offset(l, t), frame, CornerRadius(radius), blendMode = BlendMode.Clear)

        val arm = minOf(BRACKET_LENGTH.toPx(), frame.width / 2, frame.height / 2)
        val d = 2 * radius
        val corners =
            Path().apply {
                moveTo(l, t + arm)
                lineTo(l, t + radius)
                arcTo(Rect(l, t, l + d, t + d), 180f, 90f, false)
                lineTo(l + arm, t)

                moveTo(r - arm, t)
                lineTo(r - radius, t)
                arcTo(Rect(r - d, t, r, t + d), 270f, 90f, false)
                lineTo(r, t + arm)

                moveTo(r, b - arm)
                lineTo(r, b - radius)
                arcTo(Rect(r - d, b - d, r, b), 0f, 90f, false)
                lineTo(r - arm, b)

                moveTo(l + arm, b)
                lineTo(l + radius, b)
                arcTo(Rect(l, b - d, l + d, b), 90f, 90f, false)
                lineTo(l, b - arm)
            }
        drawPath(corners, Color.White, style = Stroke(width = BRACKET_WIDTH.toPx(), cap = StrokeCap.Round))
    }
}

/** Below the frame: the hint and the shutter, on the same dim as around the frame. */
@Composable
private fun Controls(
    taking: Boolean,
    enabled: Boolean,
    onShutter: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = SCRIM_ALPHA))
            .padding(start = Spacing.s4, end = Spacing.s4, bottom = Spacing.s6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.s4),
    ) {
        Text(
            stringResource(R.string.bill_camera_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Shutter(taking = taking, enabled = enabled, onClick = onShutter)
    }
}

/** The round shutter button; while the photo is saved it shows progress and cannot be pressed again. */
@Composable
private fun Shutter(
    taking: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val label = stringResource(R.string.bill_camera_shutter)
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = if (enabled || taking) Color.White else Color.White.copy(alpha = DISABLED_ALPHA),
        border = BorderStroke(4.dp, Color.White.copy(alpha = DISABLED_ALPHA)),
        modifier = Modifier.size(72.dp).semantics { contentDescription = label },
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (taking) CircularProgressIndicator(Modifier.size(32.dp), color = Color.Black, strokeWidth = 3.dp)
        }
    }
}

/** Over the camera image outside the frame and behind the controls, so the frame stands out and the hint reads. */
private const val SCRIM_ALPHA = 0.55f

private const val DISABLED_ALPHA = 0.5f

private val FRAME_INSET = 24.dp
private val FRAME_RADIUS = 16.dp
private val BRACKET_LENGTH = 36.dp
private val BRACKET_WIDTH = 4.dp

@ThemePreviews
@Composable
private fun ReceiptCameraPreview() {
    // The permission card: a preview cannot open the camera.
    PreviewTheme { ReceiptCamera(onPhoto = {}, onClose = {}) }
}

@ThemePreviews
@Composable
private fun ReceiptFramePreview() {
    PreviewTheme {
        Column(Modifier.size(width = 360.dp, height = 640.dp).background(Color.DarkGray)) {
            ReceiptFrame(Modifier.weight(1f))
            Controls(taking = false, enabled = true, onShutter = {})
        }
    }
}
