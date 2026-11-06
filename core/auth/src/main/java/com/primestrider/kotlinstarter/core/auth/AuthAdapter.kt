package com.primestrider.kotlinstarter.core.auth

data class AuthResult(
    val user: AuthUser,
    val tokens: AuthTokens,
)

/**
 * The only backend-specific piece of auth: how to sign in and how to trade a refresh token for new
 * tokens. Bind one implementation in a feature module.
 *
 * [refresh] must use the `@RefreshRetrofit` / `@RefreshClient` (`core/network`), never the app client:
 * the app client's dispatcher threads are blocked while a refresh runs. Throw an `IOException` when
 * the server cannot be reached; that and an `HttpException` with a 5xx or 429 status keep the session.
 * Any other exception (another HTTP error, a bad body) means the refresh token was rejected and signs
 * the user out.
 */
interface AuthAdapter {
    suspend fun signIn(credentials: SignInCredentials): AuthResult

    suspend fun refresh(refreshToken: String): AuthTokens
}
