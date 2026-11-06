package com.primestrider.kotlinstarter.feature.example.sdk.picker

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import java.io.File
import kotlin.math.roundToInt

/** The centered square of a [width] x [height] image: its shorter side, trimmed equally from both ends of the longer one. */
fun squareCropRect(
    width: Int,
    height: Int,
): Rect {
    val side = minOf(width, height)
    val left = (width - side) / 2
    val top = (height - side) / 2
    return Rect(left, top, left + side, top + side)
}

/**
 * Overwrites the JPEG [file] with its centered square at [quality] (0..1) and returns the side in pixels.
 * Throws when the file cannot be decoded.
 */
internal fun cropToSquare(
    file: File,
    quality: Float,
): Int {
    val source = checkNotNull(BitmapFactory.decodeFile(file.path)) { "Could not decode the image" }
    val rect = squareCropRect(source.width, source.height)
    val square = Bitmap.createBitmap(source, rect.left, rect.top, rect.width(), rect.height())
    file.outputStream().use { square.compress(Bitmap.CompressFormat.JPEG, (quality * 100).roundToInt(), it) }
    return square.width
}
