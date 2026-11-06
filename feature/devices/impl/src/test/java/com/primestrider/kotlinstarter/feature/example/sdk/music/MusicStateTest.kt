package com.primestrider.kotlinstarter.feature.example.sdk.music

import androidx.media3.common.C
import org.junit.Assert.assertEquals
import org.junit.Test

class MusicStateTest {
    @Test
    fun unknown_duration_is_zero_and_position_is_clamped() {
        assertEquals(
            MusicState(connected = true, playing = false, positionMs = 0, durationMs = 0),
            musicState(isPlaying = false, positionMs = 1_500, durationMs = C.TIME_UNSET),
        )
    }

    @Test
    fun playing_state_keeps_position_within_duration() {
        assertEquals(
            MusicState(connected = true, playing = true, positionMs = 4_000, durationMs = 10_000),
            musicState(isPlaying = true, positionMs = 4_000, durationMs = 10_000),
        )
        assertEquals(10_000, musicState(isPlaying = true, positionMs = 12_000, durationMs = 10_000).positionMs)
    }

    @Test
    fun progress_is_a_fraction_of_the_duration() {
        assertEquals(0.4f, MusicState(true, true, 4_000, 10_000).progress)
        assertEquals(0f, MusicState().progress)
    }

    @Test
    fun time_is_shown_as_minutes_and_seconds() {
        assertEquals("0:04", formatTime(4_200))
        assertEquals("1:05", formatTime(65_000))
    }
}
