package com.primestrider.kotlinstarter.feature.example.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.viewModelScope
import com.primestrider.kotlinstarter.core.auth.AuthTokens
import com.primestrider.kotlinstarter.core.auth.AuthUser
import com.primestrider.kotlinstarter.core.auth.FakeSessionCipher
import com.primestrider.kotlinstarter.core.auth.Session
import com.primestrider.kotlinstarter.core.auth.SessionRepository
import com.primestrider.kotlinstarter.core.designsystem.component.MessageVariant
import com.primestrider.kotlinstarter.core.designsystem.component.Messenger
import com.primestrider.kotlinstarter.core.designsystem.component.UiText
import com.primestrider.kotlinstarter.feature.account.impl.R
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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val json = Json { ignoreUnknownKeys = true }
    private val user = AuthUser(1, "emilys", "e@x.com", "Emily", "Johnson")
    private val session = Session(user, AuthTokens("a1", "r1"))
    private lateinit var server: MockWebServer
    private lateinit var api: DummyJsonApi
    private val created = mutableListOf<AccountViewModel>()

    /** Writes can be made to fail to exercise a broken storage layer. */
    private class FlakyDataStore(
        private val delegate: DataStore<Preferences> = InMemoryPreferencesDataStore(),
    ) : DataStore<Preferences> {
        var failWrites = false
        override val data = delegate.data

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            if (failWrites) throw IOException("disk full")
            return delegate.updateData(transform)
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        server = MockWebServer()
        server.start()
        api =
            Retrofit
                .Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(DummyJsonApi::class.java)
    }

    @After
    fun tearDown() {
        created.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
        server.close()
    }

    private suspend fun TestScope.viewModel(
        messenger: Messenger = Messenger(),
        store: DataStore<Preferences> = InMemoryPreferencesDataStore(),
        signedIn: Boolean = true,
        api: DummyJsonApi = this@AccountViewModelTest.api,
    ): Pair<AccountViewModel, SessionRepository> {
        val repository = SessionRepository(store, FakeSessionCipher(), json, backgroundScope)
        runCurrent()
        if (signedIn) repository.signIn(session)
        return AccountViewModel(repository, api, json, messenger).also { created += it } to repository
    }

    private suspend fun AccountViewModel.awaitResult(): ProfileLoad =
        withContext(Dispatchers.Default) {
            withTimeout(5_000) { profile.first { it is ProfileLoad.Loaded || it is ProfileLoad.Failed } }
        }

    @Test
    fun loadProfile_goes_loading_then_loaded() =
        runTest(dispatcher) {
            val gate = CompletableDeferred<UserResponse>()
            val gatedApi =
                object : DummyJsonApi {
                    override suspend fun login(body: LoginRequest): LoginResponse = error("not used")

                    override suspend fun me(): UserResponse = gate.await()
                }
            val (vm, _) = viewModel(api = gatedApi)
            assertEquals(ProfileLoad.Idle, vm.profile.value)
            vm.loadProfile()
            runCurrent()
            assertEquals(ProfileLoad.Loading, vm.profile.value)
            val user = UserResponse(1, "emilys", "e@x.com", "Emily", "Johnson")
            gate.complete(user)
            runCurrent()
            assertEquals(ProfileLoad.Loaded(user), vm.profile.value)
        }

    @Test
    fun server_error_is_failed() =
        runTest(dispatcher) {
            server.enqueue(
                MockResponse
                    .Builder()
                    .code(500)
                    .body("""{"message":"boom"}""")
                    .build(),
            )
            val (vm, _) = viewModel()
            vm.loadProfile()
            runCurrent()
            val failed = vm.awaitResult() as ProfileLoad.Failed
            assertEquals(500, failed.error.status)
            assertEquals("boom", failed.error.message)
        }

    @Test
    fun signOut_clears_session_and_emits_one_info_message() =
        runTest(dispatcher) {
            val messenger = Messenger()
            val (vm, repository) = viewModel(messenger)
            val signedOut = mutableListOf<Unit>()
            backgroundScope.launch { vm.signedOut.collect { signedOut += it } }
            runCurrent()
            assertEquals(session, vm.session.value)
            vm.signOut()
            runCurrent()
            assertNull(repository.session.value)
            assertNull(vm.session.value)
            val message = messenger.messages.first()
            assertEquals(MessageVariant.INFO, message.variant)
            assertEquals(UiText.Resource(R.string.auth_signed_out), message.text)
            assertEquals(true, vm.signingOut.value)
            assertEquals(1, signedOut.size)
        }

    @Test
    fun signOut_storage_failure_shows_error_and_keeps_session() =
        runTest(dispatcher) {
            val messenger = Messenger()
            val store = FlakyDataStore()
            val (vm, repository) = viewModel(messenger, store)
            store.failWrites = true
            vm.signOut()
            runCurrent()
            assertNotNull(repository.session.value)
            val message = messenger.messages.first()
            assertEquals(MessageVariant.ERROR, message.variant)
            assertEquals(UiText.Resource(R.string.auth_sign_out_failed), message.text)
            assertEquals(false, vm.signingOut.value)
        }
}
