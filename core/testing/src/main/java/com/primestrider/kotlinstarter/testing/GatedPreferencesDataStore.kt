package com.primestrider.kotlinstarter.testing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/** [InMemoryPreferencesDataStore] whose reads and writes can be held back or made to fail. */
class GatedPreferencesDataStore(
    private val delegate: InMemoryPreferencesDataStore = InMemoryPreferencesDataStore(),
) : DataStore<Preferences> {
    @Volatile var readGate: CompletableDeferred<Unit>? = null

    @Volatile var writeGate: CompletableDeferred<Unit>? = null

    @Volatile var readError: Throwable? = null

    @Volatile var writeError: Throwable? = null

    override val data: Flow<Preferences> =
        flow {
            readError?.let { throw it }
            readGate?.await()
            emitAll(delegate.data)
        }

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        writeError?.let { throw it }
        writeGate?.await()
        return delegate.updateData(transform)
    }
}
