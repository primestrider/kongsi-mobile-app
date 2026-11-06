package com.primestrider.kotlinstarter.feature.example.sdk.music

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.primestrider.kotlinstarter.core.designsystem.component.AppButton
import com.primestrider.kotlinstarter.core.designsystem.component.ButtonVariant
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.feature.devices.impl.R
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold
import com.primestrider.kotlinstarter.feature.example.sdk.SdkSection

/** Background music: a media playback foreground service controlled through a `MediaController`. */
@Composable
fun MusicScreen(onBack: () -> Unit) {
    val music = rememberMusicController()
    val state by music.state
    MusicContent(state = state, onPlay = music::play, onPause = music::pause, onStop = music::stop, onBack = onBack)
}

@Composable
internal fun MusicContent(
    state: MusicState,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onBack: () -> Unit,
) {
    ExamplePageScaffold(title = R.string.sdk_music_title, subtitle = R.string.sdk_music_subtitle, onBack = onBack) {
        SdkSection(R.string.sdk_music_player_title, R.string.sdk_music_player_description) {
            Text(stringResource(R.string.sdk_music_track_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.sdk_music_track_artist),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
            Text(
                "${formatTime(state.positionMs)} / ${formatTime(state.durationMs)}",
                style = MaterialTheme.typography.labelMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                if (state.playing) {
                    AppButton(stringResource(R.string.sdk_music_pause), onClick = onPause)
                } else {
                    AppButton(
                        stringResource(R.string.sdk_music_play),
                        onClick = onPlay,
                        enabled = state.connected,
                    )
                }
                AppButton(
                    stringResource(R.string.sdk_music_stop),
                    onClick = onStop,
                    variant = ButtonVariant.OUTLINED,
                    enabled = state.connected,
                )
            }
        }
        SdkSection(R.string.sdk_music_background_title, R.string.sdk_music_background_description) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Text(stringResource(R.string.sdk_music_notification_note), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@ThemePreviews
@Composable
private fun MusicScreenPreview() {
    PreviewTheme {
        MusicContent(
            state = MusicState(connected = true, playing = true, positionMs = 4_000, durationMs = 10_000),
            onPlay = {},
            onPause = {},
            onStop = {},
            onBack = {},
        )
    }
}
