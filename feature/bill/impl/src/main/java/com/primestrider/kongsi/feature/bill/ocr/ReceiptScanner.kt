package com.primestrider.kongsi.feature.bill.ocr

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.primestrider.kongsi.feature.bill.ui.capture.ReceiptPhotos
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** A photo could not be read: it could not be opened, or the text recogniser failed on it. */
class ReceiptScanException(
    cause: Throwable,
) : Exception(cause)

/** Reads the text on a receipt photo. An interface so the screen's ViewModel can be tested without ML Kit. */
interface ReceiptScanner {
    /**
     * The lines of text on the photo at [uri], with their boxes, for [ReceiptParser]. A photo taken in the app is
     * deleted afterwards, read or not: only what is read from it is kept. Throws [ReceiptScanException].
     */
    suspend fun scan(uri: Uri): List<ReceiptLine>
}

/**
 * [ReceiptScanner] on ML Kit Text Recognition v2 (Latin), with the model bundled in the app, so it works offline right
 * after install and nothing leaves the phone.
 */
@Singleton
class MlKitReceiptScanner
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ReceiptScanner {
        private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

        override suspend fun scan(uri: Uri): List<ReceiptLine> =
            withContext(Dispatchers.IO) {
                try {
                    val image = InputImage.fromFilePath(context, uri)
                    val text = recognizer.process(image).await()
                    text.textBlocks.flatMap { it.lines }.mapNotNull { line ->
                        val box = line.boundingBox ?: return@mapNotNull null
                        ReceiptLine(line.text, box.left, box.top, box.right, box.bottom)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Opening the image throws IOException; ML Kit fails with MlKitException or others of its own.
                    throw ReceiptScanException(e)
                } finally {
                    ReceiptPhotos.discard(context, uri)
                }
            }
    }

/** Waits for a Play services [Task] without the extra coroutines-play-services artifact. */
private suspend fun <T> Task<T>.await(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { continuation.resume(it) }
        addOnFailureListener { continuation.resumeWithException(it) }
        addOnCanceledListener { continuation.cancel() }
    }
