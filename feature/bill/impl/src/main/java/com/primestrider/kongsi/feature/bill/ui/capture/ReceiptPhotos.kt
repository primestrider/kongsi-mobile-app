package com.primestrider.kongsi.feature.bill.ui.capture

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.primestrider.kongsi.feature.bill.impl.R
import java.io.File

/** Hands out receipt photos in `cache/receipts/` (see `res/xml/receipt_paths.xml`), declared in this module's manifest. */
class ReceiptFileProvider : FileProvider(R.xml.receipt_paths)

/**
 * Receipt photos taken with the phone's camera app. They live in the app's cache, never in the gallery, and are deleted
 * once read: only what is read from them is kept.
 */
object ReceiptPhotos {
    /** The cache folder, which is also the `<cache-path name>` in `res/xml/receipt_paths.xml`: keep them equal. */
    private const val FOLDER = "receipts"

    /**
     * A new, empty file for the camera app to write a photo to, as a `content://` uri it is allowed to write. The uri
     * has [ReceiptFileProvider]'s own form, `content://<authority>/receipts/<file>`. It is built here rather than with
     * `FileProvider.getUriForFile`, which compares paths with "/" and so fails in tests run on Windows.
     */
    fun newPhotoUri(context: Context): Uri {
        val folder = File(context.cacheDir, FOLDER).apply { mkdirs() }
        val file = File(folder, "receipt-${System.currentTimeMillis()}.jpg").apply { createNewFile() }
        return Uri
            .Builder()
            .scheme("content")
            .authority(authority(context))
            .appendPath(FOLDER)
            .appendPath(file.name)
            .build()
    }

    /** Deletes [uri] when it is one of our photos; a photo picked from the gallery is the user's and stays. */
    fun discard(
        context: Context,
        uri: Uri,
    ) {
        if (uri.authority != authority(context)) return
        val name = uri.lastPathSegment ?: return
        File(File(context.cacheDir, FOLDER), File(name).name).delete()
    }

    private fun authority(context: Context) = "${context.packageName}.receipts"
}
