package com.primestrider.kotlinstarter.core.network

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class SafeApiCallTest {
    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var server: MockWebServer
    private lateinit var api: TestApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api =
            Retrofit
                .Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(TestApi::class.java)
    }

    @After
    fun tearDown() {
        runCatching { server.close() }
    }

    private fun respond(
        code: Int,
        body: String,
    ) = server.enqueue(
        MockResponse
            .Builder()
            .code(code)
            .body(body)
            .build(),
    )

    @Test
    fun successReturnsData() =
        runTest {
            respond(200, """{"id":1}""")
            assertEquals(ApiResult.Success(Item(1)), safeApiCall(json) { api.item() })
        }

    @Test
    fun httpErrorWithMessageBody() =
        runTest {
            respond(404, """{"message":"Not found"}""")
            assertEquals(
                ApiResult.Failure(ApiError("Not found", 404, data = json.parseToJsonElement("""{"message":"Not found"}"""))),
                safeApiCall(json) { api.item() },
            )
        }

    @Test
    fun httpErrorKeepsTheParsedJsonBody() =
        runTest {
            respond(422, """{"message":"x","fields":{"a":"b"}}""")
            val error = (safeApiCall(json) { api.item() } as ApiResult.Failure).error
            assertEquals("x", error.message)
            assertEquals(422, error.status)
            assertEquals(
                "b",
                error.data
                    ?.jsonObject
                    ?.get("fields")
                    ?.jsonObject
                    ?.get("a")
                    ?.jsonPrimitive
                    ?.content,
            )
        }

    @Test
    fun httpErrorWithNonObjectJsonBodyKeepsDataWithoutMessage() =
        runTest {
            respond(400, """["bad"]""")
            assertEquals(
                ApiResult.Failure(ApiError(null, 400, data = json.parseToJsonElement("""["bad"]"""))),
                safeApiCall(json) { api.item() },
            )
        }

    @Test
    fun httpErrorWithNonJsonBody() =
        runTest {
            respond(500, "oops")
            assertEquals(
                ApiResult.Failure(ApiError(null, 500)),
                safeApiCall(json) { api.item() },
            )
        }

    @Test
    fun unreachableServerIsNetworkError() =
        runTest {
            server.close()
            val result = safeApiCall(json) { api.item() }
            assertEquals(ApiResult.Failure(ApiError(null, null, isNetworkError = true)), result)
        }

    @Test
    fun malformedJsonIsSerializationFailure() =
        runTest {
            respond(200, "{not json")
            assertEquals(
                ApiResult.Failure(ApiError(null, null)),
                safeApiCall(json) { api.item() },
            )
        }
}
