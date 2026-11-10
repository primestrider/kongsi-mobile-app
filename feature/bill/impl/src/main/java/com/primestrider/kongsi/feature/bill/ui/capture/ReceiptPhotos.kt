package com.primestrider.kongsi.feature.bill.ui.capture

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Receipt photos taken with the camera inside the app ([ReceiptCamera]). They live in the app's cache, never in the
 * gallery, and are deleted once read: only what is read from them is kept.
 */
object ReceiptPhotos {
    private const val FOLDER = "receipts"

    /** A new file in `cache/receipts/` for the camera to write a photo to. */
    fun newPhotoFile(context: Context): File {
        val folder = File(context.cacheDir, FOLDER).apply { mkdirs() }
        return File(folder, "receipt-${System.currentTimeMillis()}.jpg")
    }

    /** Deletes [uri] when it is one of our photos; a photo picked from the gallery is the user's and stays. */
    fun discard(
        context: Context,
        uri: Uri,
    ) {
        if (uri.scheme != "file") return
        val file = File(uri.path ?: return)
        if (file.parentFile?.canonicalFile == File(context.cacheDir, FOLDER).canonicalFile) file.delete()
    }
}
