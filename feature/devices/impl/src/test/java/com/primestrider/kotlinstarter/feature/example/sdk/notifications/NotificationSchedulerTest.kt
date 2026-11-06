package com.primestrider.kotlinstarter.feature.example.sdk.notifications

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationSchedulerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private lateinit var scheduler: NotificationScheduler

    @Before
    fun setUp() {
        scheduler = NotificationScheduler(context)
        scheduler.cancelAll()
        manager.cancelAll()
    }

    private fun posted() = shadowOf(manager).allNotifications

    private fun title(notification: Notification) = notification.extras.getString(Notification.EXTRA_TITLE)

    @Test
    fun delay_zero_posts_one_notification_on_the_default_channel() {
        scheduler.schedule("Payment received", "Body", 0)
        val notification = posted().single()
        assertEquals("default", notification.channelId)
        assertEquals("Payment received", title(notification))
        assertEquals(0, scheduler.pending())
    }

    @Test
    fun delay_five_schedules_one_alarm_and_one_pending() {
        scheduler.schedule("Later", "Body", 5)
        assertNotNull(shadowOf(alarms).peekNextScheduledAlarm())
        assertEquals(1, scheduler.pending())
        assertTrue(posted().isEmpty())
    }

    @Test
    fun cancel_all_clears_pending_and_the_alarm() {
        scheduler.schedule("Later", "Body", 5)
        scheduler.schedule("Later still", "Body", 30)
        assertEquals(2, scheduler.pending())
        scheduler.cancelAll()
        assertEquals(0, scheduler.pending())
        assertNull(shadowOf(alarms).peekNextScheduledAlarm())
    }

    @Test
    fun blank_title_becomes_notification() {
        scheduler.schedule("   ", "Body", 0)
        assertEquals("Notification", title(posted().single()))
    }

    @Test
    fun the_receiver_posts_the_notification_and_forgets_the_pending_id() {
        val id = scheduler.schedule("Alarm", "Body", 5)
        val intent =
            Intent(context, NotificationReceiver::class.java)
                .putExtra(NotificationScheduler.EXTRA_ID, id)
                .putExtra(NotificationScheduler.EXTRA_TITLE, "Alarm")
                .putExtra(NotificationScheduler.EXTRA_BODY, "Body")
        NotificationReceiver().onReceive(context, intent)
        assertEquals("Alarm", title(posted().single()))
        assertEquals(0, scheduler.pending())
    }

    @Test
    fun foreground_posting_follows_the_switch() {
        assertTrue(shouldPost(foreground = false, showInForeground = false))
        assertTrue(shouldPost(foreground = true, showInForeground = true))
        assertFalse(shouldPost(foreground = true, showInForeground = false))
    }

    @Test
    fun a_tap_travels_in_the_deep_link_of_the_notification() {
        scheduler.schedule("Paid & done", "Body", 0)
        val pendingIntent = shadowOf(posted().single().contentIntent)
        val tap = pendingIntent.savedIntent
        assertEquals("kotlinstarter", tap.data?.scheme)
        assertEquals("/sdk/notifications", tap.data?.path)
        val (id, title) = NotificationScheduler.tapOf(tap)!!
        assertEquals("Paid & done", title)
        assertTrue(id > 0)
        assertNull(NotificationScheduler.tapOf(Intent(Intent.ACTION_VIEW)))
    }

    @Test
    fun the_foreground_switch_is_stored_for_the_receiver() {
        assertTrue(scheduler.showInForeground)
        scheduler.showInForeground = false
        assertFalse(NotificationScheduler(context).showInForeground)
        scheduler.showInForeground = true
    }

    @Test
    fun now_with_show_while_open_off_is_not_posted_but_reported_as_received() =
        runTest(UnconfinedTestDispatcher()) {
            val received = mutableListOf<String>()
            backgroundScope.launch { NotificationEvents.received.collect { received += it } }
            scheduler.showInForeground = false
            try {
                scheduler.schedule("Quiet", "Body", 0)
            } finally {
                scheduler.showInForeground = true
            }
            assertTrue(posted().isEmpty())
            assertEquals(listOf("Quiet"), received)
        }

    @Test
    fun now_with_show_while_open_on_is_posted_and_reported_as_received() =
        runTest(UnconfinedTestDispatcher()) {
            val received = mutableListOf<String>()
            backgroundScope.launch { NotificationEvents.received.collect { received += it } }
            scheduler.schedule("Loud", "Body", 0)
            assertEquals("Loud", title(posted().single()))
            assertEquals(listOf("Loud"), received)
        }
}
