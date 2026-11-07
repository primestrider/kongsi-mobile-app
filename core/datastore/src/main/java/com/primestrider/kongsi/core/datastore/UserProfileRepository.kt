package com.primestrider.kongsi.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** The user's own details. Blank values are stored as absent, so both fields are null until the user fills them. */
data class UserProfile(
    val name: String? = null,
    /** A mobile number as written locally (`08…`); validated by the screen that saves it. */
    val phone: String? = null,
)

/**
 * The user's profile, kept on the device in the settings DataStore. Profile edits it; the bill feature reads it
 * (the user's own name in a bill, "Use my number" for an e-wallet).
 */
@Singleton
class UserProfileRepository
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) {
        val profile: Flow<UserProfile> =
            dataStore.data
                .catch { error ->
                    if (error is IOException) emit(emptyPreferences()) else throw error
                }.map { UserProfile(name = it[StorageKeys.profileName], phone = it[StorageKeys.profilePhone]) }
                .distinctUntilChanged()

        suspend fun save(profile: UserProfile) {
            dataStore.edit { prefs ->
                prefs.putOrRemove(StorageKeys.profileName, profile.name)
                prefs.putOrRemove(StorageKeys.profilePhone, profile.phone)
            }
        }

        private fun MutablePreferences.putOrRemove(
            key: Preferences.Key<String>,
            value: String?,
        ) {
            val trimmed = value?.trim()
            if (trimmed.isNullOrEmpty()) remove(key) else set(key, trimmed)
        }
    }
