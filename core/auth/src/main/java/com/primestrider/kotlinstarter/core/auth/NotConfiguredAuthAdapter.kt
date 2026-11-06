package com.primestrider.kotlinstarter.core.auth

/**
 * Used when no feature binds an `@AuthBackend` [AuthAdapter] (e.g. after deleting the examples): sign-in fails
 * and a stored session cannot refresh, so it ends signed out. Bind a real adapter to enable auth.
 */
object NotConfiguredAuthAdapter : AuthAdapter {
    private fun notConfigured(): Nothing =
        throw UnsupportedOperationException("Sign-in is not set up: bind an @AuthBackend AuthAdapter in a feature module")

    override suspend fun signIn(credentials: SignInCredentials): AuthResult = notConfigured()

    override suspend fun refresh(refreshToken: String): AuthTokens = notConfigured()
}
