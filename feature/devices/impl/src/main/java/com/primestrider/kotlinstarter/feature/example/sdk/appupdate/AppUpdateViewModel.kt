package com.primestrider.kotlinstarter.feature.example.sdk.appupdate

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed interface AppUpdateState {
    data object Checking : AppUpdateState

    data object UpToDate : AppUpdateState

    data class Available(
        val versionCode: Int,
    ) : AppUpdateState

    data class Downloading(
        val bytes: Long,
        val total: Long,
    ) : AppUpdateState

    /** Downloaded in the background; installing restarts the app, so the user chooses when. */
    data object Downloaded : AppUpdateState

    /** [code] is an `InstallErrorCode`; outside Play (debug builds, sideloaded APKs) checking fails too. */
    data class Failed(
        val code: Int,
    ) : AppUpdateState

    data class Unavailable(
        val message: String,
    ) : AppUpdateState
}

/**
 * Play in-app updates, flexible flow: ask Play whether a newer version exists, let the user accept it, download
 * while the app keeps running, then install on request. Only an app installed from Play gets answers; a debug build
 * or a sideloaded APK reports [AppUpdateState.Unavailable]. Test it with internal app sharing or `FakeAppUpdateManager`.
 */
@HiltViewModel
class AppUpdateViewModel
    @Inject
    constructor(
        private val manager: AppUpdateManager,
    ) : ViewModel() {
        private val _state = MutableStateFlow<AppUpdateState>(AppUpdateState.Checking)
        val state: StateFlow<AppUpdateState> = _state.asStateFlow()
        private var info: AppUpdateInfo? = null

        private val listener =
            InstallStateUpdatedListener { install -> install.toState()?.let { _state.value = it } }

        init {
            manager.registerListener(listener)
            check()
        }

        fun check() {
            _state.value = AppUpdateState.Checking
            viewModelScope.launch {
                _state.value =
                    try {
                        val latest = manager.appUpdateInfo.await().also { info = it }
                        when {
                            latest.installStatus() == InstallStatus.DOWNLOADED -> AppUpdateState.Downloaded
                            latest.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                                latest.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) ->
                                AppUpdateState.Available(latest.availableVersionCode())
                            else -> AppUpdateState.UpToDate
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        AppUpdateState.Unavailable(e.message ?: e.javaClass.simpleName)
                    }
            }
        }

        /** Shows Play's confirmation dialog; the download then reports through the install listener. */
        fun startUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>) {
            // Play starts a flow only once per AppUpdateInfo: a second try needs a fresh check (onUpdateFlowResult).
            val latest = info ?: return
            info = null
            manager.startUpdateFlowForResult(latest, launcher, AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build())
        }

        /** Play's dialog closed: unless the user accepted, check again so Update can be offered and started anew. */
        fun onUpdateFlowResult(resultCode: Int) {
            if (resultCode != Activity.RESULT_OK) check()
        }

        /** Installs the downloaded update; Play restarts the app. */
        fun completeUpdate() {
            manager.completeUpdate()
        }

        private fun InstallState.toState(): AppUpdateState? =
            when (installStatus()) {
                InstallStatus.PENDING, InstallStatus.DOWNLOADING -> AppUpdateState.Downloading(bytesDownloaded(), totalBytesToDownload())
                InstallStatus.DOWNLOADED -> AppUpdateState.Downloaded
                InstallStatus.FAILED -> AppUpdateState.Failed(installErrorCode())
                InstallStatus.CANCELED -> info?.let { AppUpdateState.Available(it.availableVersionCode()) }
                else -> null
            }

        override fun onCleared() {
            manager.unregisterListener(listener)
        }
    }

@Module
@InstallIn(SingletonComponent::class)
object AppUpdateModule {
    @Provides
    fun appUpdateManager(
        @ApplicationContext context: Context,
    ): AppUpdateManager = AppUpdateManagerFactory.create(context)
}
