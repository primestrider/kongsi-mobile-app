package com.primestrider.kotlinstarter.feature.example.sdk.background

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import javax.inject.Singleton

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class SyncWorkerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val log = SyncLog(context)

    private fun factory(clock: () -> Long) =
        object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters,
            ): ListenableWorker = SyncWorker(appContext, workerParameters, log, clock)
        }

    private fun run(clock: () -> Long): ListenableWorker.Result =
        runBlocking { TestListenableWorkerBuilder<SyncWorker>(context).setWorkerFactory(factory(clock)).build().doWork() }

    @Test
    fun a_fresh_log_has_no_runs() {
        assertNull(log.lastRun())
        assertEquals(0, log.runs())
    }

    @Test
    fun one_run_records_the_time_and_counts_one() {
        assertEquals(ListenableWorker.Result.success(), run { 1_000L })
        assertEquals(1, log.runs())
        assertEquals(1_000L, log.lastRun())
    }

    @Test
    fun two_runs_count_two_and_keep_the_latest_time() {
        run { 1_000L }
        run { 2_000L }
        assertEquals(2, log.runs())
        assertEquals(2_000L, log.lastRun())
    }

    @Test
    fun the_log_is_one_instance_per_process_so_record_is_serialised() {
        assertTrue(SyncLog::class.java.isAnnotationPresent(Singleton::class.java))
    }

    @Test
    fun an_exception_fails_the_run_without_recording_it() {
        assertEquals(ListenableWorker.Result.failure(), run { error("clock broke") })
        assertEquals(0, log.runs())
    }
}
