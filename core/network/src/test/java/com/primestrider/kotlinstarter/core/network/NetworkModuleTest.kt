package com.primestrider.kotlinstarter.core.network

import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Authenticator
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.util.Optional

class NetworkModuleTest {
    private val apiUrl = "https://api.example.org/".toHttpUrl()

    @Test
    fun app_client_uses_contributed_interceptors_and_optional_authenticator() {
        val bare = NetworkModule.okHttpClient(emptySet(), Optional.empty(), apiUrl, emptySet())
        assertEquals(Authenticator.NONE, bare.authenticator)

        val tagging =
            Interceptor { chain ->
                chain.proceed(
                    chain
                        .request()
                        .newBuilder()
                        .header("X-Test", "yes")
                        .build(),
                )
            }
        val authenticator = Authenticator { _, _ -> null }
        val client = NetworkModule.okHttpClient(setOf(tagging), Optional.of(authenticator), apiUrl, emptySet())
        assertSame(authenticator, client.authenticator)

        MockWebServer().use { server ->
            server.enqueue(MockResponse(code = 200))
            server.start()
            client.newCall(Request.Builder().url(server.url("/")).build()).execute().close()
            assertEquals("yes", server.takeRequest().headers["X-Test"])
        }
    }
}
