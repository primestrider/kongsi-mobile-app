package com.primestrider.kotlinstarter.core.auth

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.primestrider.kotlinstarter.testing.GatedPreferencesDataStore
import com.primestrider.kotlinstarter.testing.InMemoryPreferencesDataStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.ProviderException
import java.security.UnrecoverableKeyException
import javax.crypto.AEADBadTagException

@OptIn(ExperimentalCoroutinesApi::class)
class SessionRepositoryTest {
    private val key = stringPreferencesKey("auth.session")
    private val json = Json { ignoreUnknownKeys = true }
    private val sample =
        Session(
            user = AuthUser(1, "emilys", "e@x.com", "Emily", "Johnson"),
            tokens = AuthTokens("access", "refresh"),
        )

    private fun TestScope.repo(
        store: InMemoryPreferencesDataStore,
        cipher: SessionCipher = FakeSessionCipher(),
    ) = SessionRepository(store, cipher, json, this)

    private suspend fun InMemoryPreferencesDataStore.raw(): Preferences = data.first()

    @Test
    fun freshStoreLoadsAsSignedOut() =
        runTest {
            val repository = repo(InMemoryPreferencesDataStore())
            assertFalse(repository.loaded.value)
            advanceUntilIdle()
            assertTrue(repository.loaded.value)
            assertNull(repository.session.value)
            assertFalse(repository.isSignedIn)
            assertNull(repository.accessToken())
        }

    @Test
    fun replacedAccessTokensAreSupersededOnlyWithinTheirSession() =
        runTest {
            val repository = repo(InMemoryPreferencesDataStore())
            advanceUntilIdle()
            repository.signIn(sample)
            assertTrue(repository.updateTokens("refresh", AuthTokens("a2", "r2")))
            assertTrue(repository.updateTokens("r2", AuthTokens("a3", "r3")))
            val refreshed = repository.session.value!!
            assertTrue(repository.isSupersededToken("access", refreshed))
            assertTrue(repository.isSupersededToken("a2", refreshed))
            assertFalse(repository.isSupersededToken("a3", refreshed))
            assertFalse(repository.updateTokens("stale", AuthTokens("x", "y")))
            assertFalse(repository.isSupersededToken("a3", refreshed))

            repository.signIn(sample)
            assertFalse(repository.isSupersededToken("a2", sample))
            assertFalse(repository.isSupersededToken("a2", refreshed))
            assertTrue(repository.updateTokens("refresh", AuthTokens("a2", "r2")))
            val beforeSignOut = repository.session.value!!
            repository.signOut(SignOutReason.USER)
            assertFalse(repository.isSupersededToken("access", beforeSignOut))
        }

    @Test
    fun onlyTheMostRecentlyReplacedAccessTokensAreKept() =
        runTest {
            val repository = repo(InMemoryPreferencesDataStore())
            advanceUntilIdle()
            repository.signIn(Session(sample.user, AuthTokens("a0", "r0")))
            for (i in 1..10) assertTrue(repository.updateTokens("r${i - 1}", AuthTokens("a$i", "r$i")))
            val current = repository.session.value!!
            assertFalse(repository.isSupersededToken("a0", current))
            assertFalse(repository.isSupersededToken("a1", current))
            for (i in 2..9) assertTrue("a$i", repository.isSupersededToken("a$i", current))
            assertFalse(repository.isSupersededToken("a10", current))
        }

    @Test
    fun signInStoresEncryptedValue() =
        runTest {
            val store = InMemoryPreferencesDataStore()
            val repository = repo(store)
            advanceUntilIdle()
            repository.signIn(sample)
            assertEquals(sample, repository.session.value)
            assertEquals("access", repository.accessToken())
            val stored = store.raw()[key]
            assertNotNull(stored)
            assertFalse(stored!!.contains("refresh"))
            assertFalse(stored.contains("emilys"))
        }

    @Test
    fun newRepositoryRestoresSession() =
        runTest {
            val store = InMemoryPreferencesDataStore()
            repo(store).signIn(sample)
            val restored = repo(store)
            advanceUntilIdle()
            assertEquals(sample, restored.session.value)
            assertTrue(restored.loaded.value)
        }

    @Test
    fun updateTokensReplacesAndPersists() =
        runTest {
            val store = InMemoryPreferencesDataStore()
            val repository = repo(store)
            advanceUntilIdle()
            repository.signIn(sample)
            val newTokens = AuthTokens("a2", "r2")
            assertTrue(repository.updateTokens("refresh", newTokens))
            assertEquals(newTokens, repository.session.value?.tokens)
            val restored = repo(store)
            advanceUntilIdle()
            assertEquals(newTokens, restored.session.value?.tokens)
        }

    @Test
    fun updateTokensIsNoOpWhenSignedOut() =
        runTest {
            val store = InMemoryPreferencesDataStore()
            val repository = repo(store)
            advanceUntilIdle()
            assertFalse(repository.updateTokens("refresh", AuthTokens("a", "r")))
            assertNull(repository.session.value)
            assertNull(store.raw()[key])
        }

    @Test
    fun updateTokensLeavesASessionWithAnotherRefreshTokenAlone() =
        runTest {
            val store = InMemoryPreferencesDataStore()
            val repository = repo(store)
            advanceUntilIdle()
            repository.signIn(sample)
            assertFalse(repository.updateTokens("someone-else", AuthTokens("a2", "r2")))
            assertEquals(sample, repository.session.value)
            assertEquals(sample, repo(store).also { advanceUntilIdle() }.session.value)
        }

    @Test
    fun signOutIfCurrentSignsOutTheMatchingSession() =
        runTest {
            val store = InMemoryPreferencesDataStore()
            val repository = repo(store)
            advanceUntilIdle()
            repository.signIn(sample)
            assertTrue(repository.signOutIfCurrent("refresh", SignOutReason.EXPIRED))
            assertNull(repository.session.value)
            assertNull(store.raw()[key])
            assertEquals(SignOutReason.EXPIRED, repository.signOutReason.value)
        }

    @Test
    fun signOutIfCurrentLeavesASessionWithAnotherRefreshTokenAlone() =
        runTest {
            val store = InMemoryPreferencesDataStore()
            val repository = repo(store)
            advanceUntilIdle()
            repository.signIn(sample)
            assertFalse(repository.signOutIfCurrent("someone-else", SignOutReason.EXPIRED))
            assertEquals(sample, repository.session.value)
            assertNotNull(store.raw()[key])
            assertNull(repository.signOutReason.value)
        }

    @Test
    fun signOutIfCurrentIsNoOpWhenSignedOut() =
        runTest {
            val repository = repo(InMemoryPreferencesDataStore())
            advanceUntilIdle()
            repository.signOut(SignOutReason.USER)
            assertFalse(repository.signOutIfCurrent("refresh", SignOutReason.EXPIRED))
            assertEquals(SignOutReason.USER, repository.signOutReason.value)
        }

    @Test
    fun signOutClearsStorageAndSetsReason() =
        runTest {
            val store = InMemoryPreferencesDataStore()
            val repository = repo(store)
            advanceUntilIdle()
            repository.signIn(sample)
            repository.signOut(SignOutReason.EXPIRED)
            assertNull(repository.session.value)
            assertNull(store.raw()[key])
            assertEquals(SignOutReason.EXPIRED, repository.signOutReason.value)
            repository.clearSignOutReason()
            assertNull(repository.signOutReason.value)
        }

    @Test
    fun signInClearsSignOutReason() =
        runTest {
            val repository = repo(InMemoryPreferencesDataStore())
            advanceUntilIdle()
            repository.signOut(SignOutReason.EXPIRED)
            repository.signIn(sample)
            assertNull(repository.signOutReason.value)
        }

    @Test
    fun garbageStoredValueIsTreatedAsSignedOutAndRemoved() =
        runTest {
            val store = InMemoryPreferencesDataStore()
            store.edit { it[key] = "not-base64!!" }
            val repository = repo(store)
            advanceUntilIdle()
            assertTrue(repository.loaded.value)
            assertNull(repository.session.value)
            assertNull(store.raw()[key])
        }

    @Test
    fun signOutDuringUpdateTokensWriteIsNotResurrected() =
        runTest {
            val store = GatedPreferencesDataStore()
            val repository = SessionRepository(store, FakeSessionCipher(), json, this)
            advanceUntilIdle()
            repository.signIn(sample)
            val gate = CompletableDeferred<Unit>()
            store.writeGate = gate
            val update = launch { repository.updateTokens("refresh", AuthTokens("a2", "r2")) }
            runCurrent()
            val signOut = launch { repository.signOut(SignOutReason.EXPIRED) }
            runCurrent()
            gate.complete(Unit)
            advanceUntilIdle()
            assertTrue(update.isCompleted && signOut.isCompleted)
            assertNull(repository.session.value)
            assertNull(store.data.first()[key])
        }

    @Test
    fun storageReadFailureIsSignedOutButKeepsStoredData() =
        runTest {
            val store = GatedPreferencesDataStore()
            SessionRepository(store, FakeSessionCipher(), json, this).signIn(sample)
            store.readError = IOException("disk")
            val repository = SessionRepository(store, FakeSessionCipher(), json, this)
            advanceUntilIdle()
            assertTrue(repository.loaded.value)
            assertNull(repository.session.value)
            store.readError = null
            assertNotNull(store.data.first()[key])
        }

    @Test
    fun failedStorageRemovalKeepsSessionSignedIn() =
        runTest {
            val store = GatedPreferencesDataStore()
            val repository = SessionRepository(store, FakeSessionCipher(), json, this)
            advanceUntilIdle()
            repository.signIn(sample)
            store.writeError = IOException("disk")
            runCatching { repository.signOut(SignOutReason.USER) }.also { assertTrue(it.exceptionOrNull() is IOException) }
            assertEquals(sample, repository.session.value)
            assertNull(repository.signOutReason.value)
        }

    @Test
    fun tamperedCiphertextIsTreatedAsSignedOutAndRemoved() =
        runTest {
            val store = InMemoryPreferencesDataStore()
            repo(store).signIn(sample)
            val repository = repo(store, failingDecrypt(AEADBadTagException("tag mismatch")))
            advanceUntilIdle()
            assertTrue(repository.loaded.value)
            assertNull(repository.session.value)
            assertNull(store.raw()[key])
        }

    @Test
    fun permanentlyLostKeyIsTreatedAsSignedOutAndRemoved() =
        runTest {
            val store = InMemoryPreferencesDataStore()
            repo(store).signIn(sample)
            val repository = repo(store, failingDecrypt(UnrecoverableKeyException("gone")))
            advanceUntilIdle()
            assertNull(repository.session.value)
            assertNull(store.raw()[key])
        }

    @Test
    fun transientKeystoreFailureIsSignedOutButKeepsStoredData() =
        runTest {
            val store = InMemoryPreferencesDataStore()
            repo(store).signIn(sample)
            val repository = repo(store, failingDecrypt(GeneralSecurityException("keystore busy")))
            advanceUntilIdle()
            assertTrue(repository.loaded.value)
            assertNull(repository.session.value)
            assertNotNull(store.raw()[key])
        }

    @Test
    fun keystoreProviderFailureIsSignedOutButKeepsStoredData() =
        runTest {
            val store = InMemoryPreferencesDataStore()
            repo(store).signIn(sample)
            val repository = repo(store, failingDecrypt(ProviderException("keystore daemon died")))
            advanceUntilIdle()
            assertTrue(repository.loaded.value)
            assertNull(repository.session.value)
            assertNotNull(store.raw()[key])
        }

    private fun failingDecrypt(error: Exception) =
        object : SessionCipher {
            override fun encrypt(plain: ByteArray) = plain

            override fun decrypt(cipher: ByteArray): ByteArray = throw error
        }
}
