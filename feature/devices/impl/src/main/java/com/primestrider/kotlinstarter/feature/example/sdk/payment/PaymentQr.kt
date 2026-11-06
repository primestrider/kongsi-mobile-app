package com.primestrider.kotlinstarter.feature.example.sdk.payment

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter

/** Pixel size of the rendered QR bitmap; the screen scales it down, so it stays sharp on dense displays. */
const val PAYMENT_QR_SIZE_PX = 512

private const val DARK = 0xFF000000.toInt()
private const val LIGHT = 0xFFFFFFFF.toInt()

/** Encodes [code] as a [size]×[size] QR bit matrix (ZXing, with its default quiet zone). */
fun paymentQrMatrix(
    code: String,
    size: Int = PAYMENT_QR_SIZE_PX,
): BitMatrix = QRCodeWriter().encode(code, BarcodeFormat.QR_CODE, size, size)

/** Renders [matrix] dark-on-light (whatever the app theme, so scanners can read it) into an [ImageBitmap]. */
fun paymentQrBitmap(matrix: BitMatrix): ImageBitmap {
    val width = matrix.width
    val height = matrix.height
    val pixels = IntArray(width * height) { index -> if (matrix[index % width, index / width]) DARK else LIGHT }
    return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888).asImageBitmap()
}
