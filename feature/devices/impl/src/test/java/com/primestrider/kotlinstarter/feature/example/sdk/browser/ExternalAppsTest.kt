package com.primestrider.kotlinstarter.feature.example.sdk.browser

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class ExternalAppsTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun returns_false_when_no_activity_can_handle_the_uri() {
        shadowOf(app).checkActivities(true)
        assertFalse(openExternal(app, ExternalApp.CALL))
    }

    @Test
    fun starts_the_uri_when_an_activity_resolves_it() {
        val dialer = ComponentName("dialer", "dialer.Dialer")
        shadowOf(app.packageManager).addActivityIfNotPresent(dialer)
        shadowOf(app.packageManager).addIntentFilterForActivity(
            dialer,
            IntentFilter(Intent.ACTION_VIEW).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                addDataScheme("tel")
            },
        )
        shadowOf(app).checkActivities(true)

        assertTrue(openExternal(app, ExternalApp.CALL))
        assertEquals(ExternalApp.CALL.uri.toUri(), shadowOf(app).nextStartedActivity.data)
    }

    @Test
    fun uris_match_the_reference() {
        assertEquals("https://wa.me/6281234567890?text=Halo", ExternalApp.WHATSAPP.uri)
        assertEquals("tel:+6281234567890", ExternalApp.CALL.uri)
        assertEquals("mailto:support@example.com?subject=Help", ExternalApp.EMAIL.uri)
        assertEquals("geo:0,0?q=Monas%20Jakarta", ExternalApp.MAPS.uri)
    }

    @Test
    fun payment_deep_link_uses_the_scheme() {
        assertEquals("kotlinstarter://example/sdk/payment-code?amount=50000", paymentDeepLink("kotlinstarter"))
    }
}
