package com.primestrider.kotlinstarter.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.auth.FakeSessionCipher
import com.primestrider.kotlinstarter.core.auth.SessionRepository
import com.primestrider.kotlinstarter.core.crash.CrashRecorder
import com.primestrider.kotlinstarter.core.datastore.ThemePreferenceRepository
import com.primestrider.kotlinstarter.core.model.ThemeMode
import com.primestrider.kotlinstarter.testing.GatedPreferencesDataStore
import com.primestrider.kotlinstarter.testing.InMemoryPreferencesDataStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Robolectric for the crash recorder's SharedPreferences.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val recorder = CrashRecorder(context)

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        recorder.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun themeModeIsNullUntilFirstValueThenSystem() =
        runTest {
            val store = InMemoryPreferencesDataStore()
            val viewModel = AppViewModel(ThemePreferenceRepository(store), sessionRepository(this), recorder)
            assertNull(viewModel.themeMode.value)
            advanceUntilIdle()
            assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)
        }

    private fun sessionRepository(scope: CoroutineScope) =
        SessionRepository(InMemoryPreferencesDataStore(), FakeSessionCipher(), Json, scope)

    @Test
    fun notReadyWhileSessionStillLoading() =
        runTest {
            val store = GatedPreferencesDataStore()
            val gate = CompletableDeferred<Unit>()
            store.readGate = gate
            val session = SessionRepository(store, FakeSessionCipher(), Json, this)
            val viewModel = AppViewModel(ThemePreferenceRepository(InMemoryPreferencesDataStore()), session, recorder)
            advanceUntilIdle()
            assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)
            assertFalse(viewModel.ready.value)
            gate.complete(Unit)
            advanceUntilIdle()
            assertTrue(viewModel.ready.value)
        }

    @Test
    fun readyOnlyAfterThemeAndSessionLoaded() =
        runTest {
            val viewModel =
                AppViewModel(ThemePreferenceRepository(InMemoryPreferencesDataStore()), sessionRepository(this), recorder)
            assertFalse(viewModel.ready.value)
            advanceUntilIdle()
            assertTrue(viewModel.ready.value)
        }

    @Test
    fun storedCrashIsExposedAndDismissClearsIt() =
        runTest {
            val handler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { _, _ -> }
            try {
                recorder.install()
                Thread.getDefaultUncaughtExceptionHandler()!!.uncaughtException(Thread.currentThread(), IllegalStateException("boom"))
            } finally {
                Thread.setDefaultUncaughtExceptionHandler(handler)
            }
            val viewModel =
                AppViewModel(ThemePreferenceRepository(InMemoryPreferencesDataStore()), sessionRepository(this), recorder)
            assertEquals("java.lang.IllegalStateException", viewModel.lastCrash.value?.exceptionClass)
            viewModel.dismissCrash()
            assertNull(viewModel.lastCrash.value)
            assertNull(recorder.lastCrash())
        }

    @Test
    fun noCrashMeansNoFallback() =
        runTest {
            val viewModel =
                AppViewModel(ThemePreferenceRepository(InMemoryPreferencesDataStore()), sessionRepository(this), recorder)
            assertNull(viewModel.lastCrash.value)
        }
}
