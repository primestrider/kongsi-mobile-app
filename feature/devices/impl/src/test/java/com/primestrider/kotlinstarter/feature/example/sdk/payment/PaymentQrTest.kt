package com.primestrider.kotlinstarter.feature.example.sdk.payment

import com.google.zxing.common.BitMatrix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentQrTest {
    private fun BitMatrix.darkCount(): Int = (0 until width).sumOf { x -> (0 until height).count { y -> this[x, y] } }

    @Test
    fun encodes_a_square_matrix_with_dark_modules() {
        val matrix = paymentQrMatrix("0042 7310 0005", size = 256)
        assertEquals(256, matrix.width)
        assertEquals(256, matrix.height)
        assertTrue(matrix.darkCount() > 0)
    }

    @Test
    fun different_codes_give_different_matrices() {
        val first = paymentQrMatrix("0042 7310 0005", size = 256)
        val second = paymentQrMatrix("9999 1234 5678", size = 256)
        assertNotEquals(first, second)
    }
}
