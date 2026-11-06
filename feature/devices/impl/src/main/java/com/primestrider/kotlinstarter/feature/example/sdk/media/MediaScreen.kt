package com.primestrider.kotlinstarter.feature.example.sdk.media

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.primestrider.kotlinstarter.core.designsystem.component.AppButton
import com.primestrider.kotlinstarter.core.designsystem.component.Banner
import com.primestrider.kotlinstarter.core.designsystem.component.BannerVariant
import com.primestrider.kotlinstarter.core.designsystem.component.ButtonVariant
import com.primestrider.kotlinstarter.core.designsystem.component.InfoCard
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Radii
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.core.permission.PermissionGate
import com.primestrider.kotlinstarter.core.pip.LocalPictureInPictureHost
import com.primestrider.kotlinstarter.core.pip.enterPictureInPicture
import com.primestrider.kotlinstarter.core.pip.rememberIsInPictureInPictureMode
import com.primestrider.kotlinstarter.core.pip.supportsPictureInPicture
import com.primestrider.kotlinstarter.core.util.findActivity
import com.primestrider.kotlinstarter.feature.devices.impl.R
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold
import com.primestrider.kotlinstarter.feature.example.sdk.SdkSection
import kotlinx.coroutines.delay

// The reference URL (d23dyxeqlo5psv.cloudfront.net/big_buck_bunny.mp4) answers 403; this is the same film from the ExoPlayer test media.
private const val VIDEO_URL = "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4"
private const val TICK_MILLIS = 200L

/** Voice note section state. */
internal data class VoiceNoteState(
    val recording: Boolean = false,
    val recordedMillis: Long = 0,
    val hasNote: Boolean = false,
    val playing: Boolean = false,
    val positionMillis: Long = 0,
    val durationMillis: Long = 0,
    val failed: Boolean = false,
)

internal enum class PipAvailability { AVAILABLE, NEEDS_ANDROID_8, UNAVAILABLE }

/**
 * Voice notes (MediaRecorder + ExoPlayer) and a muted looping video (a second ExoPlayer) that can continue in
 * picture-in-picture. While the video plays, its surface is the [LocalPictureInPictureHost] content; leaving the screen
 * clears the host and releases the recorder and both players.
 */
@Composable
fun MediaScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val host = LocalPictureInPictureHost.current
    val inPictureInPicture = rememberIsInPictureInPictureMode()
    val recorder = remember { VoiceRecorder(context.applicationContext) }
    val audio = remember { ExoPlayer.Builder(context).build() }
    val video =
        remember {
            ExoPlayer.Builder(context).build().apply {
                repeatMode = Player.REPEAT_MODE_ONE
                volume = 0f
                setMediaItem(MediaItem.fromUri(VIDEO_URL))
            }
        }
    var voice by remember { mutableStateOf(VoiceNoteState()) }
    var videoPlaying by remember { mutableStateOf(false) }
    var videoFailed by remember { mutableStateOf(false) }
    var recordedMillis by remember { mutableLongStateOf(0L) }

    fun loadNote() {
        val note = recorder.file
        val hasNote = note.length() > 0
        if (hasNote) {
            audio.setMediaItem(MediaItem.fromUri(Uri.fromFile(note)))
            audio.prepare()
        }
        voice = voice.copy(hasNote = hasNote, positionMillis = 0, durationMillis = 0)
    }

    DisposableEffect(Unit) {
        loadNote()
        val audioListener =
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    voice = voice.copy(playing = isPlaying, positionMillis = audio.currentPosition)
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    val duration = audio.duration
                    if (duration != C.TIME_UNSET) voice = voice.copy(durationMillis = duration)
                }
            }
        val videoListener =
            object : Player.Listener {
                override fun onPlayWhenReadyChanged(
                    playWhenReady: Boolean,
                    reason: Int,
                ) {
                    videoPlaying = playWhenReady
                }

                override fun onPlayerError(error: PlaybackException) {
                    videoFailed = true
                    video.pause()
                }
            }
        audio.addListener(audioListener)
        video.addListener(videoListener)
        onDispose {
            // Review Focus 2: nothing of this screen outlives it.
            host.clear()
            recorder.release()
            audio.release()
            video.release()
        }
    }

    LaunchedEffect(voice.recording) {
        while (voice.recording) {
            recordedMillis = recorder.elapsedMillis()
            delay(TICK_MILLIS)
        }
    }
    LaunchedEffect(voice.playing) {
        while (voice.playing) {
            voice = voice.copy(positionMillis = audio.currentPosition)
            delay(TICK_MILLIS)
        }
    }
    // The PiP window shows the video only while it plays.
    LaunchedEffect(videoPlaying) {
        if (videoPlaying) host.set { VideoSurface(video, Modifier.fillMaxSize()) } else host.clear()
    }
    // Closing the PiP window or leaving the app stops everything; in PiP the activity is only paused.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        if (recorder.isRecording) {
            recorder.stop()
            voice = voice.copy(recording = false)
            loadNote()
        }
        audio.pause()
        video.pause()
    }

    val pip =
        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.O -> PipAvailability.NEEDS_ANDROID_8
            context.supportsPictureInPicture() -> PipAvailability.AVAILABLE
            else -> PipAvailability.UNAVAILABLE
        }

    ExamplePageScaffold(title = R.string.sdk_media_title, subtitle = R.string.sdk_media_subtitle, onBack = onBack) {
        MediaContent(
            voice = voice.copy(recordedMillis = recordedMillis),
            videoPlaying = videoPlaying,
            videoFailed = videoFailed,
            pip = pip,
            onRecord = {
                if (voice.recording) {
                    recorder.stop()
                    voice = voice.copy(recording = false)
                    loadNote()
                } else {
                    // The player reads the file that is about to be overwritten: let it go first.
                    audio.stop()
                    audio.clearMediaItems()
                    voice =
                        try {
                            recorder.start()
                            VoiceNoteState(recording = true)
                        } catch (e: Exception) {
                            VoiceNoteState(failed = true)
                        }
                }
            },
            onPlay = {
                if (voice.playing) {
                    audio.pause()
                } else {
                    // A finished note starts again from the beginning.
                    if (audio.playbackState == Player.STATE_ENDED) audio.seekTo(0)
                    audio.play()
                }
            },
            onVideo = {
                if (videoPlaying) {
                    video.pause()
                } else {
                    videoFailed = false
                    // IDLE before the first play and after an error: (re)load the stream.
                    if (video.playbackState == Player.STATE_IDLE) video.prepare()
                    video.play()
                }
            },
            onPictureInPicture = { context.findActivity()?.enterPictureInPicture() },
            videoSurface = { modifier ->
                // In PiP the host owns the video surface; this inline view lets go so the two do not compete.
                VideoSurface(if (inPictureInPicture) null else video, modifier)
            },
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoSurface(
    player: Player?,
    modifier: Modifier,
) {
    AndroidView(
        factory = {
            PlayerView(it).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                setShutterBackgroundColor(android.graphics.Color.BLACK)
            }
        },
        update = { it.player = player },
        onRelease = { it.player = null },
        // No background here: a SurfaceView shows through a hole in the window, so anything drawn over it hides the video.
        modifier = modifier,
    )
}

/** Both sections; [videoSurface] draws the video. Nothing here records or plays, the callbacks do. */
@Composable
internal fun MediaContent(
    voice: VoiceNoteState,
    videoPlaying: Boolean,
    videoFailed: Boolean = false,
    pip: PipAvailability,
    onRecord: () -> Unit,
    onPlay: () -> Unit,
    onVideo: () -> Unit,
    onPictureInPicture: () -> Unit,
    videoSurface: @Composable (Modifier) -> Unit,
) {
    SdkSection(R.string.sdk_media_voice_title, R.string.sdk_media_voice_description) {
        PermissionGate(listOf(Manifest.permission.RECORD_AUDIO), reason = stringResource(R.string.sdk_media_mic_reason)) {
            VoiceNote(voice, onRecord, onPlay)
        }
    }
    SdkSection(R.string.sdk_media_video_title, R.string.sdk_media_video_description) {
        videoSurface(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(Radii.md)))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            AppButton(
                stringResource(if (videoPlaying) R.string.sdk_media_pause_video else R.string.sdk_media_play_video),
                onClick = onVideo,
            )
            if (pip == PipAvailability.AVAILABLE) {
                AppButton(
                    stringResource(R.string.sdk_media_pip),
                    onClick = onPictureInPicture,
                    variant = ButtonVariant.OUTLINED,
                    enabled = videoPlaying,
                )
            }
        }
        if (videoFailed) Banner(title = stringResource(R.string.sdk_media_video_failed), variant = BannerVariant.ERROR)
        when (pip) {
            PipAvailability.NEEDS_ANDROID_8 -> Caption(stringResource(R.string.sdk_media_pip_needs_api))
            PipAvailability.UNAVAILABLE -> Caption(stringResource(R.string.sdk_media_pip_unavailable))
            PipAvailability.AVAILABLE -> Unit
        }
    }
}

@Composable
private fun VoiceNote(
    voice: VoiceNoteState,
    onRecord: () -> Unit,
    onPlay: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            AppButton(stringResource(if (voice.recording) R.string.sdk_media_stop else R.string.sdk_media_record), onClick = onRecord)
            AppButton(
                stringResource(if (voice.playing) R.string.sdk_media_pause else R.string.sdk_media_play),
                onClick = onPlay,
                variant = ButtonVariant.OUTLINED,
                enabled = voice.hasNote && !voice.recording,
            )
        }
        if (voice.failed) Banner(title = stringResource(R.string.sdk_media_record_failed), variant = BannerVariant.ERROR)
        InfoCard(
            listOf(
                stringResource(R.string.sdk_media_recording) to
                    if (voice.recording) formatClock(voice.recordedMillis) else stringResource(R.string.sdk_media_idle),
                stringResource(R.string.sdk_media_playback) to
                    if (voice.hasNote) "${formatClock(voice.positionMillis)} / ${formatClock(voice.durationMillis)}" else null,
            ),
        )
    }
}

@Composable
private fun Caption(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@ThemePreviews
@Composable
private fun MediaScreenPreview() {
    PreviewTheme {
        ExamplePageScaffold(title = R.string.sdk_media_title, subtitle = R.string.sdk_media_subtitle, onBack = {}) {
            MediaContent(
                voice = VoiceNoteState(hasNote = true, durationMillis = 4_000),
                videoPlaying = false,
                pip = PipAvailability.AVAILABLE,
                onRecord = {},
                onPlay = {},
                onVideo = {},
                onPictureInPicture = {},
                videoSurface = { modifier -> Box(modifier) },
            )
        }
    }
}
