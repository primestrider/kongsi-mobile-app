package com.primestrider.kotlinstarter.feature.example.sdk.integrity

import android.content.Context
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityManager.PrepareIntegrityTokenRequest
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenProvider
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenRequest
import com.primestrider.kotlinstarter.feature.devices.impl.BuildConfig
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton

private const val TOKEN_PREVIEW = 24

/** The Google Cloud project number linked in Play Console (`BuildConfig.PLAY_INTEGRITY_PROJECT`); 0 = not set. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PlayIntegrityProject

sealed interface IntegrityState {
    data object NotConfigured : IntegrityState

    data object Ready : IntegrityState

    data object Requesting : IntegrityState

    /** The token is encrypted and signed by Google: only your server can decode and verify it. */
    data class Token(
        val preview: String,
        val length: Int,
    ) : IntegrityState

    data class Failed(
        val message: String,
    ) : IntegrityState
}

/** Gets a Play Integrity token bound to a request hash; the Play implementation is [PlayIntegrityTokens]. */
fun interface IntegrityTokens {
    suspend fun token(requestHash: String): String
}

/**
 * The SHA-256 of [action] (base64url, no padding). Bind a token to the exact request it protects ("transfer:100"),
 * so a token captured for one request cannot be replayed for another; the server recomputes and compares it.
 */
fun requestHash(action: String): String =
    Base64.encodeToString(
        MessageDigest.getInstance("SHA-256").digest(action.toByteArray()),
        Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
    )

/** Requests a token for a sample action and shows a short preview of it; the verdict itself is the server's job. */
@HiltViewModel
class IntegrityViewModel
    @Inject
    constructor(
        @PlayIntegrityProject private val projectNumber: Long,
        private val tokens: IntegrityTokens,
    ) : ViewModel() {
        private val _state = MutableStateFlow(if (projectNumber == 0L) IntegrityState.NotConfigured else IntegrityState.Ready)
        val state: StateFlow<IntegrityState> = _state.asStateFlow()

        fun request(action: String) {
            if (projectNumber == 0L) return
            _state.value = IntegrityState.Requesting
            viewModelScope.launch {
                _state.value =
                    try {
                        val token = tokens.token(requestHash(action))
                        IntegrityState.Token(preview = token.take(TOKEN_PREVIEW) + "…", length = token.length)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        IntegrityState.Failed(e.message ?: e.javaClass.simpleName)
                    }
            }
        }
    }

/**
 * Prepares a token provider once and reuses it. Play documents that a provider can expire or become invalid
 * (`INTEGRITY_TOKEN_PROVIDER_INVALID`), so a failed request drops it, prepares a new one and retries once.
 */
class ReusableTokenProvider<P : Any>(
    private val prepare: suspend () -> P,
    private val request: suspend (provider: P, requestHash: String) -> String,
) : IntegrityTokens {
    private val mutex = Mutex()
    private var provider: P? = null

    private suspend fun provider(): P = mutex.withLock { provider ?: prepare().also { provider = it } }

    override suspend fun token(requestHash: String): String {
        val first = provider()
        return try {
            request(first, requestHash)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            mutex.withLock { if (provider === first) provider = null }
            request(provider(), requestHash)
        }
    }
}

/**
 * Standard requests: prepare a token provider (it warms up Play's checks in the background), then request a token per
 * protected action. Without Play services or outside Play the calls fail with an `IntegrityServiceException`.
 */
@Singleton
class PlayIntegrityTokens
    @Inject
    constructor(
        @ApplicationContext context: Context,
        @PlayIntegrityProject projectNumber: Long,
    ) : IntegrityTokens by ReusableTokenProvider<StandardIntegrityTokenProvider>(
            prepare = {
                IntegrityManagerFactory
                    .createStandard(context)
                    .prepareIntegrityToken(PrepareIntegrityTokenRequest.builder().setCloudProjectNumber(projectNumber).build())
                    .await()
            },
            request = { provider, requestHash ->
                provider.request(StandardIntegrityTokenRequest.builder().setRequestHash(requestHash).build()).await().token()
            },
        )

@Module
@InstallIn(SingletonComponent::class)
abstract class IntegrityModule {
    @Binds
    abstract fun tokens(impl: PlayIntegrityTokens): IntegrityTokens

    companion object {
        @Provides
        @PlayIntegrityProject
        fun projectNumber(): Long = BuildConfig.PLAY_INTEGRITY_PROJECT
    }
}
