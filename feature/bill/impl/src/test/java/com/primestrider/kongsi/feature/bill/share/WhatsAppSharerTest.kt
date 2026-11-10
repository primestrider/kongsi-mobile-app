package com.primestrider.kongsi.feature.bill.share

import android.app.Application
import android.content.Intent
import android.content.pm.PackageInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class WhatsAppSharerTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val message = "Halo Budi, ini bagianmu:\n*Total: Rp41.580*"

    private fun install(name: String) {
        val info = PackageInfo().apply { packageName = name }
        shadowOf(app.packageManager).installPackage(info)
    }

    @Test
    fun a_number_opens_that_persons_whatsapp_chat_with_the_message() {
        install("com.whatsapp")

        val intent = WhatsAppSharer.intentFor(app, message, phone = "0812-3456-7890")

        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("com.whatsapp", intent.`package`)
        assertEquals("https://wa.me/6281234567890?text=Halo%20Budi%2C%20ini%20bagianmu%3A%0A*Total%3A%20Rp41.580*", intent.dataString)
    }

    @Test
    fun whatsapp_business_is_used_when_whatsapp_is_not_installed() {
        install("com.whatsapp.w4b")

        val intent = WhatsAppSharer.intentFor(app, message, phone = "6281234567890")

        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("com.whatsapp.w4b", intent.`package`)
    }

    @Test
    fun without_a_number_the_message_goes_to_whatsapp_to_pick_a_chat_or_group() {
        install("com.whatsapp")
        install("com.whatsapp.w4b")

        val intent = WhatsAppSharer.intentFor(app, message, phone = null)

        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("com.whatsapp", intent.`package`)
        assertEquals("text/plain", intent.type)
        assertEquals(message, intent.getStringExtra(Intent.EXTRA_TEXT))
    }

    @Test
    fun a_number_that_is_not_indonesian_mobile_is_sent_without_a_chat() {
        install("com.whatsapp")

        val intent = WhatsAppSharer.intentFor(app, message, phone = "12345")

        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("com.whatsapp", intent.`package`)
    }

    @Test
    fun without_whatsapp_the_system_share_sheet_opens() {
        val intent = WhatsAppSharer.intentFor(app, message, phone = "081234567890")

        assertEquals(Intent.ACTION_CHOOSER, intent.action)
        @Suppress("DEPRECATION")
        val target = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        assertEquals(Intent.ACTION_SEND, target.action)
        assertNull(target.`package`)
        assertEquals(message, target.getStringExtra(Intent.EXTRA_TEXT))
    }

    @Test
    fun sharing_starts_the_intent() {
        install("com.whatsapp")

        WhatsAppSharer.share(app, message, phone = "081234567890")

        val started = shadowOf(app).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, started.action)
        assertEquals("com.whatsapp", started.`package`)
    }
}
