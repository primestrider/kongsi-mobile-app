package com.primestrider.kotlinstarter.feature.example.sdk.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner

/** Whether an alarm that fired should show a notification: not while the app is open and the demo switch is off. */
internal fun shouldPost(
    foreground: Boolean,
    showInForeground: Boolean,
): Boolean = !foreground || showInForeground

/** Fires when a scheduled alarm is due: posts the notification, or only reports it when the app is open and the switch is off. */
class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val scheduler = NotificationScheduler(context)
        val id = intent.getIntExtra(NotificationScheduler.EXTRA_ID, -1)
        val title = intent.getStringExtra(NotificationScheduler.EXTRA_TITLE).orEmpty()
        val body = intent.getStringExtra(NotificationScheduler.EXTRA_BODY).orEmpty()
        scheduler.forget(id)
        val foreground =
            ProcessLifecycleOwner
                .get()
                .lifecycle.currentState
                .isAtLeast(Lifecycle.State.STARTED)
        if (shouldPost(foreground, scheduler.showInForeground)) scheduler.post(id, title, body)
        if (foreground) NotificationEvents.emit(title)
    }
}
