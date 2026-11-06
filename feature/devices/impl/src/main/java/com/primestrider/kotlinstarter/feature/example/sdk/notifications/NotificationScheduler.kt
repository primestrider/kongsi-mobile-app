package com.primestrider.kotlinstarter.feature.example.sdk.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import com.primestrider.kotlinstarter.core.common.AppConfig
import com.primestrider.kotlinstarter.feature.devices.impl.R

private const val PREFS_NAME = "sdk_notifications"
private const val PENDING_KEY = "pending"
private const val NEXT_ID_KEY = "next_id"
private const val SHOW_IN_FOREGROUND_KEY = "show_in_foreground"
const val NOTIFICATION_CHANNEL_ID = "default"

/** The body for a notification that is sent after [delaySeconds] (0 = immediately). */
fun notificationBody(
    context: Context,
    delaySeconds: Int,
): String =
    if (delaySeconds > 0) {
        context.getString(R.string.sdk_notifications_body_scheduled, delaySeconds)
    } else {
        context.getString(R.string.sdk_notifications_body_now)
    }

/**
 * Posts local notifications now, or later through [AlarmManager] (inexact, so no exact-alarm permission).
 * The ids of the notifications still waiting are kept in `SharedPreferences`, so [pending] survives
 * rotation and process death; an id whose alarm no longer exists (device restart) is dropped.
 */
class NotificationScheduler(
    context: Context,
) {
    private val context = context.applicationContext
    private val prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val alarms = this.context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /** Read by [NotificationReceiver], hence in preferences rather than in a ViewModel. */
    var showInForeground: Boolean
        get() = prefs.getBoolean(SHOW_IN_FOREGROUND_KEY, true)
        set(value) = prefs.edit { putBoolean(SHOW_IN_FOREGROUND_KEY, value) }

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel =
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.sdk_notifications_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            )
        manager.createNotificationChannel(channel)
    }

    /**
     * Posts now (delay 0) or in [delaySeconds]; returns the notification id. A blank [title] becomes "Notification".
     * Delay 0 is treated like an alarm arriving while the app is open: it is shown only when [showInForeground] is on,
     * and reported to [NotificationEvents] either way.
     */
    fun schedule(
        title: String,
        body: String,
        delaySeconds: Int,
    ): Int {
        val shownTitle = title.trim().ifEmpty { context.getString(R.string.sdk_notifications_default_title) }
        val id = nextId()
        if (delaySeconds <= 0) {
            // "Now" is sent from the open app, so it arrives in the foreground: same rule as the receiver.
            if (shouldPost(foreground = true, showInForeground = showInForeground)) post(id, shownTitle, body)
            NotificationEvents.emit(shownTitle)
            return id
        }
        prefs.edit { putStringSet(PENDING_KEY, pendingIds() + id.toString()) }
        val intent = alarmIntent(id, shownTitle, body, PendingIntent.FLAG_UPDATE_CURRENT) ?: return id
        alarms.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delaySeconds * 1_000L, intent)
        return id
    }

    /** How many scheduled notifications have not been shown yet. */
    fun pending(): Int {
        val ids = pendingIds()
        val alive = ids.filter { alarmIntent(it.toInt(), "", "", PendingIntent.FLAG_NO_CREATE) != null }.toSet()
        if (alive.size != ids.size) prefs.edit { putStringSet(PENDING_KEY, alive) }
        return alive.size
    }

    fun cancelAll() {
        pendingIds().forEach { id ->
            alarmIntent(id.toInt(), "", "", PendingIntent.FLAG_NO_CREATE)?.let {
                alarms.cancel(it)
                it.cancel()
            }
        }
        prefs.edit { remove(PENDING_KEY) }
    }

    /** Removes [id] from the pending set once its alarm has fired. */
    internal fun forget(id: Int) {
        prefs.edit { putStringSet(PENDING_KEY, pendingIds() - id.toString()) }
        alarmIntent(id, "", "", PendingIntent.FLAG_NO_CREATE)?.cancel()
    }

    internal fun post(
        id: Int,
        title: String,
        body: String,
    ) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        ensureChannel()
        val tap =
            Intent(Intent.ACTION_VIEW, tapUri(AppConfig.appScheme(context), id, title)).setPackage(context.packageName)
        val content = PendingIntent.getActivity(context, id, tap, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification =
            NotificationCompat
                .Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.sdk_ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(content)
                .setAutoCancel(true)
                .build()
        // The areNotificationsEnabled() check above is the runtime-permission check that lint cannot see.
        @Suppress("MissingPermission")
        manager.notify(id, notification)
    }

    private fun alarmIntent(
        id: Int,
        title: String,
        body: String,
        flag: Int,
    ): PendingIntent? {
        val intent =
            Intent(context, NotificationReceiver::class.java)
                .putExtra(EXTRA_ID, id)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_BODY, body)
        return PendingIntent.getBroadcast(context, id, intent, flag or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun pendingIds(): Set<String> = prefs.getStringSet(PENDING_KEY, emptySet()).orEmpty()

    private fun nextId(): Int {
        val id = prefs.getInt(NEXT_ID_KEY, 1)
        prefs.edit { putInt(NEXT_ID_KEY, id + 1) }
        return id
    }

    companion object {
        private const val TAPPED_PARAM = "tapped"
        private const val TITLE_PARAM = "title"

        // The tap travels in the deep link itself: NavController.handleDeepLink may rewrite the intent extras.
        internal fun tapUri(
            scheme: String,
            id: Int,
            title: String,
        ): Uri =
            Uri
                .parse("$scheme://example/sdk/notifications")
                .buildUpon()
                .appendQueryParameter(TAPPED_PARAM, id.toString())
                .appendQueryParameter(TITLE_PARAM, title)
                .build()

        /** The (notification id, title) of a tap on a notification of this demo, or null for any other intent. */
        fun tapOf(intent: Intent): Pair<Int, String>? {
            val data = intent.data ?: return null
            val id = data.getQueryParameter(TAPPED_PARAM)?.toIntOrNull() ?: return null
            return id to data.getQueryParameter(TITLE_PARAM).orEmpty()
        }

        const val EXTRA_ID = "notification_id"
        const val EXTRA_TITLE = "notification_title"
        const val EXTRA_BODY = "notification_body"
    }
}
