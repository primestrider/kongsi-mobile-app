package com.primestrider.kotlinstarter.feature.example.sdk.camera

import android.view.OrientationEventListener
import android.view.Surface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CameraRotationTest {
    @Test
    fun upright_and_near_upright_angles_are_rotation_0() {
        listOf(0, 10, 44, 315, 359, 360).forEach { assertEquals("angle $it", Surface.ROTATION_0, surfaceRotationFor(it)) }
    }

    @Test
    fun turning_the_phone_clockwise_maps_to_the_opposite_surface_rotation() {
        // Turned clockwise onto its right edge, the display content rotates 270°.
        assertEquals(Surface.ROTATION_270, surfaceRotationFor(45))
        assertEquals(Surface.ROTATION_270, surfaceRotationFor(90))
        assertEquals(Surface.ROTATION_180, surfaceRotationFor(135))
        assertEquals(Surface.ROTATION_180, surfaceRotationFor(180))
        assertEquals(Surface.ROTATION_90, surfaceRotationFor(225))
        assertEquals(Surface.ROTATION_90, surfaceRotationFor(270))
        assertEquals(Surface.ROTATION_90, surfaceRotationFor(314))
    }

    @Test
    fun an_unknown_angle_keeps_the_previous_rotation() {
        assertNull(surfaceRotationFor(OrientationEventListener.ORIENTATION_UNKNOWN))
    }
}
