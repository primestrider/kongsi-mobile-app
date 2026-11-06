package com.primestrider.kotlinstarter.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Builds the app's preferences DataStore. A corrupted file is replaced by empty preferences so
 * reads fall back to defaults instead of killing the data flow.
 */
fun createPreferencesDataStore(
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    produceFile: () -> File,
): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        scope = scope,
        produceFile = produceFile,
    )

/** DataStore file names. Never rename them: an app update would no longer find existing settings or the session. */
object DataStoreFiles {
    const val SETTINGS = "settings"
    const val SESSION = "session"
}

/** Qualifier for the encrypted-session DataStore (file `session`), kept apart from settings. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SessionStore

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = createPreferencesDataStore { context.preferencesDataStoreFile(DataStoreFiles.SETTINGS) }

    @Provides
    @Singleton
    @SessionStore
    fun provideSessionDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = createPreferencesDataStore { context.preferencesDataStoreFile(DataStoreFiles.SESSION) }
}
