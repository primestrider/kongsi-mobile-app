package com.primestrider.kotlinstarter.core.crash

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class CrashRecorderTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val original = Thread.getDefaultUncaughtExceptionHandler()
    private val previousCalls = mutableListOf<Throwable>()

    @Before
    fun setUp() {
        Thread.setDefaultUncaughtExceptionHandler { _, e -> previousCalls += e }
    }

    @After
    fun tearDown() {
        Thread.setDefaultUncaughtExceptionHandler(original)
        prefs().edit().clear().commit()
    }

    private fun prefs() = context.getSharedPreferences("crash", Context.MODE_PRIVATE)

    private fun crash(error: Throwable) = Thread.getDefaultUncaughtExceptionHandler()!!.uncaughtException(Thread.currentThread(), error)

    @Test
    fun crash_is_recorded_and_passed_on_once() {
        val recorder = CrashRecorder(preferences = ::prefs, clock = { 42L })
        recorder.install()
        val error = IllegalStateException("boom")
        crash(error)
        assertEquals(CrashReport("java.lang.IllegalStateException", "boom", 42L), recorder.lastCrash())
        assertEquals(1, previousCalls.size)
        assertSame(error, previousCalls.single())
    }

    @Test
    fun long_message_is_truncated() {
        val recorder = CrashRecorder(context)
        recorder.install()
        crash(RuntimeException("x".repeat(2_000)))
        assertEquals(500, recorder.lastCrash()!!.message!!.length)
    }

    @Test
    fun failed_write_still_reaches_the_previous_handler() {
        val recorder = CrashRecorder(preferences = { error("disk gone") })
        recorder.install()
        crash(IllegalStateException("boom"))
        assertEquals(1, previousCalls.size)
    }

    @Test
    fun installing_twice_records_once() {
        val recorder = CrashRecorder(context)
        recorder.install()
        recorder.install()
        crash(IllegalStateException("boom"))
        assertEquals(1, previousCalls.size)
    }

    @Test
    fun clear_removes_the_report() {
        val recorder = CrashRecorder(context)
        recorder.install()
        crash(IllegalStateException("boom"))
        recorder.clear()
        assertNull(recorder.lastCrash())
    }
}
