package com.primestrider.kotlinstarter.feature.example.sdk.background

import android.content.Context
import android.os.Looper
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class BackgroundSyncViewModelTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val log = SyncLog(context)
    private val created = mutableListOf<ViewModel>()
    private lateinit var workManager: WorkManager

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val factory =
            object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ): ListenableWorker = SyncWorker(appContext, workerParameters, log) { 5_000L }
            }
        val config =
            Configuration
                .Builder()
                .setExecutor(SynchronousExecutor())
                .setWorkerFactory(factory)
                .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        workManager = WorkManager.getInstance(context)
    }

    @After
    fun tearDown() {
        created.forEach { it.viewModelScope.cancel() }
        WorkManagerTestInitHelper.closeWorkDatabase()
        Dispatchers.resetMain()
    }

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) =
        BackgroundSyncViewModel(workManager, log, handle).also { created += it }

    /** Polls until [check] holds: WorkManager and its flows answer on their own threads. */
    private fun <T> eventually(
        read: () -> T,
        check: (T) -> Boolean,
    ): T {
        val deadline = System.currentTimeMillis() + 5_000
        while (true) {
            shadowOf(Looper.getMainLooper()).idle()
            val value = read()
            if (check(value)) return value
            if (System.currentTimeMillis() > deadline) fail("Timed out; last value: $value")
            Thread.sleep(20)
        }
    }

    private fun uniqueWork(check: (List<WorkInfo>) -> Boolean) =
        eventually({ workManager.getWorkInfosForUniqueWork(SYNC_WORK_NAME).get() }, check)

    private fun List<WorkInfo>.intervalMinutes() = TimeUnit.MILLISECONDS.toMinutes(single().periodicityInfo!!.repeatIntervalMillis)

    @Test
    fun enabling_enqueues_one_periodic_work_every_15_minutes() {
        val vm = viewModel()
        vm.setEnabled(true)
        val infos = uniqueWork { it.isNotEmpty() }
        assertEquals(WorkInfo.State.ENQUEUED, infos.single().state)
        assertEquals(15L, infos.intervalMinutes())
        eventually({ vm.state.value }) { it.registered && it.status == WorkInfo.State.ENQUEUED }
    }

    @Test
    fun changing_the_interval_while_enabled_updates_the_work() {
        val vm = viewModel()
        vm.setEnabled(true)
        eventually({ vm.state.value }) { it.registered }
        vm.setInterval(60)
        val infos = uniqueWork { it.size == 1 && it.intervalMinutes() == 60L }
        assertEquals(WorkInfo.State.ENQUEUED, infos.single().state)
        assertEquals(60, vm.state.value.intervalMinutes)
    }

    @Test
    fun changing_the_interval_while_disabled_registers_nothing() {
        val vm = viewModel()
        vm.setInterval(30)
        assertEquals(30, vm.state.value.intervalMinutes)
        Thread.sleep(100)
        assertTrue(workManager.getWorkInfosForUniqueWork(SYNC_WORK_NAME).get().isEmpty())
    }

    @Test
    fun disabling_cancels_the_work() {
        val vm = viewModel()
        vm.setEnabled(true)
        uniqueWork { it.isNotEmpty() }
        vm.setEnabled(false)
        uniqueWork { infos -> infos.isNotEmpty() && infos.all { it.state == WorkInfo.State.CANCELLED } }
        eventually({ vm.state.value }) { !it.registered }
    }

    @Test
    fun the_interval_survives_a_new_view_model_under_a_prefixed_key() {
        val handle = SavedStateHandle()
        viewModel(handle).setInterval(60)
        assertEquals(60, viewModel(handle).state.value.intervalMinutes)
        assertFalse(handle.keys().any { it == "id" || it == "amount" })
    }

    @Test
    fun run_now_runs_the_worker_once_and_the_state_shows_it() {
        val vm = viewModel()
        vm.runNow()
        eventually({ vm.state.value }) { it.runs == 1 && it.lastRun == 5_000L }
    }

    @Test
    fun a_fresh_screen_shows_the_interval_of_the_enqueued_work() {
        val first = viewModel()
        first.setEnabled(true)
        eventually({ first.state.value }) { it.registered }
        first.setInterval(60)
        uniqueWork { it.size == 1 && it.intervalMinutes() == 60L }
        // No saved choice, as after the process was killed: the enqueued work is the source.
        val fresh = viewModel(SavedStateHandle())
        eventually({ fresh.state.value }) { it.registered && it.intervalMinutes == 60 }
    }
}
