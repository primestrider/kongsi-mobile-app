package com.primestrider.kotlinstarter.core.network

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import retrofit2.HttpException
import java.io.IOException

/**
 * Every HTTP failure, normalized. [message] is the server's `message` field when it sent one;
 * [data] is the whole error body when it is valid JSON (for example per-field validation errors).
 */
data class ApiError(
    val message: String?,
    val status: Int? = null,
    val isNetworkError: Boolean = false,
    val data: JsonElement? = null,
)

sealed interface ApiResult<out T> {
    data class Success<T>(
        val data: T,
    ) : ApiResult<T>

    data class Failure(
        val error: ApiError,
    ) : ApiResult<Nothing>
}

/** Runs [block] and maps every failure to [ApiResult.Failure]; coroutine cancellation is rethrown. */
suspend fun <T> safeApiCall(
    json: Json,
    block: suspend () -> T,
): ApiResult<T> =
    try {
        ApiResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        ApiResult.Failure(e.toApiError(json))
    } catch (e: IOException) {
        ApiResult.Failure(e.toApiError(json))
    } catch (e: SerializationException) {
        ApiResult.Failure(e.toApiError(json))
    }

/**
 * Maps a failure that already happened (for example a Paging `LoadResult.Error`) to [ApiError]:
 * HTTP errors keep status and body, I/O failures are network errors, anything else is generic.
 */
fun Throwable.toApiError(json: Json): ApiError =
    when (this) {
        is HttpException -> {
            val body = jsonBody(json)
            ApiError(body.serverMessage(), code(), data = body)
        }
        is IOException -> ApiError(null, isNetworkError = true)
        else -> ApiError(null)
    }

/** The error body parsed as JSON, or null when it is missing, unreadable or not valid JSON. */
private fun HttpException.jsonBody(json: Json): JsonElement? =
    try {
        val body = response()?.errorBody()?.string().orEmpty()
        json.parseToJsonElement(body).takeIf { it.isStrictJson() }
    } catch (e: SerializationException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    } catch (e: IOException) {
        null
    }

// The tree parser accepts unquoted words (a plain-text body "oops" parses), so check every value is real JSON.
private fun JsonElement.isStrictJson(): Boolean =
    when (this) {
        is JsonObject -> values.all { it.isStrictJson() }
        is JsonArray -> all { it.isStrictJson() }
        is JsonPrimitive -> isString || this is JsonNull || booleanOrNull != null || doubleOrNull != null
    }

private fun JsonElement?.serverMessage(): String? = ((this as? JsonObject)?.get("message") as? JsonPrimitive)?.contentOrNull

/** Carries an [ApiError] through APIs that only accept a [Throwable], such as Paging's `LoadResult.Error`. */
class ApiException(
    val error: ApiError,
) : Exception(error.message)

/** The [ApiError] behind a failure: the one an [ApiException] carries, else a generic error. */
fun Throwable.asApiError(): ApiError = (this as? ApiException)?.error ?: ApiError(null)
