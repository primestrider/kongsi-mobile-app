package com.primestrider.kotlinstarter

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.primestrider.kotlinstarter.core.auth.SessionExpiryAnnouncer
import com.primestrider.kotlinstarter.core.crash.CrashRecorder
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class KotlinStarterApp :
    Application(),
    Configuration.Provider {
    @Inject lateinit var sessionExpiryAnnouncer: SessionExpiryAnnouncer

    @Inject lateinit var workerFactory: HiltWorkerFactory

    // WorkManager starts on first use (its startup initializer is removed in the manifest) with this configuration,
    // so @HiltWorker workers get their dependencies injected.
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        // Earliest app code: before content providers and Hilt, so crashes while they start are recorded too.
        // The injected CrashRecorder (read by AppViewModel) uses the same store.
        CrashRecorder(base).install()
    }

    override fun onCreate() {
        super.onCreate()
        sessionExpiryAnnouncer.start()
    }
}
