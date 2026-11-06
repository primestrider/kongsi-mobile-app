package com.primestrider.kotlinstarter.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.primestrider.kotlinstarter.core.datastore.StorageKeys
import com.primestrider.kotlinstarter.core.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemePreferenceRepository
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) {
        val themeMode: Flow<ThemeMode> =
            dataStore.data
                .catch { error ->
                    if (error is IOException) emit(emptyPreferences()) else throw error
                }.map { ThemeMode.fromStored(it[StorageKeys.themeMode]) }

        suspend fun setThemeMode(mode: ThemeMode) {
            dataStore.edit { it[StorageKeys.themeMode] = mode.name }
        }
    }
