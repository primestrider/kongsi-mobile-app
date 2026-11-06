package com.primestrider.kotlinstarter.feature.example.sdk.device

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** What the default network looks like right now. [validated] means the system confirmed it reaches the internet. */
data class NetworkSnapshot(
    val type: String,
    val connected: Boolean,
    val validated: Boolean,
)

fun NetworkCapabilities?.toSnapshot(): NetworkSnapshot {
    if (this == null) return NetworkSnapshot(type = "None", connected = false, validated = false)
    val type =
        when {
            hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            else -> "None"
        }
    return NetworkSnapshot(
        type = type,
        connected = hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
        validated = hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
    )
}

fun NetworkSnapshot.isOffline(): Boolean = !connected || !validated

/** The current default network, read once. */
fun ConnectivityManager.currentSnapshot(): NetworkSnapshot = getNetworkCapabilities(activeNetwork).toSnapshot()

/** Live default-network state: emits on every capability change and "None" when the network is lost. */
fun ConnectivityManager.snapshots(): Flow<NetworkSnapshot> =
    callbackFlow {
        val callback =
            object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities,
                ) {
                    trySend(networkCapabilities.toSnapshot())
                }

                override fun onLost(network: Network) {
                    trySend((null as NetworkCapabilities?).toSnapshot())
                }
            }
        registerDefaultNetworkCallback(callback)
        awaitClose { unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

internal fun Context.connectivityManager(): ConnectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
