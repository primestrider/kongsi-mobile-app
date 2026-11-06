package com.primestrider.kotlinstarter.feature.example.sdk.location

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class ForegroundWatchEffectTest {
    @get:Rule
    val composeRule = createComposeRule()

    private class TestOwner : LifecycleOwner {
        override val lifecycle = LifecycleRegistry.createUnsafe(this)
    }

    private val owner = TestOwner().apply { lifecycle.currentState = Lifecycle.State.RESUMED }
    private val events = mutableListOf<String>()
    private var watch by mutableStateOf(false)
    private var key by mutableStateOf("balanced")

    private fun setContent() =
        composeRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                val current = key
                ForegroundWatchEffect(watch, current) {
                    events += "start $current"
                    return@ForegroundWatchEffect { events += "stop $current" }
                }
            }
        }

    private fun moveTo(state: Lifecycle.State) {
        composeRule.runOnIdle { owner.lifecycle.currentState = state }
        composeRule.waitForIdle()
    }

    @Test
    fun nothing_runs_while_the_switch_is_off() {
        setContent()
        moveTo(Lifecycle.State.CREATED)
        moveTo(Lifecycle.State.RESUMED)
        assertEquals(emptyList<String>(), events)
    }

    @Test
    fun the_watch_stops_in_the_background_and_resumes_in_the_foreground() {
        setContent()
        composeRule.runOnIdle { watch = true }
        composeRule.waitForIdle()
        assertEquals(listOf("start balanced"), events)

        moveTo(Lifecycle.State.CREATED)
        assertEquals(listOf("start balanced", "stop balanced"), events)

        moveTo(Lifecycle.State.RESUMED)
        assertEquals(listOf("start balanced", "stop balanced", "start balanced"), events)
    }

    @Test
    fun a_new_key_restarts_and_switching_off_stops() {
        setContent()
        composeRule.runOnIdle { watch = true }
        composeRule.runOnIdle { key = "high" }
        composeRule.runOnIdle { watch = false }
        composeRule.waitForIdle()
        assertEquals(listOf("start balanced", "stop balanced", "start high", "stop high"), events)
    }
}
