package com.primestrider.kotlinstarter.core.network

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class AnonymousTest {
    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var server: MockWebServer
    private lateinit var api: TestApi
    private var captured: Request? = null

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val client =
            OkHttpClient
                .Builder()
                .addInterceptor(
                    Interceptor { chain ->
                        captured = chain.request()
                        chain.proceed(chain.request())
                    },
                ).build()
        api =
            Retrofit
                .Builder()
                .baseUrl(server.url("/"))
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(TestApi::class.java)
        repeat(2) { server.enqueue(MockResponse.Builder().body("""{"id":1}""").build()) }
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun anonymousMethodIsDetected() =
        runTest {
            api.open()
            assertTrue(captured!!.isAnonymous())
        }

    @Test
    fun regularMethodIsNotAnonymous() =
        runTest {
            api.item()
            assertFalse(captured!!.isAnonymous())
        }
}
