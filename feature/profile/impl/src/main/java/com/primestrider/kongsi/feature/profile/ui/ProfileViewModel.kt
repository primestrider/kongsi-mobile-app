package com.primestrider.kongsi.feature.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.primestrider.kongsi.core.datastore.ThemePreferenceRepository
import com.primestrider.kongsi.core.datastore.UserProfile
import com.primestrider.kongsi.core.datastore.UserProfileRepository
import com.primestrider.kongsi.core.i18n.AppLanguage
import com.primestrider.kongsi.core.i18n.LanguageController
import com.primestrider.kongsi.core.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

data class ProfileUiState(
    val profile: UserProfile = UserProfile(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.fallback,
)

/** The Profile tab: the user's details from DataStore, the theme preference and the app language. */
@HiltViewModel
class ProfileViewModel
    @Inject
    constructor(
        profileRepository: UserProfileRepository,
        private val themeRepository: ThemePreferenceRepository,
        private val languageController: LanguageController,
    ) : ViewModel() {
        private val language = MutableStateFlow(languageController.current())

        val state: StateFlow<ProfileUiState> =
            combine(profileRepository.profile, themeRepository.themeMode, language, ::ProfileUiState)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState(language = language.value))

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
