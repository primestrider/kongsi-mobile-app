package com.primestrider.kotlinstarter.feature.example.sdk.payment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PaymentCodeTest {
    private val shape = Regex("""^\d{4} \d{4} \d{4}$""")

    @Test
    fun code_is_three_groups_of_four_digits() {
        val random = Random(42)
        repeat(200) { assertTrue(shape.matches(paymentCode(random))) }
    }

    @Test
    fun small_numbers_keep_their_leading_zeros() {
        val zeros =
            object : Random() {
                override fun nextBits(bitCount: Int) = 0
            }
        assertEquals("0000 0000 0000", paymentCode(zeros))
    }
}
