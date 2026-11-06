package com.primestrider.kotlinstarter.core.auth

import com.primestrider.kotlinstarter.core.network.ApiBaseUrl
import com.primestrider.kotlinstarter.core.network.isAnonymous
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import javax.inject.Inject

/**
 * Adds `Authorization: Bearer <accessToken>` to every request to the API origin ([apiUrl]: scheme, host, port) except
 * `@Anonymous` ones. Requests to any other origin (an image CDN, a third-party API) never get the token,
 * nor does the API host over another scheme or port.
 */
class AuthInterceptor
    @Inject
    constructor(
        private val repository: SessionRepository,
        @ApiBaseUrl private val apiUrl: HttpUrl,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val token = repository.accessToken()
            if (token == null || !request.isForApi(apiUrl) || request.isAnonymous()) return chain.proceed(request)
            return chain.proceed(request.newBuilder().header(AUTHORIZATION, bearer(token)).build())
        }
    }

/** True when this request goes to the origin (scheme, host and port) of [apiUrl], the only one the bearer is meant for. */
internal fun Request.isForApi(apiUrl: HttpUrl): Boolean = url.scheme == apiUrl.scheme && url.host == apiUrl.host && url.port == apiUrl.port

internal const val AUTHORIZATION = "Authorization"
private const val BEARER_PREFIX = "Bearer "

internal fun bearer(token: String): String = BEARER_PREFIX + token

/** The token of a `Bearer` authorization header, or null when there is none. */
internal fun bearerToken(header: String?): String? = header?.takeIf { it.startsWith(BEARER_PREFIX) }?.removePrefix(BEARER_PREFIX)
