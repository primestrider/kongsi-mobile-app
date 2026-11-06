package com.primestrider.kotlinstarter.feature.example.sdk.music

import androidx.media3.common.Player
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class MusicSessionPolicyTest {
    private val all =
        Player.Commands
            .Builder()
            .addAllCommands()
            .build()

    @Test
    fun other_apps_can_control_playback_but_not_change_the_media() {
        val commands = allowedPlayerCommands(all, fromThisApp = false)
        assertTrue(commands.contains(Player.COMMAND_PLAY_PAUSE))
        assertTrue(commands.contains(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM))
        assertFalse(commands.contains(Player.COMMAND_SET_MEDIA_ITEM))
        assertFalse(commands.contains(Player.COMMAND_CHANGE_MEDIA_ITEMS))
    }

    @Test
    fun this_app_keeps_every_command() {
        assertTrue(allowedPlayerCommands(all, fromThisApp = true).contains(Player.COMMAND_SET_MEDIA_ITEM))
    }
}
