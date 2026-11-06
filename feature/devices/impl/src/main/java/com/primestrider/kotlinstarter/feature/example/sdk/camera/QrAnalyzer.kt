package com.primestrider.kotlinstarter.feature.example.sdk.camera

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executor

/**
 * Reads QR codes from the camera frames with ML Kit and offers the first raw value to [gate]. [analyze] runs
 * on the analysis executor; ML Kit's results arrive on [mainExecutor], where the gate is touched. Every
 * [ImageProxy] is closed when its detection finishes, whether it succeeded, failed or was never started.
 */
class QrAnalyzer(
    private val gate: QrResultGate,
    private val mainExecutor: Executor,
    private val scanner: BarcodeScanner =
        BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()),
) : ImageAnalysis.Analyzer {
    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        val media = image.image
        if (media == null || !gate.scanning || gate.current != null) {
            image.close()
            return
        }
        try {
            scanner
                .process(InputImage.fromMediaImage(media, image.imageInfo.rotationDegrees))
                .addOnSuccessListener(mainExecutor) { codes ->
                    codes.firstNotNullOfOrNull { it.rawValue?.takeIf(String::isNotBlank) }?.let(gate::offer)
                }.addOnCompleteListener(mainExecutor) { image.close() }
        } catch (e: RuntimeException) {
            image.close()
        }
    }

    /** Releases the ML Kit detector; call once the analysis use case is unbound. */
    fun close() = scanner.close()
}
