package com.primestrider.kotlinstarter.feature.example.sdk.camera

import android.content.Context
import android.view.Surface
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** What the camera is doing: reading QR codes, or taking photos. */
enum class CameraMode { SCAN, PHOTO }

/** Flash setting of [ImageCapture]; [mode] is its `ImageCapture.FLASH_MODE_*` value. */
enum class FlashSetting(
    val mode: Int,
) {
    OFF(ImageCapture.FLASH_MODE_OFF),
    ON(ImageCapture.FLASH_MODE_ON),
    AUTO(ImageCapture.FLASH_MODE_AUTO),
}

private const val JPEG_QUALITY = 70

/**
 * Wraps CameraX for the demo: binds the preview plus either image capture (photo mode) or QR analysis (scan
 * mode) to a lifecycle, and applies flash, torch, linear zoom and the lens. Analysis runs on its own
 * single-thread executor; [release] unbinds everything, closes the QR detector and shuts that executor down.
 */
class CameraController(
    context: Context,
    gate: QrResultGate,
) {
    private val context = context.applicationContext
    private val mainExecutor = ContextCompat.getMainExecutor(this.context)
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val analyzer by lazy { QrAnalyzer(gate, mainExecutor) }
    private var analyzerCreated = false
    private var provider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var released = false

    private var rotation: Int? = null
    private var flash = FlashSetting.OFF
    private var torch = false
    private var zoom = 0f

    /** True once the use cases are bound to the camera; the Capture button waits for it. */
    var bound: Boolean by mutableStateOf(false)
        private set

    /** Set when the camera cannot be opened, e.g. a device without that lens. */
    var failed: Boolean by mutableStateOf(false)
        private set

    /** (Re)binds the use cases of [mode] on the chosen lens to [owner], showing the preview in [previewView]. */
    fun bind(
        owner: LifecycleOwner,
        previewView: PreviewView,
        mode: CameraMode,
        front: Boolean,
    ) {
        bound = false
        failed = false
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (released) return@addListener
            val cameraProvider =
                try {
                    future.get()
                } catch (e: Exception) {
                    failed = true
                    return@addListener
                }
            provider = cameraProvider
            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            val selector = if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            val targetRotation = rotation ?: previewView.display?.rotation ?: Surface.ROTATION_0
            try {
                cameraProvider.unbindAll()
                camera =
                    when (mode) {
                        CameraMode.SCAN -> {
                            imageCapture = null
                            val analysis = analysis(targetRotation).also { imageAnalysis = it }
                            cameraProvider.bindToLifecycle(owner, selector, preview, analysis)
                        }
                        CameraMode.PHOTO -> {
                            imageAnalysis = null
                            val capture =
                                ImageCapture
                                    .Builder()
                                    .setJpegQuality(JPEG_QUALITY)
                                    .setFlashMode(flash.mode)
                                    .setTargetRotation(targetRotation)
                                    .build()
                            imageCapture = capture
                            cameraProvider.bindToLifecycle(owner, selector, preview, capture)
                        }
                    }
                applyTorch()
                applyZoom()
                bound = true
            } catch (e: IllegalArgumentException) {
                // No camera for this lens (CameraSelector filters everything out).
                failed = true
            } catch (e: IllegalStateException) {
                failed = true
            }
        }, mainExecutor)
    }

    private fun analysis(targetRotation: Int): ImageAnalysis {
        analyzerCreated = true
        return ImageAnalysis
            .Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(targetRotation)
            .build()
            .also { it.setAnalyzer(analysisExecutor, analyzer) }
    }

    /**
     * The device's current `Surface.ROTATION_*`. MainActivity handles orientation changes without being
     * recreated, so the bound use cases are updated here; otherwise photos keep the rotation of bind time.
     */
    fun setTargetRotation(value: Int) {
        rotation = value
        imageCapture?.targetRotation = value
        imageAnalysis?.targetRotation = value
    }

    fun setFlash(value: FlashSetting) {
        flash = value
        imageCapture?.flashMode = value.mode
    }

    fun setTorch(on: Boolean) {
        torch = on
        applyTorch()
    }

    /** [linear] is 0..1 across the lens' zoom range. */
    fun setZoom(linear: Float) {
        zoom = linear
        applyZoom()
    }

    // The returned futures complete with an error on a lens without a flash unit; nothing to do then.
    private fun applyTorch() {
        camera?.cameraControl?.enableTorch(torch)
    }

    private fun applyZoom() {
        camera?.cameraControl?.setLinearZoom(zoom)
    }

    /** Takes a photo into `cache/camera/<time>.jpg`; [onResult] gets the file, or null when the capture failed. */
    fun capture(onResult: (File?) -> Unit) {
        val capture = imageCapture ?: return onResult(null)
        val file = File(context.cacheDir, "camera/${System.currentTimeMillis()}.jpg").also { it.parentFile?.mkdirs() }
        capture.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            mainExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) = onResult(file)

                override fun onError(exception: ImageCaptureException) = onResult(null)
            },
        )
    }

    /** Unbinds the camera, closes the QR detector and stops the analysis thread; the controller is unusable afterwards. */
    fun release() {
        released = true
        bound = false
        provider?.unbindAll()
        analysisExecutor.shutdown()
        if (analyzerCreated) analyzer.close()
    }
}
