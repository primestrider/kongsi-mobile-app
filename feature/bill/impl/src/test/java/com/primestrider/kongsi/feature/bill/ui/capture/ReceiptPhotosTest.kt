package com.primestrider.kongsi.feature.bill.ui.capture

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class ReceiptPhotosTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val folder = File(app.cacheDir, "receipts")

    @Test
    fun a_new_photo_goes_to_the_receipts_cache_folder_through_the_apps_own_provider() {
        val uri = ReceiptPhotos.newPhotoUri(app)

        assertEquals("${app.packageName}.receipts", uri.authority)
        assertTrue(folder.isDirectory)
        assertTrue(File(folder, uri.lastPathSegment!!).exists())
    }

    @Test
    fun discarding_deletes_our_own_photo() {
        val uri = ReceiptPhotos.newPhotoUri(app)
        val file = File(folder, uri.lastPathSegment!!)

        ReceiptPhotos.discard(app, uri)

        assertFalse(file.exists())
    }

    @Test
    fun discarding_leaves_a_photo_from_the_gallery_alone() {
        val mine = ReceiptPhotos.newPhotoUri(app)

        ReceiptPhotos.discard(app, Uri.parse("content://media/picker/0/com.android.providers.media.photopicker/media/1"))

        assertTrue(File(folder, mine.lastPathSegment!!).exists())
    }
}
