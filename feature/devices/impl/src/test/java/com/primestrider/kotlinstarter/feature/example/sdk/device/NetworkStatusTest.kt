package com.primestrider.kotlinstarter.feature.example.sdk.device

import android.net.NetworkCapabilities
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNetworkCapabilities

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class NetworkStatusTest {
    private fun caps(
        transport: Int?,
        vararg capabilities: Int,
    ): NetworkCapabilities {
        val c = ShadowNetworkCapabilities.newInstance()
        val shadow = org.robolectric.Shadows.shadowOf(c)
        capabilities.forEach { shadow.addCapability(it) }
        transport?.let { shadow.addTransportType(it) }
        return c
    }

    @Test
    fun null_capabilities_means_none_and_offline() {
        val snapshot = (null as NetworkCapabilities?).toSnapshot()
        assertEquals("None", snapshot.type)
        assertFalse(snapshot.connected)
        assertTrue(snapshot.isOffline())
    }

    @Test
    fun wifi_with_validated_internet_is_online() {
        val snapshot =
            caps(
                NetworkCapabilities.TRANSPORT_WIFI,
                NetworkCapabilities.NET_CAPABILITY_INTERNET,
                NetworkCapabilities.NET_CAPABILITY_VALIDATED,
            ).toSnapshot()
        assertEquals("Wi-Fi", snapshot.type)
        assertFalse(snapshot.isOffline())
    }

    @Test
    fun internet_without_validation_is_offline() {
        val snapshot = caps(NetworkCapabilities.TRANSPORT_CELLULAR, NetworkCapabilities.NET_CAPABILITY_INTERNET).toSnapshot()
        assertEquals("Cellular", snapshot.type)
        assertTrue(snapshot.connected)
        assertFalse(snapshot.validated)
        assertTrue(snapshot.isOffline())
    }

    @Test
    fun other_transports_are_named() {
        assertEquals("Ethernet", caps(NetworkCapabilities.TRANSPORT_ETHERNET).toSnapshot().type)
        assertEquals("VPN", caps(NetworkCapabilities.TRANSPORT_VPN).toSnapshot().type)
    }
}
