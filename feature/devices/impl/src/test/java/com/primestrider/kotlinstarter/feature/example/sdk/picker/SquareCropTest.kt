package com.primestrider.kotlinstarter.feature.example.sdk.picker

import android.graphics.Rect
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class SquareCropTest {
    @Test
    fun landscape_is_cropped_from_the_sides() {
        assertEquals(Rect(500, 0, 3500, 3000), squareCropRect(4000, 3000))
    }

    @Test
    fun portrait_is_cropped_from_top_and_bottom() {
        assertEquals(Rect(0, 500, 3000, 3500), squareCropRect(3000, 4000))
    }

    @Test
    fun a_square_is_kept_whole() {
        assertEquals(Rect(0, 0, 100, 100), squareCropRect(100, 100))
    }
}
