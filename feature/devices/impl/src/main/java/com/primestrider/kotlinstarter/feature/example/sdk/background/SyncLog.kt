package com.primestrider.kotlinstarter.feature.example.sdk.background

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "example_sync"
private const val LAST_RUN_KEY = "last_run"
private const val RUNS_KEY = "runs"

/**
 * What [SyncWorker] records about its runs, in `SharedPreferences("example_sync")`; the worker may run in a fresh
 * process. One instance per process (`@Singleton`), so [record]'s `@Synchronized` serialises concurrent workers.
 */
@Singleton
class SyncLog
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        /** Epoch milliseconds of the last run, or null before the first one. */
        fun lastRun(): Long? = if (prefs.contains(LAST_RUN_KEY)) prefs.getLong(LAST_RUN_KEY, 0L) else null

        fun runs(): Int = prefs.getInt(RUNS_KEY, 0)

        /** Stores [now] as the last run and adds one to the count; committed before the worker reports success. */
        @Synchronized
        fun record(now: Long) {
            prefs
                .edit()
                .putLong(LAST_RUN_KEY, now)
                .putInt(RUNS_KEY, runs() + 1)
                .commit()
        }
    }
