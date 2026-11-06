package com.primestrider.kotlinstarter.feature.example.auth

import com.primestrider.kotlinstarter.core.auth.AuthTokens
import com.primestrider.kotlinstarter.core.auth.SignInCredentials
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.atomic.AtomicInteger

class DummyJsonAuthAdapterTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val appCalls = AtomicInteger()
    private val refreshCalls = AtomicInteger()
    private lateinit var server: MockWebServer
    private lateinit var settings: DemoTokenSettings
    private lateinit var adapter: DummyJsonAuthAdapter

    private fun retrofit(counter: AtomicInteger): Retrofit {
        val client =
            OkHttpClient
                .Builder()
                .addInterceptor { chain ->
                    counter.incrementAndGet()
                    chain.proceed(chain.request())
                }.build()
        return Retrofit
            .Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        settings = DemoTokenSettings()
        adapter =
            DummyJsonAuthAdapter(
                retrofit(appCalls).create(DummyJsonApi::class.java),
                retrofit(refreshCalls).create(DummyJsonRefreshApi::class.java),
                settings,
            )
    }

    @After
    fun tearDown() = server.close()

    private val loginJson =
        """{"id":1,"username":"emilys","email":"e@x.com","firstName":"Emily","lastName":"Johnson",""" +
            """"gender":"female","image":"https://img/e.png","accessToken":"a1","refreshToken":"r1"}"""

    @Test
    fun signIn_posts_credentials_anonymously_and_maps_fields() =
        runTest {
            server.enqueue(MockResponse.Builder().body(loginJson).build())
            val result = adapter.signIn(SignInCredentials("emilys", "emilyspass"))

            val request = server.takeRequest()
            assertEquals("/auth/login", request.url.encodedPath)
            assertEquals("POST", request.method)
            assertNull(request.headers["Authorization"])
            assertEquals("""{"username":"emilys","password":"emilyspass"}""", request.body!!.utf8())
            assertEquals(1L, result.user.id)
            assertEquals("emilys", result.user.username)
            assertEquals("e@x.com", result.user.email)
            assertEquals("Emily", result.user.firstName)
            assertEquals("Johnson", result.user.lastName)
            assertEquals("https://img/e.png", result.user.image)
            assertEquals(AuthTokens("a1", "r1"), result.tokens)
        }

    @Test
    fun signIn_requests_one_minute_tokens_when_enabled() =
        runTest {
            settings.shortLivedTokens = true
            server.enqueue(MockResponse.Builder().body(loginJson).build())
            adapter.signIn(SignInCredentials("emilys", "emilyspass"))
            assertTrue(
                server
                    .takeRequest()
                    .body!!
                    .utf8()
                    .contains(""""expiresInMins":1"""),
            )
        }

    @Test
    fun refresh_goes_through_the_refresh_client_only() =
        runTest {
            server.enqueue(MockResponse.Builder().body("""{"accessToken":"a2","refreshToken":"r2"}""").build())
            val tokens = adapter.refresh("r1")

            val request = server.takeRequest()
            assertEquals("/auth/refresh", request.url.encodedPath)
            assertEquals("POST", request.method)
            assertNull(request.headers["Authorization"])
            assertEquals("""{"refreshToken":"r1"}""", request.body!!.utf8())
            assertEquals(AuthTokens("a2", "r2"), tokens)
            assertEquals(1, refreshCalls.get())
            assertEquals(0, appCalls.get())
        }

    @Test
    fun refresh_requests_one_minute_tokens_when_enabled() =
        runTest {
            settings.shortLivedTokens = true
            server.enqueue(MockResponse.Builder().body("""{"accessToken":"a2","refreshToken":"r2"}""").build())
            adapter.refresh("r1")
            assertTrue(
                server
                    .takeRequest()
                    .body!!
                    .utf8()
                    .contains(""""expiresInMins":1"""),
            )
        }
}
