package com.primestrider.kotlinstarter.core.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class ToApiErrorTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun http(
        code: Int,
        body: String,
    ) = HttpException(Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())))

    @Test
    fun http_error_keeps_status_message_and_body() {
        val error = http(404, """{"message":"x"}""").toApiError(json)
        assertEquals("x", error.message)
        assertEquals(404, error.status)
        assertEquals(false, error.isNetworkError)
        assertEquals(
            "x",
            error.data!!
                .jsonObject["message"]!!
                .jsonPrimitive.content,
        )
    }

    @Test
    fun io_failure_is_a_network_error() {
        assertEquals(ApiError(null, isNetworkError = true), IOException("down").toApiError(json))
    }

    @Test
    fun anything_else_is_generic() {
        assertEquals(ApiError(null), IllegalStateException("bug").toApiError(json))
    }
}
