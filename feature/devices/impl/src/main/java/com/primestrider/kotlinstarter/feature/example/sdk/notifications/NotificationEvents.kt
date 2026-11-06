package com.primestrider.kotlinstarter.feature.example.sdk.notifications

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/** Title of a scheduled notification that arrived while the app was in the foreground; the receiver emits, the demo logs it. */
object NotificationEvents {
    private val mutableReceived = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val received: SharedFlow<String> = mutableReceived

    private var lastTapped = -1

    /** The event log of the latest demo screen, kept for the next one (main thread only). */
    internal val history = ArrayList<String>()

    internal fun reset() {
        history.clear()
        lastTapped = -1
    }

    /** True the first time notification [id] is tapped; the launch intent is seen again whenever the demo reopens. */
    internal fun firstTap(id: Int): Boolean = (id != lastTapped).also { lastTapped = id }

    internal fun emit(title: String) {
        mutableReceived.tryEmit(title)
    }
}
