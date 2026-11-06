package com.primestrider.kotlinstarter.feature.example.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.primestrider.kotlinstarter.core.auth.Session
import com.primestrider.kotlinstarter.core.auth.SessionRepository
import com.primestrider.kotlinstarter.core.auth.SignOutReason
import com.primestrider.kotlinstarter.core.designsystem.component.Messenger
import com.primestrider.kotlinstarter.core.designsystem.component.UiText
import com.primestrider.kotlinstarter.core.network.ApiError
import com.primestrider.kotlinstarter.core.network.ApiResult
import com.primestrider.kotlinstarter.core.network.safeApiCall
import com.primestrider.kotlinstarter.feature.account.impl.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject

/** State of the "Load from /auth/me" demo request. */
sealed interface ProfileLoad {
    data object Idle : ProfileLoad

    data object Loading : ProfileLoad

    data class Loaded(
        val user: UserResponse,
    ) : ProfileLoad

    data class Failed(
        val error: ApiError,
    ) : ProfileLoad
}

/** Session access for the Profile hub and the protected Account page, plus the authenticated `auth/me` demo call. */
@HiltViewModel
class AccountViewModel
    @Inject
    constructor(
        private val repository: SessionRepository,
        private val api: DummyJsonApi,
        private val json: Json,
        private val messenger: Messenger,
    ) : ViewModel() {
        val session: StateFlow<Session?> = repository.session

        /** False until the stored session has been read; until then a null [session] means "not known yet". */
        val loaded: StateFlow<Boolean> = repository.loaded

        private val _profile = MutableStateFlow<ProfileLoad>(ProfileLoad.Idle)
        val profile: StateFlow<ProfileLoad> = _profile.asStateFlow()

        private val _signingOut = MutableStateFlow(false)

        /** True once the user asked to sign out; lets a protected route tell that from a session lost by expiry. */
        val signingOut: StateFlow<Boolean> = _signingOut.asStateFlow()

        private val _signedOut = Channel<Unit>(Channel.BUFFERED)

        /** Emits once after a successful user sign-out. */
        val signedOut: Flow<Unit> = _signedOut.receiveAsFlow()

        fun loadProfile() {
            if (_profile.value == ProfileLoad.Loading) return
            _profile.value = ProfileLoad.Loading
            viewModelScope.launch {
                _profile.value =
                    when (val result = safeApiCall(json) { api.me() }) {
                        is ApiResult.Success -> ProfileLoad.Loaded(result.data)
                        is ApiResult.Failure -> ProfileLoad.Failed(result.error)
                    }
            }
        }

        /** A storage failure keeps the user signed in and shows an error instead of crashing. */
        @Suppress("TooGenericExceptionCaught")
        fun signOut() {
            viewModelScope.launch {
                // Leaving the screen mid-write must not leave the session half removed.
                withContext(NonCancellable) {
                    try {
                        _signingOut.value = true
                        repository.signOut(SignOutReason.USER)
                        messenger.info(UiText.Resource(R.string.auth_signed_out))
                        _signedOut.send(Unit)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        _signingOut.value = false
                        messenger.error(UiText.Resource(R.string.auth_sign_out_failed))
                    }
                }
            }
        }
    }
