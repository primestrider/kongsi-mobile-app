package com.primestrider.kotlinstarter.core.auth

import com.primestrider.kotlinstarter.core.common.ApplicationScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

sealed interface RefreshOutcome {
    /** Retry with [accessToken]. */
    data class Refreshed(
        val accessToken: String,
    ) : RefreshOutcome

    /** Signed out (already, or because the refresh was rejected): give up. */
    data object SignedOut : RefreshOutcome

    /** The refresh could not reach the server; the session is kept. */
    data object NetworkFailure : RefreshOutcome
}

/**
 * Single-flight token refresh: concurrent callers await the same attempt and share its outcome.
 * An [IOException], or an [HttpException] with a 5xx or 429 status, from the adapter is a
 * [RefreshOutcome.NetworkFailure] and keeps the session; any other failure signs out with
 * [SignOutReason.EXPIRED]. A cancelled attempt is also a [RefreshOutcome.NetworkFailure]. Catches every
 * [Exception]; only the caller's own cancellation propagates. [Error]s (e.g. `NotImplementedError` from an
 * unfinished adapter) are not caught and still propagate.
 *
 * An attempt only ever touches the session it started for (matched by refresh token): if the user signed
 * out or another session signed in meanwhile, its new tokens are dropped, a rejection signs no one out,
 * and the outcome is [RefreshOutcome.SignedOut]. Likewise a 401 joins only an attempt started for the current
 * session, and a failed token other than the current one is retried with the current one only when the
 * current session itself replaced it ([SessionRepository.isSupersededToken]); a token of a previous session
 * gets [RefreshOutcome.SignedOut], so a late 401 is never replayed under another user's bearer.
 *
 * The attempt runs in the application scope, so cancelling the call that triggered it does not
 * cancel an in-flight refresh.
 */
@Singleton
class SessionRefresher
    @Inject
    constructor(
        private val repository: SessionRepository,
        private val adapter: AuthAdapter,
        @ApplicationScope private val scope: CoroutineScope,
    ) {
        private val lock = Any()
        private var inFlight: InFlight? = null

        /** For tests: whether an attempt is currently registered for callers to join. */
        internal val hasAttemptInFlight: Boolean get() = synchronized(lock) { inFlight != null }

        suspend fun refresh(failedAccessToken: String?): RefreshOutcome {
            val attempt =
                synchronized(lock) {
                    val session = repository.session.value ?: return RefreshOutcome.SignedOut
                    val current = session.tokens
                    if (current.accessToken != failedAccessToken) {
                        // Retry with the stored token only when this session itself refreshed since the failed
                        // request was sent. A token of a previous session must never be retried with this one's.
                        val refreshedSince = failedAccessToken != null && repository.isSupersededToken(failedAccessToken, session)
                        return if (refreshedSince) RefreshOutcome.Refreshed(current.accessToken) else RefreshOutcome.SignedOut
                    }
                    // Join only an attempt for this session; one left over from a previous session is not ours.
                    inFlight?.takeIf { it.refreshToken == current.refreshToken }?.deferred ?: startAttempt(current.refreshToken)
                }
            return try {
                attempt.await()
            } catch (e: CancellationException) {
                // The caller itself was cancelled: propagate. Otherwise the attempt was (the scope
                // is shutting down), which is no verdict on the session.
                currentCoroutineContext().ensureActive()
                RefreshOutcome.NetworkFailure
            }
        }

        /** Must be called holding [lock]. */
        private fun startAttempt(refreshToken: String): Deferred<RefreshOutcome> {
            val deferred =
                scope.async {
                    val self = coroutineContext.job
                    try {
                        attempt(refreshToken)
                    } finally {
                        // Cleared before the result is published, so no caller joins a finished attempt.
                        // Only this attempt: a newer one (for another session) may have replaced it.
                        synchronized(lock) { if (inFlight?.deferred === self) inFlight = null }
                    }
                }
            inFlight = InFlight(refreshToken, deferred)
            // Also covers an attempt whose body never ran (scope already cancelled).
            deferred.invokeOnCompletion { synchronized(lock) { if (inFlight?.deferred === deferred) inFlight = null } }
            return deferred
        }

        /** An attempt and the refresh token (so the session) it was started for. */
        private class InFlight(
            val refreshToken: String,
            val deferred: Deferred<RefreshOutcome>,
        )

        @Suppress("TooGenericExceptionCaught", "SwallowedException")
        private suspend fun attempt(refreshToken: String): RefreshOutcome {
            val tokens =
                try {
                    adapter.refresh(refreshToken)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: IOException) {
                    return RefreshOutcome.NetworkFailure
                } catch (e: HttpException) {
                    return if (e.isTransient()) RefreshOutcome.NetworkFailure else expire(refreshToken)
                } catch (e: Exception) {
                    return expire(refreshToken)
                }
            return try {
                // False when the user signed out or someone else signed in meanwhile: never retry for them.
                val stored = repository.updateTokens(refreshToken, tokens)
                if (stored) RefreshOutcome.Refreshed(tokens.accessToken) else RefreshOutcome.SignedOut
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The old refresh token is likely spent; without storing the new one the session is unusable.
                expire(refreshToken)
            }
        }

        @Suppress("TooGenericExceptionCaught", "SwallowedException")
        private suspend fun expire(refreshToken: String): RefreshOutcome {
            try {
                // Only the session this attempt refreshed; a later sign-out or sign-in is left alone.
                repository.signOutIfCurrent(refreshToken, SignOutReason.EXPIRED)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Storage failed: the session stays as it was and the caller simply gets the 401.
            }
            return RefreshOutcome.SignedOut
        }
    }

/** A server error or rate limit says nothing about the refresh token, so the session is kept. */
private fun HttpException.isTransient(): Boolean = code() >= SERVER_ERROR || code() == TOO_MANY_REQUESTS

private const val SERVER_ERROR = 500
private const val TOO_MANY_REQUESTS = 429
