package com.primestrider.kotlinstarter.feature.example.sdk.blob

import org.junit.Assert.assertEquals
import org.junit.Test

class BinaryPayloadTest {
    private val initial = """{"trx":"TRX-001","amount":50000,"status":"PAID"}"""

    @Test
    fun size_counts_utf8_bytes() {
        assertEquals(2, BinaryPayload.of("é", "text/plain").size)
    }

    @Test
    fun text_reads_the_bytes_back() {
        assertEquals(initial, BinaryPayload.of(initial, "application/json").text())
    }

    @Test
    fun first_eight_bytes_as_hex() {
        assertEquals("7b 22 74 72 78 22 3a 22", BinaryPayload.of(initial, "application/json").slice(0, 8).hex())
    }

    @Test
    fun slice_beyond_the_end_clamps() {
        val slice = BinaryPayload.of("abc", "text/plain").slice(1, 99)
        assertEquals(2, slice.size)
        assertEquals("bc", slice.text())
        assertEquals(0, BinaryPayload.of("abc", "text/plain").slice(5, 9).size)
    }

    @Test
    fun combine_joins_payloads_and_strings() {
        val p = BinaryPayload.of(initial, "application/json")
        val combined = BinaryPayload.combine(listOf(p, "\n", p), "text/plain")
        assertEquals(2 * p.size + 1, combined.size)
        assertEquals("text/plain", combined.type)
    }
}
