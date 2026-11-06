package com.primestrider.kotlinstarter.feature.example.sdk.camera

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The camera preview swallows touches, so in landscape (a ~360dp-tall window) a fixed 380dp preview left no place to
 * start a scroll and the Capture button was unreachable. The preview takes at most ~55% of the window height.
 */
class CameraPreviewHeightTest {
    @Test
    fun landscape_preview_leaves_room_to_scroll() {
        assertEquals(198.dp, cameraPreviewHeight(windowHeightDp = 360))
    }

    @Test
    fun portrait_preview_keeps_its_full_height() {
        assertEquals(380.dp, cameraPreviewHeight(windowHeightDp = 900))
    }
}
