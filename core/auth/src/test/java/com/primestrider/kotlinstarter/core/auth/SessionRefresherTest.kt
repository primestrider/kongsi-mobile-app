package com.primestrider.kotlinstarter.core.auth

import com.primestrider.kotlinstarter.testing.GatedPreferencesDataStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class SessionRefresherTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val user = AuthUser(1, "emilys", "e@x.com", "Emily", "Johnson")
    private val tokens = AuthTokens("a1", "r1")
    private val nextUser = Session(AuthUser(2, "bob", "b@x.com", "Bob", "Smith"), AuthTokens("b1", "rb1"))

    private fun TestScope.repo(store: GatedPreferencesDataStore = GatedPreferencesDataStore()) =
        SessionRepository(store, FakeSessionCipher(), json, backgroundScope)

    private fun TestScope.refresher(
        repository: SessionRepository,
        adapter: AuthAdapter,
    ) = SessionRefresher(repository, adapter, backgroundScope)

    private suspend fun TestScope.signedInRepo(store: GatedPreferencesDataStore = GatedPreferencesDataStore()) =
        repo(store).also { it.signIn(Session(user, tokens)) }

    @Test
    fun tokenSupersededInThisSessionReturnsCurrentTokenWithoutCallingAdapter() =
        runTest {
            val repository = signedInRepo()
            assertTrue(repository.updateTokens("r1", AuthTokens("a2", "r2")))
            val adapter = FakeAuthAdapter()
            assertEquals(RefreshOutcome.Refreshed("a2"), refresher(repository, adapter).refresh("a1"))
            assertEquals(0, adapter.refreshCount)
        }

    @Test
    fun tokenOfAPreviousSessionIsSignedOutAndNeverRetriedWithTheNextUsersToken() =
        runTest {
            val repository = signedInRepo()
            repository.signOut(SignOutReason.USER)
            repository.signIn(Session(nextUser.user, AuthTokens("b1", "r2")))
            val adapter = FakeAuthAdapter()
            assertEquals(RefreshOutcome.SignedOut, refresher(repository, adapter).refresh("a1"))
            assertEquals(0, adapter.refreshCount)
            assertEquals(AuthTokens("b1", "r2"), repository.session.value?.tokens)
            assertNull(repository.signOutReason.value)
        }

    @Test
    fun nextUsersUnauthorizedStartsItsOwnAttemptInsteadOfJoiningThePreviousSessions() =
        runTest {
            val repository = signedInRepo()
            val gate = CompletableDeferred<Unit>()
            val adapter = FakeAuthAdapter(next = AuthTokens("b2", "r3"), onRefresh = { gate.await() })
            val refresher = refresher(repository, adapter)
            val first = async { refresher.refresh("a1") }
            runCurrent()
            repository.signOut(SignOutReason.USER)
            repository.signIn(Session(nextUser.user, AuthTokens("b1", "r2")))
            val second = async { refresher.refresh("b1") }
            runCurrent()
            assertEquals(2, adapter.refreshCount)
            assertEquals("r2", adapter.lastRefreshToken)
            gate.complete(Unit)
            assertEquals(RefreshOutcome.SignedOut, first.await())
            assertEquals(RefreshOutcome.Refreshed("b2"), second.await())
            assertEquals(AuthTokens("b2", "r3"), repository.session.value?.tokens)
            assertFalse(refresher.hasAttemptInFlight)
        }

    @Test
    fun signedOutReturnsSignedOutWithoutCallingAdapter() =
        runTest {
            val adapter = FakeAuthAdapter()
            assertEquals(RefreshOutcome.SignedOut, refresher(repo(), adapter).refresh("a1"))
            assertEquals(0, adapter.refreshCount)
        }

    @Test
    fun refreshStoresAndReturnsNewToken() =
        runTest {
            val repository = signedInRepo()
            val adapter = FakeAuthAdapter()
            assertEquals(RefreshOutcome.Refreshed("a2"), refresher(repository, adapter).refresh("a1"))
            assertEquals(AuthTokens("a2", "r2"), repository.session.value?.tokens)
            assertEquals("r1", adapter.lastRefreshToken)
        }

    @Test
    fun networkFailureKeepsSession() =
        runTest {
            val repository = signedInRepo()
            val adapter = FakeAuthAdapter(failure = IOException("offline"))
            assertEquals(RefreshOutcome.NetworkFailure, refresher(repository, adapter).refresh("a1"))
            assertEquals(tokens, repository.session.value?.tokens)
            assertNull(repository.signOutReason.value)
        }

    @Test
    fun rejectedRefreshSignsOutAsExpired() =
        runTest {
            val repository = signedInRepo()
            val rejected = HttpException(Response.error<Any>(401, "".toResponseBody()))
            val adapter = FakeAuthAdapter(failure = rejected)
            assertEquals(RefreshOutcome.SignedOut, refresher(repository, adapter).refresh("a1"))
            assertNull(repository.session.value)
            assertEquals(SignOutReason.EXPIRED, repository.signOutReason.value)
        }

    @Test
    fun unexpectedFailureSignsOutAsExpired() =
        runTest {
            val repository = signedInRepo()
            val adapter = FakeAuthAdapter(failure = IllegalStateException("bad body"))
            assertEquals(RefreshOutcome.SignedOut, refresher(repository, adapter).refresh("a1"))
            assertNull(repository.session.value)
            assertEquals(SignOutReason.EXPIRED, repository.signOutReason.value)
        }

    @Test
    fun concurrentCallersShareOneAttemptAndItsOutcome() =
        runTest {
            val repository = signedInRepo()
            val adapter = FakeAuthAdapter(failure = IOException("offline"), onRefresh = { delay(100) })
            val refresher = refresher(repository, adapter)
            val outcomes = List(3) { async { refresher.refresh("a1") } }.awaitAll()
            assertEquals(List(3) { RefreshOutcome.NetworkFailure }, outcomes)
            assertEquals(1, adapter.refreshCount)
        }

    @Test
    fun cancelledScopeIsNetworkFailureAndDoesNotWedgeLaterRefreshes() =
        runTest {
            val repository = signedInRepo()
            val adapter = FakeAuthAdapter()
            val deadScope = CoroutineScope(Job().apply { cancel() })
            val refresher = SessionRefresher(repository, adapter, deadScope)
            assertEquals(RefreshOutcome.NetworkFailure, refresher.refresh("a1"))
            assertFalse(refresher.hasAttemptInFlight)
            assertEquals(RefreshOutcome.NetworkFailure, refresher.refresh("a1"))
            assertFalse(refresher.hasAttemptInFlight)
            assertEquals(0, adapter.refreshCount)
            assertEquals(tokens, repository.session.value?.tokens)
        }

    @Test
    fun storageFailureDuringExpirySignOutDoesNotThrow() =
        runTest {
            val store = GatedPreferencesDataStore()
            val repository = signedInRepo(store)
            store.writeError = IOException("disk")
            val adapter = FakeAuthAdapter(failure = IllegalStateException("bad body"))
            assertEquals(RefreshOutcome.SignedOut, refresher(repository, adapter).refresh("a1"))
        }

    @Test
    fun serverErrorFromRefreshIsNetworkFailureAndKeepsSession() =
        runTest {
            val repository = signedInRepo()
            val adapter = FakeAuthAdapter(failure = httpError(503))
            assertEquals(RefreshOutcome.NetworkFailure, refresher(repository, adapter).refresh("a1"))
            assertEquals(tokens, repository.session.value?.tokens)
            assertNull(repository.signOutReason.value)
        }

    @Test
    fun rateLimitedRefreshIsNetworkFailureAndKeepsSession() =
        runTest {
            val repository = signedInRepo()
            val adapter = FakeAuthAdapter(failure = httpError(429))
            assertEquals(RefreshOutcome.NetworkFailure, refresher(repository, adapter).refresh("a1"))
            assertEquals(tokens, repository.session.value?.tokens)
            assertNull(repository.signOutReason.value)
        }

    @Test
    fun userSignOutDuringInFlightRefreshIsNotResurrected() =
        runTest {
            val repository = signedInRepo()
            val gate = CompletableDeferred<Unit>()
            val refresher = refresher(repository, FakeAuthAdapter(onRefresh = { gate.await() }))
            val outcome = async { refresher.refresh("a1") }
            runCurrent()
            repository.signOut(SignOutReason.USER)
            gate.complete(Unit)
            assertEquals(RefreshOutcome.SignedOut, outcome.await())
            assertNull(repository.session.value)
            assertEquals(SignOutReason.USER, repository.signOutReason.value)
        }

    @Test
    fun staleRefreshDoesNotTouchTheNextUsersTokens() =
        runTest {
            val repository = signedInRepo()
            val gate = CompletableDeferred<Unit>()
            val refresher = refresher(repository, FakeAuthAdapter(onRefresh = { gate.await() }))
            val outcome = async { refresher.refresh("a1") }
            runCurrent()
            repository.signOut(SignOutReason.USER)
            repository.signIn(nextUser)
            gate.complete(Unit)
            assertEquals(RefreshOutcome.SignedOut, outcome.await())
            assertEquals(nextUser, repository.session.value)
        }

    @Test
    fun staleRejectedRefreshDoesNotSignOutTheNextUser() =
        runTest {
            val repository = signedInRepo()
            val gate = CompletableDeferred<Unit>()
            val adapter = FakeAuthAdapter(failure = httpError(401), onRefresh = { gate.await() })
            val refresher = refresher(repository, adapter)
            val outcome = async { refresher.refresh("a1") }
            runCurrent()
            repository.signOut(SignOutReason.USER)
            repository.signIn(nextUser)
            gate.complete(Unit)
            assertEquals(RefreshOutcome.SignedOut, outcome.await())
            assertEquals(nextUser, repository.session.value)
            assertNull(repository.signOutReason.value)
        }

    private fun httpError(code: Int) = HttpException(Response.error<Any>(code, "".toResponseBody()))
}
