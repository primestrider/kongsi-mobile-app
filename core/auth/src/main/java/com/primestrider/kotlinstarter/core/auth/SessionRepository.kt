package com.primestrider.kotlinstarter.core.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.primestrider.kotlinstarter.core.common.ApplicationScope
import com.primestrider.kotlinstarter.core.datastore.SessionStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import javax.crypto.AEADBadTagException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Single source of truth for the signed-in session, persisted encrypted. Signed in iff a session
 * (and so tokens) exists. Unreadable stored data counts as signed out; it is removed only when it can
 * never be read again.
 * All mutations are serialized by a mutex so memory and disk cannot diverge.
 */
@OptIn(ExperimentalEncodingApi::class)
@Singleton
class SessionRepository
    @Inject
    constructor(
        @SessionStore private val dataStore: DataStore<Preferences>,
        private val cipher: SessionCipher,
        private val json: Json,
        @ApplicationScope scope: CoroutineScope,
    ) {
        private val _session = MutableStateFlow<Session?>(null)
        val session: StateFlow<Session?> = _session.asStateFlow()

        private val _loaded = MutableStateFlow(false)

        /** True once the stored session has been read for the first time. */
        val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

        private val _signOutReason = MutableStateFlow<SignOutReason?>(null)
        val signOutReason: StateFlow<SignOutReason?> = _signOutReason.asStateFlow()

        val isSignedIn: Boolean get() = _session.value != null

        private val lock = Mutex()

        /**
         * Access tokens the current session has replaced, tagged with the session they belong to so a reader
         * never mixes one session with another session's tokens. Memory only. Written under [lock], before
         * [_session], so a reader that sees a session also sees its set.
         */
        @Volatile private var superseded: SupersededTokens? = null

        init {
            scope.launch {
                try {
                    lock.withLock {
                        // A sign-in that raced the initial read wins.
                        _session.compareAndSet(null, read())
                    }
                } finally {
                    _loaded.value = true
                }
            }
        }

        fun accessToken(): String? = _session.value?.tokens?.accessToken

        /**
         * True when [token] is an access token that [session] (a value read from [session]) replaced by a
         * refresh. False for its current token, for a token of another session, and once [session] is no
         * longer current (signed out, another sign-in, or refreshed again: the set then belongs to the newer
         * snapshot). Pass the same snapshot the decision is based on, so both come from one session state.
         */
        internal fun isSupersededToken(
            token: String,
            session: Session,
        ): Boolean {
            val replaced = superseded ?: return false
            return replaced.session === session && token in replaced.tokens
        }

        suspend fun signIn(session: Session) {
            lock.withLock {
                _signOutReason.value = null
                write(session)
                superseded = null
                _session.value = session
            }
        }

        /**
         * Stores refreshed [tokens], but only while the session is still the one whose refresh token was
         * [expectedRefreshToken]. Returns false (and changes nothing) after a sign-out or a sign-in of
         * another session, so a stale refresh can never resurrect or overwrite a session.
         */
        suspend fun updateTokens(
            expectedRefreshToken: String,
            tokens: AuthTokens,
        ): Boolean =
            lock.withLock {
                val current = _session.value
                if (current == null || current.tokens.refreshToken != expectedRefreshToken) return@withLock false
                val updated = current.copy(tokens = tokens)
                write(updated)
                val earlier = superseded?.takeIf { it.session === current }?.tokens.orEmpty()
                superseded = SupersededTokens(updated, (earlier + current.tokens.accessToken).takeLast(MAX_SUPERSEDED_TOKENS))
                _session.value = updated
                true
            }

        suspend fun signOut(reason: SignOutReason) {
            lock.withLock { clear(reason) }
        }

        /**
         * Signs out with [reason] only while the session is still the one whose refresh token was
         * [expectedRefreshToken]. Returns false (and changes nothing) when the user already signed out
         * or another session signed in.
         */
        suspend fun signOutIfCurrent(
            expectedRefreshToken: String,
            reason: SignOutReason,
        ): Boolean =
            lock.withLock {
                if (_session.value?.tokens?.refreshToken != expectedRefreshToken) return@withLock false
                clear(reason)
                true
            }

        fun clearSignOutReason() {
            _signOutReason.value = null
        }

        /** Must be called holding [lock]. Storage first: if it fails, memory and disk both still say signed in. */
        private suspend fun clear(reason: SignOutReason) {
            dataStore.edit { it.remove(KEY) }
            superseded = null
            _session.value = null
            _signOutReason.value = reason
        }

        private suspend fun write(session: Session) {
            val plain = json.encodeToString(Session.serializer(), session).toByteArray(Charsets.UTF_8)
            dataStore.edit { it[KEY] = Base64.encode(cipher.encrypt(plain)) }
        }

        /**
         * Any failure means signed out for this run. The stored value is removed only when it can never be
         * read again ([isUnreadableForGood]); a storage or transient keystore failure keeps it for the next run.
         */
        @Suppress("TooGenericExceptionCaught", "SwallowedException")
        private suspend fun read(): Session? =
            try {
                val stored = dataStore.data.first()[KEY]
                if (stored == null) {
                    null
                } else {
                    val plain = cipher.decrypt(Base64.decode(stored))
                    json.decodeFromString(Session.serializer(), String(plain, Charsets.UTF_8))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (isUnreadableForGood(e)) removeStored()
                null
            }

        @Suppress("TooGenericExceptionCaught", "SwallowedException")
        private suspend fun removeStored() {
            try {
                dataStore.edit { it.remove(KEY) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Storage is unusable; staying signed out in memory is the best we can do.
            }
        }

        private companion object {
            val KEY = stringPreferencesKey("auth.session")
        }
    }

/** The access tokens [session] has replaced, oldest first; only meaningful while [session] is the current one. */
private class SupersededTokens(
    val session: Session,
    val tokens: List<String>,
)

/**
 * How many replaced access tokens a session remembers. A request still open across this many refreshes
 * is long dead, so older ones only cost memory: they then get `SignedOut`, i.e. the caller sees the 401.
 */
private const val MAX_SUPERSEDED_TOKENS = 8

/**
 * True when stored session data can never be decrypted or parsed: a failed authentication tag, a key that
 * is gone for good, or a value that is not valid Base64 / ciphertext / JSON. Anything else (I/O, a busy or
 * crashed keystore) may succeed on the next run.
 */
internal fun isUnreadableForGood(e: Throwable): Boolean =
    e is AEADBadTagException || isPermanentKeyFailure(e) || e is IllegalArgumentException
