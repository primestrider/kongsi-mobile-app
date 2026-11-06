package com.primestrider.kotlinstarter.feature.example.sdk.media

import android.Manifest
import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class MediaContentTest {
    @get:Rule
    val rule = createComposeRule()

    private fun show(
        voice: VoiceNoteState = VoiceNoteState(),
        videoPlaying: Boolean = false,
        videoFailed: Boolean = false,
        pip: PipAvailability = PipAvailability.AVAILABLE,
        micGranted: Boolean = true,
    ) {
        if (micGranted) {
            shadowOf(
                ApplicationProvider.getApplicationContext<Application>(),
            ).grantPermissions(Manifest.permission.RECORD_AUDIO)
        }
        rule.setContent {
            AppTheme(false) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    MediaContent(
                        voice = voice,
                        videoPlaying = videoPlaying,
                        videoFailed = videoFailed,
                        pip = pip,
                        onRecord = {},
                        onPlay = {},
                        onVideo = {},
                        onPictureInPicture = {},
                        videoSurface = {},
                    )
                }
            }
        }
    }

    @Test
    fun without_the_microphone_only_the_voice_note_section_is_gated() {
        show(micGranted = false)
        rule.onNodeWithText("Microphone access is needed to record voice notes.").assertIsDisplayed()
        rule.onNodeWithText("Record").assertDoesNotExist()
        rule.onNodeWithText("Play video").assertIsDisplayed()
    }

    @Test
    fun play_is_disabled_until_a_note_exists() {
        show()
        rule.onNodeWithText("Play").assertIsNotEnabled()
        rule.onNodeWithText("Idle").assertIsDisplayed()
    }

    @Test
    fun while_recording_the_button_stops_and_play_stays_disabled() {
        show(VoiceNoteState(recording = true, recordedMillis = 61_500, hasNote = false))
        rule.onNodeWithText("Stop").assertIsDisplayed()
        rule.onNodeWithText("1:01").assertIsDisplayed()
        rule.onNodeWithText("Play").assertIsNotEnabled()
    }

    @Test
    fun a_note_can_be_played_and_shows_its_progress() {
        show(VoiceNoteState(hasNote = true, playing = true, positionMillis = 2_000, durationMillis = 5_000))
        rule.onNodeWithText("Pause").assertIsEnabled()
        rule.onNodeWithText("0:02 / 0:05").assertIsDisplayed()
    }

    @Test
    fun picture_in_picture_needs_a_playing_video() {
        show()
        rule.onNodeWithText("Picture-in-picture").assertIsNotEnabled()
    }

    @Test
    fun a_playing_video_offers_pause_and_picture_in_picture() {
        show(videoPlaying = true)
        rule.onNodeWithText("Pause video").assertIsDisplayed()
        rule.onNodeWithText("Picture-in-picture").assertIsEnabled()
    }

    @Test
    fun below_android_8_the_button_is_replaced_by_a_caption() {
        show(pip = PipAvailability.NEEDS_ANDROID_8)
        rule.onNodeWithText("Picture-in-picture").assertDoesNotExist()
        rule.onNodeWithText("Picture-in-picture needs Android 8.").assertIsDisplayed()
    }

    @Test
    fun a_failed_video_says_so_and_offers_play_again() {
        show(videoFailed = true)
        rule.onNodeWithText("Could not load the video.").assertIsDisplayed()
        rule.onNodeWithText("Play video").assertIsEnabled()
    }
}
