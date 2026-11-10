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

    private fun newPhoto(): File = ReceiptPhotos.newPhotoFile(app).apply { writeText("jpeg") }

    @Test
    fun a_new_photo_goes_to_the_receipts_cache_folder() {
        val file = ReceiptPhotos.newPhotoFile(app)

        assertEquals(folder.canonicalFile, file.parentFile!!.canonicalFile)
        assertTrue(folder.isDirectory)
    }

    @Test
    fun discarding_deletes_our_own_photo() {
        val file = newPhoto()

        ReceiptPhotos.discard(app, Uri.fromFile(file))

        assertFalse(file.exists())
    }

    @Test
    fun discarding_leaves_a_photo_from_the_gallery_alone() {
        val mine = newPhoto()

        ReceiptPhotos.discard(app, Uri.parse("content://media/picker/0/com.android.providers.media.photopicker/media/1"))

        assertTrue(mine.exists())
    }

    @Test
    fun discarding_leaves_a_file_outside_the_receipts_folder_alone() {
        val other = File(app.cacheDir, "other.jpg").apply { writeText("jpeg") }

        ReceiptPhotos.discard(app, Uri.fromFile(other))

        assertTrue(other.exists())
    }
}
