package com.primestrider.kotlinstarter.feature.example.sdk.picker

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class PickedMediaTest {
    @Test
    fun kilobytes_have_one_decimal() {
        assertEquals("1.5 KB", formatKb(1536, Locale.US))
        assertEquals("0.0 KB", formatKb(0, Locale.US))
    }

    @Test
    fun kilobytes_follow_the_locale_separator() {
        assertEquals("1,5 KB", formatKb(1536, Locale.forLanguageTag("id")))
    }

    @Test
    fun dimensions_read_width_times_height() {
        val media = PickedMedia(Uri.EMPTY, "image/jpeg", width = 4000, height = 3000, bytes = 1)
        assertEquals("4000 × 3000", media.dimensions)
    }
}
