package com.primestrider.kotlinstarter.feature.example.sdk.camera

import android.view.OrientationEventListener
import android.view.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

/**
 * Maps an [OrientationEventListener] angle (degrees clockwise from the natural portrait position) to the
 * `Surface.ROTATION_*` that photos and analysis frames should use, or null when the angle is unknown (device
 * lying flat). Each rotation covers the 90° band centred on it, as in the CameraX documentation.
 */
fun surfaceRotationFor(orientationDegrees: Int): Int? {
    if (orientationDegrees == OrientationEventListener.ORIENTATION_UNKNOWN) return null
    return when (orientationDegrees.mod(360)) {
        in 45 until 135 -> Surface.ROTATION_270
        in 135 until 225 -> Surface.ROTATION_180
        in 225 until 315 -> Surface.ROTATION_90
        else -> Surface.ROTATION_0
    }
}

/**
 * Reports the device's physical rotation to [onRotation] while composed. MainActivity handles orientation
 * changes itself (no recreation), so the camera use cases would otherwise keep the rotation they were built
 * with and save sideways photos after the user turns the phone.
 */
@Composable
fun DeviceRotationEffect(onRotation: (Int) -> Unit) {
    val context = LocalContext.current
    val latest by rememberUpdatedState(onRotation)
    DisposableEffect(context) {
        val listener =
            object : OrientationEventListener(context) {
                override fun onOrientationChanged(orientation: Int) {
                    surfaceRotationFor(orientation)?.let { latest(it) }
                }
            }
        if (listener.canDetectOrientation()) listener.enable()
        onDispose { listener.disable() }
    }
}
