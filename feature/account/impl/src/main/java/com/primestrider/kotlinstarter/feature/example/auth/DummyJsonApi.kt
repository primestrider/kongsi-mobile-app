package com.primestrider.kotlinstarter.feature.example.auth

import com.primestrider.kotlinstarter.core.network.Anonymous
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/** DummyJSON auth endpoints on the app client. Refresh lives on [DummyJsonRefreshApi]. */
interface DummyJsonApi {
    @Anonymous
    @POST("auth/login")
    suspend fun login(
        @Body body: LoginRequest,
    ): LoginResponse

    @GET("auth/me")
    suspend fun me(): UserResponse
}

/** Token refresh, created from `@RefreshRetrofit` so it never queues behind the app client's blocked threads. */
interface DummyJsonRefreshApi {
    @Anonymous
    @POST("auth/refresh")
    suspend fun refresh(
        @Body body: RefreshRequest,
    ): TokenResponse
}

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
    val expiresInMins: Int? = null,
)

@Serializable
data class RefreshRequest(
    val refreshToken: String,
    val expiresInMins: Int? = null,
)

@Serializable
data class LoginResponse(
    val id: Long,
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val image: String? = null,
    val accessToken: String,
    val refreshToken: String,
)

@Serializable
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
)

@Serializable
data class UserResponse(
    val id: Long,
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val image: String? = null,
)
