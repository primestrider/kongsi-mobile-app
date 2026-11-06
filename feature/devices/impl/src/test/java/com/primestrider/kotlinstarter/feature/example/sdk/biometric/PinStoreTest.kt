package com.primestrider.kotlinstarter.feature.example.sdk.biometric

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Robolectric has no AndroidKeyStore: only the preferences layout behind [PinVault] is tested here. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class PinStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val store = PrefsPinStore(context)

    @Test
    fun an_empty_store_has_no_pin() {
        assertNull(store.read())
    }

    @Test
    fun ciphertext_and_iv_round_trip_through_example_pin_preferences() {
        store.write(StoredPin(byteArrayOf(1, 2, 3), byteArrayOf(9, 8), allowsDeviceCredential = true))
        val read = PrefsPinStore(context).read()!!
        assertArrayEquals(byteArrayOf(1, 2, 3), read.ciphertext)
        assertArrayEquals(byteArrayOf(9, 8), read.iv)
        assertTrue(read.allowsDeviceCredential)
        val prefs = context.getSharedPreferences("example_pin", Context.MODE_PRIVATE)
        assertEquals(setOf("ciphertext", "iv", "device_credential"), prefs.all.keys)
    }

    @Test
    fun clear_removes_everything() {
        store.write(StoredPin(byteArrayOf(1), byteArrayOf(2), allowsDeviceCredential = false))
        store.clear()
        assertNull(store.read())
        assertFalse(context.getSharedPreferences("example_pin", Context.MODE_PRIVATE).all.isNotEmpty())
    }
}
