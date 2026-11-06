package com.primestrider.kotlinstarter.feature.example.sdk.notifications

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class NotificationStatusTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    @Test
    @Config(sdk = [32])
    fun below_android_13_enabled_notifications_read_granted() {
        shadowOf(manager).setNotificationsEnabled(true)
        assertEquals(NotificationStatus.GRANTED_LEGACY, notificationStatus(context))
    }

    @Test
    @Config(sdk = [32])
    fun below_android_13_notifications_turned_off_in_settings_read_denied() {
        shadowOf(manager).setNotificationsEnabled(false)
        assertEquals(NotificationStatus.DENIED, notificationStatus(context))
    }

    @Test
    fun from_android_13_the_runtime_permission_decides() {
        assertEquals(NotificationStatus.NOT_REQUESTED, notificationStatus(context))
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertEquals(NotificationStatus.GRANTED, notificationStatus(context))
    }
}
