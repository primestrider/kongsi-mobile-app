package com.primestrider.kotlinstarter.architecture

// remove-examples: exercises the example features, so scripts/remove-examples.sh deletes it with them.

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The Device APIs feature declares its permissions in its own manifest; the merged app manifest must keep them. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class DevicesManifestTest {
    @Test
    fun merged_manifest_keeps_device_permissions() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        @Suppress("DEPRECATION")
        val requested =
            context.packageManager
                .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
                .requestedPermissions
                .orEmpty()
                .toSet()
        val expected =
            setOf(
                "android.permission.INTERNET",
                "android.permission.POST_NOTIFICATIONS",
                "android.permission.READ_CALENDAR",
                "android.permission.WRITE_CALENDAR",
                "android.permission.READ_CONTACTS",
                "android.permission.ACCESS_COARSE_LOCATION",
                "android.permission.ACCESS_FINE_LOCATION",
                "android.permission.CAMERA",
                "android.permission.RECORD_AUDIO",
                "android.permission.USE_BIOMETRIC",
                "android.permission.DETECT_SCREEN_CAPTURE",
                "android.permission.FOREGROUND_SERVICE",
                "android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK",
            )
        assertTrue("missing ${expected - requested}", requested.containsAll(expected))
    }

    @Test
    fun music_service_is_declared_for_media_playback() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val service =
            context.packageManager.getServiceInfo(
                ComponentName(context.packageName, "com.primestrider.kotlinstarter.feature.example.sdk.music.MusicService"),
                0,
            )
        assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK, service.foregroundServiceType)
        // Media controllers (the system media controls, Bluetooth, Android Auto) bind to it from outside the app.
        assertTrue(service.exported)
        val sessionServices =
            context.packageManager.queryIntentServices(
                Intent("androidx.media3.session.MediaSessionService").setPackage(context.packageName),
                0,
            )
        assertTrue(sessionServices.any { it.serviceInfo.name.endsWith(".MusicService") })
    }

    @Test
    fun share_target_receives_text_and_images() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        listOf(
            Intent(Intent.ACTION_SEND).setType("text/plain"),
            Intent(Intent.ACTION_SEND).setType("image/png"),
            Intent(Intent.ACTION_SEND_MULTIPLE).setType("image/jpeg"),
        ).forEach { intent ->
            val handlers = context.packageManager.queryIntentActivities(intent.setPackage(context.packageName), 0)
            assertTrue("$intent", handlers.any { it.activityInfo.name.endsWith(".ShareReceiverActivity") })
        }
    }
}
