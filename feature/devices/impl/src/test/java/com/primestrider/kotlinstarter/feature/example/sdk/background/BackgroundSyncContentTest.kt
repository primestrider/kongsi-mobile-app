package com.primestrider.kotlinstarter.feature.example.sdk.background

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkInfo
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class BackgroundSyncContentTest {
    @get:Rule
    val rule = createComposeRule()

    private val toggles = mutableListOf<Boolean>()
    private var runs = 0

    private fun show(
        state: BackgroundSyncUiState,
        showRunNow: Boolean = true,
    ) {
        rule.setContent {
            AppTheme(false) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    BackgroundSyncContent(
                        state = state,
                        showRunNow = showRunNow,
                        onEnabled = { toggles += it },
                        onInterval = {},
                        onRefresh = {},
                        onRunNow = { runs++ },
                    )
                }
            }
        }
    }

    @Test
    fun before_any_run_the_status_says_never_and_the_switch_is_off() {
        show(BackgroundSyncUiState())
        rule.onNodeWithText("Background sync").assertIsOff().performClick()
        assertEquals(listOf(true), toggles)
        rule.onNodeWithText("Every 15 minutes").assertIsDisplayed()
        rule.onNodeWithText("Never").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun an_enqueued_work_shows_on_registered_and_its_runs() {
        show(BackgroundSyncUiState(intervalMinutes = 60, status = WorkInfo.State.ENQUEUED, registered = true, lastRun = 0L, runs = 3))
        rule.onNodeWithText("Background sync").assertIsOn()
        rule.onNodeWithText("Every hour").assertIsDisplayed()
        rule.onNodeWithText("Enqueued").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("3").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun run_now_and_its_caption_show_only_when_allowed() {
        show(BackgroundSyncUiState(), showRunNow = false)
        rule.onNodeWithText("Run now").assertDoesNotExist()
        rule.onNodeWithText("Run now is only shown in debug builds.").assertDoesNotExist()
        rule.onNodeWithText("Android runs periodic work at most every 15 minutes.").assertIsDisplayed()
    }

    @Test
    fun run_now_runs() {
        show(BackgroundSyncUiState())
        rule.onNodeWithText("Run now").performClick()
        assertEquals(1, runs)
        rule.onNodeWithText("Run now is only shown in debug builds.").assertIsDisplayed()
    }
}
