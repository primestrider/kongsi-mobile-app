package com.primestrider.kotlinstarter.feature.example.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.viewModelScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.datastore.ThemePreferenceRepository
import com.primestrider.kotlinstarter.core.i18n.AppLanguage
import com.primestrider.kotlinstarter.core.i18n.LanguageController
import com.primestrider.kotlinstarter.core.model.ThemeMode
import com.primestrider.kotlinstarter.testing.InMemoryPreferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.IOException
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class SettingsViewModelTest {
    private val created = mutableListOf<SettingsViewModel>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        created.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
    }

    private fun viewModel() =
        SettingsViewModel(ThemePreferenceRepository(InMemoryPreferencesDataStore()), LanguageController()).also { created += it }

    @Test
    fun setThemeModeUpdatesState() =
        runTest {
            val vm = viewModel()
            vm.setThemeMode(ThemeMode.LIGHT)
            assertEquals(ThemeMode.LIGHT, vm.state.value.themeMode)
        }

    @Test
    fun setLanguageUpdatesState() =
        runTest {
            val vm = viewModel()
            vm.setLanguage(AppLanguage.ID)
            assertEquals(AppLanguage.ID, vm.state.value.language)
        }

    @Test
    fun setThemeModeSurvivesIoFailure() =
        runTest {
            val failing =
                object : DataStore<Preferences> {
                    override val data: Flow<Preferences> = flowOf(emptyPreferences())

                    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
                        throw IOException("disk full")
                }
            val vm = SettingsViewModel(ThemePreferenceRepository(failing), LanguageController()).also { created += it }
            vm.setThemeMode(ThemeMode.DARK)
            assertEquals(ThemeMode.SYSTEM, vm.state.value.themeMode)
        }

    @Test
    fun refreshLanguagePicksUpExternalChange() =
        runTest {
            val original = Locale.getDefault()
            try {
                Locale.setDefault(Locale.forLanguageTag("en-US"))
                val vm = viewModel()
                assertEquals(AppLanguage.EN, vm.state.value.language)
                // Simulates a language change made outside the app while this screen was on the back stack.
                Locale.setDefault(Locale.forLanguageTag("id-ID"))
                vm.refreshLanguage()
                assertEquals(AppLanguage.ID, vm.state.value.language)
            } finally {
                Locale.setDefault(original)
            }
        }
}
