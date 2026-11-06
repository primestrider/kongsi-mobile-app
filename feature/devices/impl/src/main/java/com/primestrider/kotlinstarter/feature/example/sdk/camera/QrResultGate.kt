package com.primestrider.kotlinstarter.feature.example.sdk.camera

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Lets one scanned QR value through at a time. While a value is [current] (the result sheet is open), every
 * further frame, of the same or another code, is ignored until [clear] is called; so a code the camera keeps
 * seeing opens exactly one sheet. Holds nothing but state: keep the instance in a ViewModel, never rebuild it
 * on recomposition.
 */
class QrResultGate {
    /** The value the open result sheet shows, or null while scanning is free to accept one. */
    var current: String? by mutableStateOf(null)
        private set

    /** Only the scan mode accepts codes; the photo mode sets this to false. */
    var scanning: Boolean by mutableStateOf(true)

    /** Accepts [value] when scanning, nothing is showing and the value is not blank; returns whether it was accepted. */
    fun offer(value: String): Boolean {
        if (!scanning || current != null || value.isBlank()) return false
        current = value
        return true
    }

    /** Closes the result: the next [offer] is accepted again. */
    fun clear() {
        current = null
    }
}
