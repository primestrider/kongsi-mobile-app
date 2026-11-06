package com.primestrider.kotlinstarter.feature.example.sdk.camera

import androidx.camera.view.PreviewView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class CameraPreviewTest {
    @Test
    fun the_preview_draws_into_a_texture_view_so_page_transitions_fade_it() {
        val view = cameraPreviewView(ApplicationProvider.getApplicationContext())
        assertEquals(PreviewView.ImplementationMode.COMPATIBLE, view.implementationMode)
    }
}
