package com.primestrider.kotlinstarter.feature.example.sdk.notifications

import android.Manifest
import android.app.Application
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class NotificationsContentTest {
    @get:Rule
    val rule = createComposeRule()

    private var scheduled = 0

    private fun show(
        status: NotificationStatus,
        events: List<String> = emptyList(),
    ) = rule.setContent {
        AppTheme(false) {
            androidx.compose.foundation.layout.Column(Modifier.verticalScroll(rememberScrollState())) {
                NotificationsContent(
                    state = NotificationsUiState(events = events),
                    status = status,
                    pending = 2,
                    showInForeground = true,
                    onTitle = {},
                    onDelay = {},
                    onShowInForeground = {},
                    onSchedule = { scheduled++ },
                    onCancelAll = {},
                )
            }
        }
    }

    @Test
    fun before_the_grant_the_form_and_events_show_and_schedule_is_disabled() {
        show(NotificationStatus.NOT_REQUESTED)
        rule.onNodeWithText("not requested").assertIsDisplayed()
        rule.onNodeWithText("Notifications need your permission to appear.").assertIsDisplayed()
        rule.onNodeWithText("Grant access").assertIsDisplayed()
        rule.onNodeWithText("Payment received").assertIsDisplayed()
        rule.onNodeWithText("No events yet.").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Schedule").assertIsNotEnabled()
        rule.onNodeWithText("Cancel all").assertIsEnabled()
        rule.onNodeWithText("2 pending").assertIsDisplayed()
    }

    @Test
    fun denied_has_its_own_status() {
        show(NotificationStatus.DENIED)
        rule.onNodeWithText("denied").assertIsDisplayed()
        rule.onNodeWithText("Schedule").assertIsNotEnabled()
    }

    @Test
    fun filled_events_replace_the_empty_message() {
        show(NotificationStatus.NOT_REQUESTED, events = listOf("9:00:05 AM · Received: Hi", "9:00:00 AM · Sent now"))
        rule.onNodeWithText("9:00:05 AM · Received: Hi").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("9:00:00 AM · Sent now").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("No events yet.").assertDoesNotExist()
    }

    @Test
    @Config(sdk = [32])
    fun below_android_13_the_status_reads_granted_and_nothing_is_asked() {
        show(NotificationStatus.GRANTED_LEGACY)
        rule.onNodeWithText("granted (not needed before Android 13)").assertIsDisplayed()
        rule.onNodeWithText("Grant access").assertDoesNotExist()
        rule
            .onNodeWithText("Schedule")
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        assertEquals(1, scheduled)
    }

    @Test
    fun granted_enables_schedule_and_hides_the_grant_card() {
        // The gate under the status reads the real permission: grant it, as the status says.
        shadowOf(ApplicationProvider.getApplicationContext<Application>()).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        show(NotificationStatus.GRANTED)
        rule.onNodeWithText("granted").assertIsDisplayed()
        rule.onNodeWithText("Schedule").assertIsEnabled()
        rule.onNodeWithText("Grant access").assertDoesNotExist()
        rule.onNodeWithText("Notifications need your permission to appear.").assertDoesNotExist()
    }
}
