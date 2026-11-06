package com.primestrider.kotlinstarter.core.auth

import com.primestrider.kotlinstarter.core.network.ApiError
import com.primestrider.kotlinstarter.core.network.ApiResult
import com.primestrider.kotlinstarter.core.network.Item
import com.primestrider.kotlinstarter.core.network.NetworkModule
import com.primestrider.kotlinstarter.core.network.TestApi
import com.primestrider.kotlinstarter.core.network.safeApiCall
import com.primestrider.kotlinstarter.testing.InMemoryPreferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.io.IOException
import java.util.Optional
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Provider
import kotlin.time.Duration.Companion.seconds

/** End to end through the app's OkHttp client: bearer header, 401 refresh, retry and expiry. */
class AuthFlowTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val user = AuthUser(1, "emilys", "e@x.com", "Emily", "Johnson")
    private val refreshScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var server: MockWebServer
    private lateinit var repository: SessionRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        refreshScope.cancel()
        runCatching { server.close() }
    }

    private fun <T> retrofitApi(
        client: OkHttpClient,
        type: Class<T>,
    ): T =
        Retrofit
            .Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(type)

    /** The app's client with its default dispatcher limits. */
    private suspend fun TestScope.signedInApi(
        adapter: AuthAdapter,
        apiUrl: HttpUrl = server.url("/"),
    ): TestApi {
        repository = SessionRepository(InMemoryPreferencesDataStore(), FakeSessionCipher(), json, backgroundScope)
        repository.signIn(Session(user, AuthTokens("a1", "r1")))
        val refresher = SessionRefresher(repository, adapter, refreshScope)
        val client =
            NetworkModule.okHttpClient(
                setOf(AuthInterceptor(repository, apiUrl)),
                Optional.of(TokenAuthenticator(Provider { refresher }, apiUrl)),
                apiUrl,
                emptySet(),
            )
        return retrofitApi(client, TestApi::class.java)
    }

    private fun response(code: Int) =
        MockResponse
            .Builder()
            .code(code)
            .body(if (code == 200) """{"id":1}""" else """{"message":"Unauthorized"}""")
            .build()

    /** `auth/refresh` returns `a2`/`r2`; other paths answer 200 only to `Bearer a2`. */
    private fun answerOnlyRefreshedToken() {
        server.dispatcher =
            object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse =
                    if (request.url.encodedPath == "/auth/refresh") {
                        MockResponse.Builder().body("""{"accessToken":"a2","refreshToken":"r2"}""").build()
                    } else {
                        response(if (request.headers["Authorization"] == "Bearer a2") 200 else 401)
                    }
            }
    }

    private fun alwaysUnauthorized() {
        server.dispatcher =
            object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse = response(401)
            }
    }

    /**
     * Holds the refresh until [count] requests reached the server. Each holds a dispatcher slot
     * until its call finishes, so with [count] = the per-host limit every slot is taken.
     */
    private suspend fun awaitRequests(count: Int) {
        while (server.requestCount < count) delay(10)
    }

    @Test
    fun authenticatedCallCarriesBearerAndAnonymousCallDoesNot() =
        runTest {
            val api = signedInApi(FakeAuthAdapter())
            server.enqueue(response(200))
            server.enqueue(response(200))
            api.item()
            api.open()
            assertEquals("Bearer a1", server.takeRequest().headers["Authorization"])
            assertNull(server.takeRequest().headers["Authorization"])
        }

    @Test
    fun callToAnotherHostCarriesNoBearerAndIsNotRefreshed() =
        runTest {
            val adapter = FakeAuthAdapter()
            val api = signedInApi(adapter, apiUrl = "https://api.example.com/".toHttpUrl())
            server.enqueue(response(401))
            val result = safeApiCall(json) { api.item() }
            assertEquals(401, (result as ApiResult.Failure).error.status)
            assertNull(server.takeRequest().headers["Authorization"])
            assertEquals(1, server.requestCount)
            assertEquals(0, adapter.refreshCount)
        }

    @Test
    fun callWithSameHostButAnotherSchemeCarriesNoBearer() =
        runTest {
            val adapter = FakeAuthAdapter()
            val httpsUrl =
                server
                    .url("/")
                    .toString()
                    .replaceFirst("http:", "https:")
                    .toHttpUrl()
            val api = signedInApi(adapter, apiUrl = httpsUrl)
            server.enqueue(response(401))
            val result = safeApiCall(json) { api.item() }
            assertEquals(401, (result as ApiResult.Failure).error.status)
            assertNull(server.takeRequest().headers["Authorization"])
            assertEquals(0, adapter.refreshCount)
        }

    @Test
    fun callWithSameHostButAnotherPortCarriesNoBearer() =
        runTest {
            val adapter = FakeAuthAdapter()
            val otherPortUrl = "http://${server.hostName}:${server.port + 1}/".toHttpUrl()
            val api = signedInApi(adapter, apiUrl = otherPortUrl)
            server.enqueue(response(401))
            val result = safeApiCall(json) { api.item() }
            assertEquals(401, (result as ApiResult.Failure).error.status)
            assertNull(server.takeRequest().headers["Authorization"])
            assertEquals(0, adapter.refreshCount)
        }

    @Test
    fun lateUnauthorizedOfPreviousSessionIsNeverRetriedWithTheNextUsersBearer() =
        runTest {
            val adapter = FakeAuthAdapter()
            val api = signedInApi(adapter)
            val release = CountDownLatch(1)
            val bearers = CopyOnWriteArrayList<String?>()
            server.dispatcher =
                object : Dispatcher() {
                    override fun dispatch(request: RecordedRequest): MockResponse {
                        bearers += request.headers["Authorization"]
                        if (bearers.size == 1) check(release.await(5, TimeUnit.SECONDS)) { "test never released the held request" }
                        return response(401)
                    }
                }
            val call = async(Dispatchers.Default) { safeApiCall(json) { api.item() } }
            withContext(Dispatchers.Default) { awaitRequests(1) }
            repository.signOut(SignOutReason.USER)
            repository.signIn(Session(AuthUser(2, "bob", "b@x.com", "Bob", "Smith"), AuthTokens("b1", "r2")))
            release.countDown()
            val result = call.await()
            assertEquals(401, (result as ApiResult.Failure).error.status)
            assertEquals(listOf<String?>("Bearer a1"), bearers.toList())
            assertEquals(0, adapter.refreshCount)
            assertEquals(AuthTokens("b1", "r2"), repository.session.value?.tokens)
        }

    @Test
    fun authenticatorIgnoresUnauthorizedFromAnotherHost() {
        val authenticator = TokenAuthenticator(Provider { error("must not refresh") }, "https://api.example.com/".toHttpUrl())
        val request =
            Request
                .Builder()
                .url("https://elsewhere.example.org/item")
                .header("Authorization", "Bearer a1")
                .build()
        val unauthorized =
            okhttp3.Response
                .Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(401)
                .message("Unauthorized")
                .build()
        assertNull(authenticator.authenticate(null, unauthorized))
    }

    @Test
    fun unauthorizedIsRefreshedAndRetriedWithNewToken() =
        runTest {
            val adapter = FakeAuthAdapter()
            val api = signedInApi(adapter)
            server.enqueue(response(401))
            server.enqueue(response(200))
            assertEquals(ApiResult.Success(Item(1)), safeApiCall(json) { api.item() })
            assertEquals("Bearer a1", server.takeRequest().headers["Authorization"])
            assertEquals("Bearer a2", server.takeRequest().headers["Authorization"])
            assertEquals(AuthTokens("a2", "r2"), repository.session.value?.tokens)
            assertEquals("r1", adapter.lastRefreshToken)
            assertEquals(1, adapter.refreshCount)
        }

    @Test
    fun concurrentUnauthorizedCallsShareOneRefresh() =
        runTest {
            val adapter = FakeAuthAdapter(onRefresh = { delay(200) })
            val api = signedInApi(adapter)
            answerOnlyRefreshedToken()
            val results = List(10) { async { safeApiCall(json) { api.item() } } }.awaitAll()
            assertTrue(results.toString(), results.all { it == ApiResult.Success(Item(1)) })
            assertEquals(1, adapter.refreshCount)
        }

    @Test
    fun refreshOverTheRefreshClientDoesNotDeadlockTheAppClient() =
        runTest(timeout = 20.seconds) {
            val refreshApi = retrofitApi(NetworkModule.refreshOkHttpClient(server.url("/"), emptySet()), RefreshApi::class.java)
            val adapter = HttpRefreshAdapter(refreshApi) { awaitRequests(DEFAULT_MAX_REQUESTS_PER_HOST) }
            val api = signedInApi(adapter)
            answerOnlyRefreshedToken()
            val results = List(8) { async { safeApiCall(json) { api.item() } } }.awaitAll()
            assertTrue(results.toString(), results.all { it == ApiResult.Success(Item(1)) })
            assertEquals(1, adapter.refreshCount)
        }

    @Test
    fun rejectedRefreshSignsOutAsExpiredWithOneAttempt() =
        runTest {
            val rejected = HttpException(Response.error<Any>(401, "".toResponseBody()))
            val adapter = FakeAuthAdapter(failure = rejected)
            val api = signedInApi(adapter)
            alwaysUnauthorized()
            val results = List(3) { async { safeApiCall(json) { api.item() } } }.awaitAll()
            results.forEach { assertEquals(401, (it as ApiResult.Failure).error.status) }
            assertNull(repository.session.value)
            assertEquals(SignOutReason.EXPIRED, repository.signOutReason.value)
            assertEquals(1, adapter.refreshCount)
        }

    // How many attempts the callers share depends on timing here; SessionRefresherTest pins it to one.
    @Test
    fun networkFailureDuringRefreshKeepsSessionAndIsNetworkError() =
        runTest {
            val adapter = FakeAuthAdapter(failure = IOException("offline"))
            val api = signedInApi(adapter)
            alwaysUnauthorized()
            val results = List(3) { async { safeApiCall(json) { api.item() } } }.awaitAll()
            assertEquals(List(3) { ApiResult.Failure(ApiError(null, null, isNetworkError = true)) }, results)
            assertEquals(AuthTokens("a1", "r1"), repository.session.value?.tokens)
            assertNull(repository.signOutReason.value)
        }

    @Test
    fun unauthorizedRetryIsNotRetriedAgain() =
        runTest {
            val api = signedInApi(FakeAuthAdapter())
            alwaysUnauthorized()
            val result = safeApiCall(json) { api.item() }
            assertEquals(401, (result as ApiResult.Failure).error.status)
            assertEquals(2, server.requestCount)
        }

    @Serializable
    private data class RefreshBody(
        val refreshToken: String,
    )

    private interface RefreshApi {
        @POST("auth/refresh")
        suspend fun refresh(
            @Body body: RefreshBody,
        ): AuthTokens
    }

    /** Refreshes over HTTP like a real adapter would, through the given API. */
    private class HttpRefreshAdapter(
        private val api: RefreshApi,
        private val beforeRefresh: suspend () -> Unit,
    ) : AuthAdapter {
        @Volatile var refreshCount = 0

        override suspend fun signIn(credentials: SignInCredentials): AuthResult = error("not used")

        override suspend fun refresh(refreshToken: String): AuthTokens {
            refreshCount++
            beforeRefresh()
            return api.refresh(RefreshBody(refreshToken))
        }
    }

    private companion object {
        /** OkHttp's default `Dispatcher.maxRequestsPerHost`. */
        const val DEFAULT_MAX_REQUESTS_PER_HOST = 5
    }
}
