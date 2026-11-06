package com.primestrider.kotlinstarter.feature.example.sdk.background

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.primestrider.kotlinstarter.feature.devices.impl.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** Unique name of the demo's periodic work; cancel it when removing the example (docs/examples.md). */
const val SYNC_WORK_NAME = "example-background-sync"
private const val RUN_NOW_TAG = "example-background-sync-now"

private fun WorkInfo.intervalMinutes(): Int? = periodicityInfo?.let { TimeUnit.MILLISECONDS.toMinutes(it.repeatIntervalMillis).toInt() }

// Prefixed: SdkDemoRoute's `id` and `amount` arguments land in this same SavedStateHandle.
private const val INTERVAL_KEY = "background_interval"

/** The intervals offered, in minutes; 15 is the shortest Android allows for periodic work. */
val SYNC_INTERVALS = listOf(15, 30, 60)

data class BackgroundSyncUiState(
    val intervalMinutes: Int = SYNC_INTERVALS.first(),
    /** State of the periodic work, or null when it was never enqueued. */
    val status: WorkInfo.State? = null,
    val registered: Boolean = false,
    val lastRun: Long? = null,
    val runs: Int = 0,
)

/** Turns the periodic sync on and off; the interval lives in [SavedStateHandle], the rest is read from WorkManager and [SyncLog]. */
@HiltViewModel
class BackgroundSyncViewModel(
    private val workManager: WorkManager,
    private val log: SyncLog,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    // WorkManager is fetched here, not provided by a module: it starts on demand, so only this screen starts it.
    @Inject
    constructor(
        @ApplicationContext context: Context,
        log: SyncLog,
        savedStateHandle: SavedStateHandle,
    ) : this(WorkManager.getInstance(context), log, savedStateHandle)

    private val refreshes = MutableStateFlow(0)

    val state: StateFlow<BackgroundSyncUiState> =
        combine(
            savedStateHandle.getStateFlow<Int?>(INTERVAL_KEY, null),
            workManager.getWorkInfosForUniqueWorkFlow(SYNC_WORK_NAME),
            // A finished "Run now" changes the log but not the periodic work: watch it to re-read the log.
            workManager.getWorkInfosByTagFlow(RUN_NOW_TAG),
            refreshes,
        ) { interval, periodic, _, _ ->
            val current = periodic.lastOrNull()
            BackgroundSyncUiState(
                // A fresh screen (no saved choice) shows the interval of the work already enqueued.
                intervalMinutes = interval ?: current?.takeIf { !it.state.isFinished }?.intervalMinutes() ?: SYNC_INTERVALS.first(),
                status = current?.state,
                registered = periodic.any { !it.state.isFinished },
                lastRun = log.lastRun(),
                runs = log.runs(),
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            BackgroundSyncUiState(
                intervalMinutes = savedStateHandle[INTERVAL_KEY] ?: SYNC_INTERVALS.first(),
                lastRun = log.lastRun(),
                runs = log.runs(),
            ),
        )

    fun setEnabled(enabled: Boolean) {
        if (enabled) enqueue(state.value.intervalMinutes) else workManager.cancelUniqueWork(SYNC_WORK_NAME)
    }

    /** Stores [minutes]; when the sync is on, updates the enqueued work in place (UPDATE keeps its schedule). */
    fun setInterval(minutes: Int) {
        savedStateHandle[INTERVAL_KEY] = minutes
        viewModelScope.launch {
            val running = workManager.getWorkInfosForUniqueWorkFlow(SYNC_WORK_NAME).first().any { !it.state.isFinished }
            if (running) enqueue(minutes)
        }
    }

    /** Re-reads the log, for runs that happened while nothing was watching (another process, a worker in the background). */
    fun refresh() {
        refreshes.value++
    }

    /** One immediate run of [SyncWorker]; debug builds only. */
    fun runNow() {
        if (!BuildConfig.DEBUG) return
        workManager.enqueue(OneTimeWorkRequestBuilder<SyncWorker>().addTag(RUN_NOW_TAG).build())
    }

    private fun enqueue(minutes: Int) {
        workManager.enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<SyncWorker>(minutes.toLong(), TimeUnit.MINUTES).build(),
        )
    }
}
