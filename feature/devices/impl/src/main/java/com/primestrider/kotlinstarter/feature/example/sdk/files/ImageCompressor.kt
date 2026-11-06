package com.primestrider.kotlinstarter.feature.example.sdk.files

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.InputStream
import kotlin.math.roundToInt

/** Result of [ImageCompressor.compress]; sizes are in bytes, dimensions are as displayed (after EXIF rotation). */
data class CompressedImage(
    val file: File,
    val originalWidth: Int,
    val originalHeight: Int,
    val originalBytes: Long,
    val width: Int,
    val height: Int,
    val bytes: Long,
)

/** `{n}% smaller`: the share of [original] that [compressed] saved, rounded; never negative. */
fun savingPercent(
    original: Long,
    compressed: Long,
): Int = if (original <= 0) 0 else ((original - compressed) * 100.0 / original).roundToInt().coerceAtLeast(0)

/** Resizes a photo to a maximum width and re-encodes it as JPEG. */
object ImageCompressor {
    /** [width] x [height] scaled to [maxWidth] keeping the aspect ratio; only ever scales down. */
    fun targetSize(
        width: Int,
        height: Int,
        maxWidth: Int,
    ): Pair<Int, Int> {
        if (width <= maxWidth) return width to height
        return maxWidth to (height * maxWidth.toDouble() / width).roundToInt().coerceAtLeast(1)
    }

    /** Decodes [input] (sub-sampled, EXIF-rotated), scales it to [maxWidth] and writes a JPEG of [quality] (0..1) to [out]. */
    fun compress(
        input: InputStream,
        maxWidth: Int,
        quality: Float,
        out: File,
    ): CompressedImage {
        val data = input.use { it.readBytes() }
        val orientation = ExifInterface(data.inputStream()).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Not an image" }

        val quarterTurn = orientation in QUARTER_TURNS
        val shownWidth = if (quarterTurn) bounds.outHeight else bounds.outWidth
        val shownHeight = if (quarterTurn) bounds.outWidth else bounds.outHeight

        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(shownWidth, maxWidth) }
        val decoded =
            checkNotNull(BitmapFactory.decodeByteArray(data, 0, data.size, options)) { "Could not decode the image" }
        val upright = decoded.orient(orientation)
        val (width, height) = targetSize(upright.width, upright.height, maxWidth)
        val scaled = if (width == upright.width) upright else Bitmap.createScaledBitmap(upright, width, height, true)

        out.parentFile?.mkdirs()
        out.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, (quality * 100).roundToInt(), it) }
        return CompressedImage(out, shownWidth, shownHeight, data.size.toLong(), scaled.width, scaled.height, out.length())
    }

    /** Largest power-of-two sub-sample that still leaves at least [maxWidth] pixels across. */
    private fun sampleSize(
        shownWidth: Int,
        maxWidth: Int,
    ): Int {
        var sample = 1
        while (shownWidth / (sample * 2) >= maxWidth) sample *= 2
        return sample
    }

    private val QUARTER_TURNS =
        setOf(
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_ROTATE_270,
            ExifInterface.ORIENTATION_TRANSPOSE,
            ExifInterface.ORIENTATION_TRANSVERSE,
        )

    private fun Bitmap.orient(orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return this
        }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }
}
