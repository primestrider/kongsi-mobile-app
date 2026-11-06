package com.primestrider.kotlinstarter.feature.example.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.primestrider.kotlinstarter.core.auth.AuthAdapter
import com.primestrider.kotlinstarter.core.auth.Session
import com.primestrider.kotlinstarter.core.auth.SessionRepository
import com.primestrider.kotlinstarter.core.auth.SignInCredentials
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject

enum class SignInField { USERNAME, PASSWORD }

data class SignInUiState(
    val username: String = "",
    val password: String = "",
    val shortLived: Boolean = false,
    val errors: Map<SignInField, Int> = emptyMap(),
    val submitAttempted: Boolean = false,
    val loading: Boolean = false,
    val error: ApiError? = null,
)

private const val USERNAME_MIN = 3
private const val PASSWORD_MIN = 6
internal const val DEMO_USERNAME = "emilys"
internal const val DEMO_PASSWORD = "emilyspass"

/** Rules of the RN sign-in schema. Errors are string resources so they follow a language switch. */
fun validateSignIn(
    username: String,
    password: String,
): Map<SignInField, Int> =
    buildMap {
        if (username.trim().length < USERNAME_MIN) put(SignInField.USERNAME, R.string.auth_validation_username_min)
        if (password.length < PASSWORD_MIN) put(SignInField.PASSWORD, R.string.auth_validation_password_min)
    }

/** Validates on submit, then live (same as the form example). A failed sign-in leaves the fields untouched. */
@HiltViewModel
class SignInViewModel
    @Inject
    constructor(
        private val adapter: AuthAdapter,
        private val repository: SessionRepository,
        private val demo: DemoTokenSettings,
        private val messenger: Messenger,
        private val json: Json,
    ) : ViewModel() {
        private val _state = MutableStateFlow(SignInUiState(shortLived = demo.shortLivedTokens))
        val state: StateFlow<SignInUiState> = _state.asStateFlow()

        private val _signedIn = Channel<Unit>(Channel.BUFFERED)

        /** Emits once after a successful sign-in. */
        val signedIn: Flow<Unit> = _signedIn.receiveAsFlow()

        fun onUsernameChange(value: String) = edit { it.copy(username = value) }

        fun onPasswordChange(value: String) = edit { it.copy(password = value) }

        fun onShortLivedChange(value: Boolean) {
            demo.shortLivedTokens = value
            _state.update { it.copy(shortLived = value) }
        }

        fun fillDemo() = edit { it.copy(username = DEMO_USERNAME, password = DEMO_PASSWORD) }

        private fun edit(transform: (SignInUiState) -> SignInUiState) {
            _state.update { current ->
                val next = transform(current)
                next.copy(errors = if (next.submitAttempted) validateSignIn(next.username, next.password) else emptyMap())
            }
        }

        fun submit() {
            val current = _state.value
            if (current.loading) return
            val errors = validateSignIn(current.username, current.password)
            if (errors.isNotEmpty()) {
                _state.update { it.copy(submitAttempted = true, errors = errors) }
                return
            }
            _state.update { it.copy(submitAttempted = true, errors = emptyMap(), loading = true, error = null) }
            val credentials = SignInCredentials(current.username.trim(), current.password)
            viewModelScope.launch {
                when (val result = safeApiCall(json) { adapter.signIn(credentials) }) {
                    is ApiResult.Success -> {
                        // Leaving the screen mid-write must not drop the stored session, the message or the event.
                        withContext(NonCancellable) {
                            if (storeSession(Session(result.data.user, result.data.tokens))) {
                                _state.update { it.copy(loading = false) }
                                messenger.success(UiText.Resource(R.string.auth_signed_in))
                                _signedIn.send(Unit)
                            } else {
                                _state.update { it.copy(loading = false, error = ApiError(null)) }
                            }
                        }
                    }
                    is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.error) }
                }
            }
        }

        /** False when the session could not be stored (storage or keystore failure): shown as the generic error. */
        @Suppress("TooGenericExceptionCaught", "SwallowedException")
        private suspend fun storeSession(session: Session): Boolean =
            try {
                repository.signIn(session)
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
    }
