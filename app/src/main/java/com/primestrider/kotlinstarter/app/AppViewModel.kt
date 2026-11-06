package com.primestrider.kotlinstarter.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.primestrider.kotlinstarter.core.auth.SessionRepository
import com.primestrider.kotlinstarter.core.crash.CrashRecorder
import com.primestrider.kotlinstarter.core.crash.CrashReport
import com.primestrider.kotlinstarter.core.datastore.ThemePreferenceRepository
import com.primestrider.kotlinstarter.core.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppViewModel
    @Inject
    constructor(
        repository: ThemePreferenceRepository,
        sessionRepository: SessionRepository,
        private val crashRecorder: CrashRecorder,
    ) : ViewModel() {
        /** Null until the stored preference has loaded. */
        val themeMode: StateFlow<ThemeMode?> =
            repository.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, null)

        /** True once theme and session are both loaded; the splash screen stays up until then. */
        val ready: StateFlow<Boolean> =
            combine(themeMode, sessionRepository.loaded) { theme, loaded -> theme != null && loaded }
                .stateIn(viewModelScope, SharingStarted.Eagerly, false)

        private val _lastCrash = MutableStateFlow(crashRecorder.lastCrash())

        /** The crash from the previous run, read once at start; non-null shows the fallback screen. */
        val lastCrash: StateFlow<CrashReport?> = _lastCrash.asStateFlow()

        fun dismissCrash() {
            crashRecorder.clear()
            _lastCrash.value = null
        }
    }
