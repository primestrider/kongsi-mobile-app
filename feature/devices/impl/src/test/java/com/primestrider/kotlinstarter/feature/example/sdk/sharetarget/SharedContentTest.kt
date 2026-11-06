package com.primestrider.kotlinstarter.feature.example.sdk.sharetarget

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class SharedContentTest {
    private fun image(n: Int): Uri = "content://com.example.gallery/image/$n".toUri()

    @Test
    fun text_with_subject() {
        val intent =
            Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, "  https://example.org  ")
                .putExtra(Intent.EXTRA_SUBJECT, "A link")
        assertEquals(SharedContent(text = "https://example.org", subject = "A link"), SharedContent.from(intent))
    }

    @Test
    fun one_image() {
        val intent = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, image(1))
        assertEquals(SharedContent(images = listOf(image(1))), SharedContent.from(intent))
    }

    @Test
    fun several_images_are_capped_at_ten() {
        val uris = ArrayList((1..12).map(::image))
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).setType("image/*").putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        assertEquals((1..10).map(::image), SharedContent.from(intent).images)
    }

    @Test
    fun missing_extras_are_empty() {
        assertTrue(SharedContent.from(Intent(Intent.ACTION_SEND).setType("text/plain")).isEmpty)
        assertTrue(SharedContent.from(Intent(Intent.ACTION_SEND).setType("image/jpeg")).isEmpty)
    }

    @Test
    fun unsupported_type_or_action_is_empty() {
        assertTrue(
            SharedContent.from(Intent(Intent.ACTION_SEND).setType("application/pdf").putExtra(Intent.EXTRA_STREAM, image(1))).isEmpty,
        )
        assertTrue(SharedContent.from(Intent(Intent.ACTION_VIEW).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "hi")).isEmpty)
    }
}
