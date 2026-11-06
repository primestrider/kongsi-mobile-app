package com.primestrider.kotlinstarter.feature.example.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.primestrider.kotlinstarter.core.datastore.ThemePreferenceRepository
import com.primestrider.kotlinstarter.core.i18n.AppLanguage
import com.primestrider.kotlinstarter.core.i18n.LanguageController
import com.primestrider.kotlinstarter.core.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode,
    val language: AppLanguage,
)

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val themeRepository: ThemePreferenceRepository,
        private val languageController: LanguageController,
    ) : ViewModel() {
        private val language = MutableStateFlow(languageController.current())

        val state: StateFlow<SettingsUiState> =
            combine(themeRepository.themeMode, language, ::SettingsUiState)
                .stateIn(
                    viewModelScope,
                    SharingStarted.Eagerly,
                    SettingsUiState(ThemeMode.SYSTEM, language.value),
                )

        fun setThemeMode(mode: ThemeMode) {
            viewModelScope.launch {
                try {
                    themeRepository.setThemeMode(mode)
                } catch (_: IOException) {
                    // Persisting failed; the stored preference (and so the state) is unchanged.
                }
            }
        }

        /** Re-reads the app language, which can change outside the app (system language settings). */
        fun refreshLanguage() {
            language.value = languageController.current()
        }

        fun setLanguage(newLanguage: AppLanguage) {
            language.value = newLanguage
            // May recreate the activity; the ViewModel and its state survive that.
            languageController.set(newLanguage)
        }
    }
