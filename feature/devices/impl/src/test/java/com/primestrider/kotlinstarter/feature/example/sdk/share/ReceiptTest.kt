package com.primestrider.kotlinstarter.feature.example.sdk.share

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Locale

/**
 * Robolectric has no native PDF backend (`PdfDocument.startPage` throws "document is closed!"), so the rendering
 * itself is checked on a device; here we test what goes onto the page.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class ReceiptTest {
    @Test
    fun parse_amount_keeps_digits_only() {
        assertEquals(25_000L, parseAmount("Rp 25.000"))
    }

    @Test
    fun parse_amount_of_nothing_is_zero() {
        assertEquals(0L, parseAmount(""))
        assertEquals(0L, parseAmount("abc"))
    }

    @Test
    fun reference_is_trx_plus_upper_case_base_36() {
        val reference = receiptReference(1_700_000_000_000)
        assertTrue(reference.startsWith("TRX-"))
        assertEquals(reference, reference.uppercase())
        assertEquals(
            "TRX-" +
                java.lang.Long
                    .toString(1_700_000_000_000, 36)
                    .uppercase(),
            reference,
        )
    }

    @Test
    fun page_content_lists_merchant_reference_and_total() {
        val content = Receipt("Kopi Kenangan", 25_000, "TRX-1", 1_700_000_000_000).content(Locale.US, ReceiptLabels())

        assertEquals("Payment receipt", content.heading)
        assertEquals(listOf("Merchant" to "Kopi Kenangan", "Reference" to "TRX-1"), content.rows)
        assertEquals("Total", content.totalLabel)
        assertTrue(content.total.contains("25,000"))
        assertTrue(content.date.contains("2023"))
    }

    @Test
    fun page_content_uses_the_given_labels() {
        val labels = ReceiptLabels(heading = "Struk pembayaran", merchant = "Merchant", reference = "Referensi", total = "Total")
        val content = Receipt("PLN Token", 100_000, "TRX-2", 0).content(Locale.US, labels)

        assertEquals("Struk pembayaran", content.heading)
        assertEquals("Referensi", content.rows.last().first)
    }
}
