package com.primestrider.kotlinstarter.core.pip

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import com.primestrider.kotlinstarter.core.util.findActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What the activity shows while it is in picture-in-picture: a screen [set]s its content (a video surface, say) and
 * [clear]s it when it leaves or stops playing. `MainActivity` renders only this content, full screen, while in PiP.
 */
@Singleton
class PictureInPictureHost
    @Inject
    constructor() {
        private val current = MutableStateFlow<(@Composable () -> Unit)?>(null)
        val content: StateFlow<(@Composable () -> Unit)?> = current.asStateFlow()

        fun set(content: @Composable () -> Unit) {
            current.value = content
        }

        fun clear() {
            current.value = null
        }
    }

/** The app's [PictureInPictureHost], provided by `MainActivity`; the default is a detached host (previews, tests). */
val LocalPictureInPictureHost = staticCompositionLocalOf { PictureInPictureHost() }

/** Whether this device can show picture-in-picture: Android 8+ with the system feature (some low-RAM devices lack it). */
fun Context.supportsPictureInPicture(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

/** Enters 16:9 picture-in-picture; false when the device or the activity does not allow it. */
fun Activity.enterPictureInPicture(): Boolean {
    if (!supportsPictureInPicture()) return false
    return try {
        enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build())
    } catch (_: IllegalStateException) {
        false
    } catch (_: IllegalArgumentException) {
        false
    }
}

/** Follows the hosting activity's picture-in-picture mode. */
@Composable
fun rememberIsInPictureInPictureMode(): Boolean {
    val activity = LocalContext.current.findActivity() as? ComponentActivity ?: return false
    var inPictureInPicture by remember(activity) { mutableStateOf(activity.isInPictureInPictureMode) }
    DisposableEffect(activity) {
        val listener = Consumer<PictureInPictureModeChangedInfo> { inPictureInPicture = it.isInPictureInPictureMode }
        activity.addOnPictureInPictureModeChangedListener(listener)
        onDispose { activity.removeOnPictureInPictureModeChangedListener(listener) }
    }
    return inPictureInPicture
}

/**
 * Shows [content] normally; while [inPictureInPicture] with host content set, shows only that content. [content] stays
 * composed underneath (hidden, out of the semantics tree), so its navigation state and the screen that set the host
 * survive the round trip.
 */
@Composable
fun PictureInPictureFrame(
    host: PictureInPictureHost,
    inPictureInPicture: Boolean,
    content: @Composable () -> Unit,
) {
    val pipContent = host.content.collectAsState().value
    val pip = pipContent.takeIf { inPictureInPicture }
    Box(Modifier.fillMaxSize()) {
        // Same slot either way: only the modifier changes, so [content] is never recomposed from scratch.
        Box(if (pip != null) Modifier.fillMaxSize().hidden() else Modifier.fillMaxSize()) { content() }
        if (pip != null) {
            Box(Modifier.fillMaxSize().background(Color.Black)) { pip() }
        }
    }
}

private fun Modifier.hidden(): Modifier = clearAndSetSemantics {}.drawWithContent {}
