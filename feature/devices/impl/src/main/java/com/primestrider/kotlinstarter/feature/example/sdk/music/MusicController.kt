package com.primestrider.kotlinstarter.feature.example.sdk.music

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.delay

private const val POSITION_TICK_MS = 500L

/** What the music screen shows. */
data class MusicState(
    val connected: Boolean = false,
    val playing: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
) {
    val progress: Float get() = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
}

/** Maps a connected player's numbers to [MusicState]: an unknown duration is 0, the position stays inside it. */
fun musicState(
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
): MusicState {
    val duration = if (durationMs == C.TIME_UNSET || durationMs < 0) 0 else durationMs
    return MusicState(connected = true, playing = isPlaying, positionMs = positionMs.coerceIn(0, duration), durationMs = duration)
}

/** `m:ss`, for the position and length of the track. */
fun formatTime(ms: Long): String {
    val seconds = ms / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}

/**
 * A `MediaController` connected to [MusicService] while this composable is on screen. Connecting starts the service;
 * releasing the controller only unbinds, so music that is playing keeps going after the screen closes.
 */
class MusicController internal constructor(
    private val controller: () -> MediaController?,
    val state: State<MusicState>,
) {
    fun play() {
        controller()?.play()
    }

    fun pause() {
        controller()?.pause()
    }

    /** Pause and rewind; the service then leaves the foreground and its notification can be dismissed. */
    fun stop() {
        controller()?.run {
            pause()
            seekTo(0)
        }
    }
}

@Composable
fun rememberMusicController(): MusicController {
    val context = LocalContext.current
    var controller by remember { mutableStateOf<MediaController?>(null) }
    val state = remember { mutableStateOf(MusicState()) }

    DisposableEffect(context) {
        val future = MediaController.Builder(context, sessionToken(context)).buildAsync()
        // A failed connection (no service, binding refused) leaves the controls disabled instead of crashing.
        future.addListener(
            { if (!future.isCancelled) controller = runCatching { future.get() }.getOrNull() },
            ContextCompat.getMainExecutor(context),
        )
        onDispose {
            MediaController.releaseFuture(future)
            controller = null
        }
    }

    // Playing/paused comes from the listener; the position is polled while connected.
    LaunchedEffect(controller) {
        val connected = controller ?: return@LaunchedEffect
        connected.addListener(
            object : Player.Listener {
                override fun onEvents(
                    player: Player,
                    events: Player.Events,
                ) {
                    state.value = musicState(player.isPlaying, player.currentPosition, player.duration)
                }
            },
        )
        while (true) {
            state.value = musicState(connected.isPlaying, connected.currentPosition, connected.duration)
            delay(POSITION_TICK_MS)
        }
    }
    return remember { MusicController({ controller }, state) }
}

private fun sessionToken(context: Context) = SessionToken(context, ComponentName(context, MusicService::class.java))
