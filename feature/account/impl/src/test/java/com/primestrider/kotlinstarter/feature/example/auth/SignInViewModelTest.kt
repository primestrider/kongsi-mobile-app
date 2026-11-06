package com.primestrider.kotlinstarter.feature.example.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.viewModelScope
import com.primestrider.kotlinstarter.core.auth.AuthAdapter
import com.primestrider.kotlinstarter.core.auth.AuthResult
import com.primestrider.kotlinstarter.core.auth.AuthTokens
import com.primestrider.kotlinstarter.core.auth.AuthUser
import com.primestrider.kotlinstarter.core.auth.FakeSessionCipher
import com.primestrider.kotlinstarter.core.auth.SessionRepository
import com.primestrider.kotlinstarter.core.auth.SignInCredentials
import com.primestrider.kotlinstarter.core.designsystem.component.MessageVariant
import com.primestrider.kotlinstarter.core.designsystem.component.Messenger
import com.primestrider.kotlinstarter.core.designsystem.component.UiMessage
import com.primestrider.kotlinstarter.core.designsystem.component.UiText
import com.primestrider.kotlinstarter.core.network.ApiError
import com.primestrider.kotlinstarter.feature.account.impl.R
import com.primestrider.kotlinstarter.testing.GatedPreferencesDataStore
import com.primestrider.kotlinstarter.testing.InMemoryPreferencesDataStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class SignInViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val json = Json { ignoreUnknownKeys = true }
    private val user = AuthUser(1, "emilys", "e@x.com", "Emily", "Johnson")
    private val created = mutableListOf<SignInViewModel>()

    private class FakeAdapter(
        var behavior: suspend () -> AuthResult,
    ) : AuthAdapter {
        var calls = 0
        var last: SignInCredentials? = null

        override suspend fun signIn(credentials: SignInCredentials): AuthResult {
            calls++
            last = credentials
            return behavior()
        }

        override suspend fun refresh(refreshToken: String): AuthTokens = error("not used")
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() {
        created.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(
        adapter: FakeAdapter,
        messenger: Messenger = Messenger(),
        demo: DemoTokenSettings = DemoTokenSettings(),
        store: DataStore<Preferences> = InMemoryPreferencesDataStore(),
    ): Pair<SignInViewModel, SessionRepository> {
        val repository = SessionRepository(store, FakeSessionCipher(), json, backgroundScope)
        return SignInViewModel(adapter, repository, demo, messenger, json).also { created += it } to repository
    }

    private val ok: suspend () -> AuthResult = { AuthResult(user, AuthTokens("a1", "r1")) }

    @Test
    fun invalid_submit_shows_both_errors_and_skips_adapter() =
        runTest(dispatcher) {
            val adapter = FakeAdapter(ok)
            val (vm, _) = viewModel(adapter)
            vm.onUsernameChange("ab")
            vm.onPasswordChange("12345")
            assertTrue(
                vm.state.value.errors
                    .isEmpty(),
            )
            vm.submit()
            runCurrent()
            assertEquals(
                mapOf(
                    SignInField.USERNAME to R.string.auth_validation_username_min,
                    SignInField.PASSWORD to R.string.auth_validation_password_min,
                ),
                vm.state.value.errors,
            )
            assertEquals(0, adapter.calls)
            vm.onUsernameChange("abc")
            assertEquals(setOf(SignInField.PASSWORD), vm.state.value.errors.keys)
        }

    @Test
    fun valid_submit_loads_then_stores_session_announces_and_emits_once() =
        runTest(dispatcher) {
            val adapter = FakeAdapter(ok)
            val messenger = Messenger()
            val (vm, repository) = viewModel(adapter, messenger)
            val emissions = mutableListOf<Unit>()
            backgroundScope.launch { vm.signedIn.collect { emissions += it } }
            runCurrent()
            vm.onUsernameChange(" emilys ")
            vm.onPasswordChange("emilyspass")
            vm.submit()
            assertTrue(vm.state.value.loading)
            runCurrent()
            assertFalse(vm.state.value.loading)
            assertEquals("emilys", adapter.last?.username)
            assertEquals(AuthTokens("a1", "r1"), repository.session.value?.tokens)
            val message = messenger.messages.first()
            assertEquals(MessageVariant.SUCCESS, message.variant)
            assertEquals(UiText.Resource(R.string.auth_signed_in), message.text)
            assertEquals(1, emissions.size)
        }

    @Test
    fun io_failure_is_a_network_error_and_keeps_fields() =
        runTest(dispatcher) {
            val (vm, repository) = viewModel(FakeAdapter { throw IOException("down") })
            vm.onUsernameChange("emilys")
            vm.onPasswordChange("emilyspass")
            vm.submit()
            runCurrent()
            assertTrue(
                vm.state.value.error!!
                    .isNetworkError,
            )
            assertFalse(vm.state.value.loading)
            assertEquals("emilys", vm.state.value.username)
            assertEquals("emilyspass", vm.state.value.password)
            assertNull(repository.session.value)
        }

    @Test
    fun http_400_surfaces_server_message() =
        runTest(dispatcher) {
            val body = """{"message":"Invalid credentials"}""".toResponseBody("application/json".toMediaType())
            val (vm, _) = viewModel(FakeAdapter { throw HttpException(Response.error<Any>(400, body)) })
            vm.onUsernameChange("emilys")
            vm.onPasswordChange("wrongpass")
            vm.submit()
            runCurrent()
            assertEquals(
                "Invalid credentials",
                vm.state.value.error
                    ?.message,
            )
            assertEquals(
                400,
                vm.state.value.error
                    ?.status,
            )
        }

    @Test
    fun session_storage_failure_is_a_generic_error_and_keeps_fields() =
        runTest(dispatcher) {
            val store = GatedPreferencesDataStore().apply { writeError = IOException("disk full") }
            val messenger = Messenger()
            val (vm, repository) = viewModel(FakeAdapter(ok), messenger, store = store)
            val emissions = mutableListOf<Unit>()
            val messages = mutableListOf<UiMessage>()
            backgroundScope.launch { vm.signedIn.collect { emissions += it } }
            backgroundScope.launch { messenger.messages.collect { messages += it } }
            vm.onUsernameChange("emilys")
            vm.onPasswordChange("emilyspass")
            vm.submit()
            runCurrent()
            assertFalse(vm.state.value.loading)
            assertEquals(ApiError(null), vm.state.value.error)
            assertEquals("emilys", vm.state.value.username)
            assertEquals("emilyspass", vm.state.value.password)
            assertNull(repository.session.value)
            assertTrue(emissions.isEmpty())
            assertTrue(messages.isEmpty())
        }

    @Test
    fun double_submit_calls_adapter_once() =
        runTest(dispatcher) {
            val gate = CompletableDeferred<AuthResult>()
            val adapter = FakeAdapter { gate.await() }
            val (vm, _) = viewModel(adapter)
            vm.onUsernameChange("emilys")
            vm.onPasswordChange("emilyspass")
            vm.submit()
            vm.submit()
            runCurrent()
            gate.complete(AuthResult(user, AuthTokens("a1", "r1")))
            runCurrent()
            assertEquals(1, adapter.calls)
        }

    @Test
    fun fillDemo_and_short_lived_toggle() =
        runTest(dispatcher) {
            val demo = DemoTokenSettings()
            val (vm, _) = viewModel(FakeAdapter(ok), demo = demo)
            vm.fillDemo()
            assertEquals("emilys", vm.state.value.username)
            assertEquals("emilyspass", vm.state.value.password)
            vm.onShortLivedChange(true)
            assertTrue(vm.state.value.shortLived)
            assertTrue(demo.shortLivedTokens)
        }

    @Test
    fun next_submit_clears_previous_error_while_loading() =
        runTest(dispatcher) {
            val gate = CompletableDeferred<AuthResult>()
            val adapter = FakeAdapter { throw IOException("down") }
            val (vm, _) = viewModel(adapter)
            vm.onUsernameChange("emilys")
            vm.onPasswordChange("emilyspass")
            vm.submit()
            runCurrent()
            assertTrue(vm.state.value.error != null)

            adapter.behavior = { gate.await() }
            vm.submit()
            assertNull(vm.state.value.error)
            assertTrue(vm.state.value.loading)
            runCurrent()
            gate.complete(AuthResult(user, AuthTokens("a1", "r1")))
            runCurrent()
            assertFalse(vm.state.value.loading)
        }
}
