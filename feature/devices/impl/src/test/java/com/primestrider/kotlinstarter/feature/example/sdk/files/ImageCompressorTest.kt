package com.primestrider.kotlinstarter.feature.example.sdk.files

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class ImageCompressorTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun scales_down_keeping_the_aspect_ratio() {
        assertEquals(1080 to 810, ImageCompressor.targetSize(4000, 3000, 1080))
    }

    @Test
    fun never_scales_up() {
        assertEquals(800 to 600, ImageCompressor.targetSize(800, 600, 1080))
    }

    @Test
    fun saving_percent_is_the_relative_reduction() {
        assertEquals(75, savingPercent(1000, 250))
    }

    @Test
    fun compresses_a_png_to_a_scaled_jpeg() {
        val png =
            ByteArrayOutputStream()
                .also { Bitmap.createBitmap(2000, 1000, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, it) }
                .toByteArray()
        val out = folder.newFile("out.jpg")

        val result = ImageCompressor.compress(png.inputStream(), maxWidth = 1080, quality = 0.6f, out = out)

        assertEquals(2000 to 1000, result.originalWidth to result.originalHeight)
        assertEquals(1080 to 540, result.width to result.height)
        assertEquals(png.size.toLong(), result.originalBytes)
        assertEquals(out.length(), result.bytes)
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(out.path, options)
        assertEquals(1080 to 540, options.outWidth to options.outHeight)
        assertEquals(listOf(0xFF.toByte(), 0xD8.toByte()), out.readBytes().take(2))
    }
}
