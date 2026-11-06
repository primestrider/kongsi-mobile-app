package com.primestrider.kotlinstarter.core.crash

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "crash"
private const val KEY_CLASS = "class"
private const val KEY_MESSAGE = "message"
private const val KEY_TIME = "time"
private const val MESSAGE_MAX = 500

data class CrashReport(
    val exceptionClass: String,
    val message: String?,
    val timestamp: Long,
)

/**
 * Remembers the last uncaught exception so the next launch can show a fallback screen.
 *
 * It does not keep the process alive: after recording, the previous handler always runs, so the system (and any
 * crash reporter installed before) still sees the crash and kills the process. Only the class name, a truncated
 * message and the time are kept; no stack trace.
 */
@Singleton
class CrashRecorder(
    private val preferences: () -> SharedPreferences,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : this({ context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) })

    private var installed = false

    /** Wraps the current default handler; idempotent. Called from `KotlinStarterApp.attachBaseContext`. */
    fun install() {
        if (installed) return
        installed = true
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            record(error)
            previous?.uncaughtException(thread, error)
        }
    }

    fun lastCrash(): CrashReport? =
        try {
            val prefs = preferences()
            prefs.getString(KEY_CLASS, null)?.let { CrashReport(it, prefs.getString(KEY_MESSAGE, null), prefs.getLong(KEY_TIME, 0)) }
        } catch (e: Exception) {
            // Unreadable crash data must never stop the app from starting.
            null
        }

    fun clear() {
        try {
            preferences().edit().clear().apply()
        } catch (e: Exception) {
            // Nothing to clear if the store cannot be opened.
        }
    }

    private fun record(error: Throwable) {
        try {
            preferences()
                .edit()
                .putString(KEY_CLASS, error.javaClass.name)
                .putString(KEY_MESSAGE, error.message?.take(MESSAGE_MAX))
                .putLong(KEY_TIME, clock())
                .commit() // synchronous: the process is about to die
        } catch (t: Throwable) {
            // Recording is best effort; the crash itself must still reach the previous handler.
        }
    }
}
