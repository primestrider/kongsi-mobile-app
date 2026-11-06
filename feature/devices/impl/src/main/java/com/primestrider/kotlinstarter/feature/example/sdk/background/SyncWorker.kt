package com.primestrider.kotlinstarter.feature.example.sdk.background

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlin.coroutines.cancellation.CancellationException

/** The demo's "sync": records the run in [SyncLog]. A real app would upload or fetch here. */
@HiltWorker
class SyncWorker internal constructor(
    context: Context,
    params: WorkerParameters,
    private val log: SyncLog,
    private val clock: () -> Long,
) : CoroutineWorker(context, params) {
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        log: SyncLog,
    ) : this(context, params, log, System::currentTimeMillis)

    override suspend fun doWork(): Result =
        try {
            log.record(clock())
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure()
        }
}
