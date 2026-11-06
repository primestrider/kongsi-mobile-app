package com.primestrider.kotlinstarter.core.auth

import java.util.concurrent.atomic.AtomicInteger

/** Counts refreshes; runs [onRefresh], then returns [next] or throws [failure] when set. */
class FakeAuthAdapter(
    private val next: AuthTokens = AuthTokens("a2", "r2"),
    private val failure: Throwable? = null,
    private val onRefresh: suspend () -> Unit = {},
) : AuthAdapter {
    private val count = AtomicInteger()
    val refreshCount: Int get() = count.get()

    @Volatile var lastRefreshToken: String? = null
        private set

    override suspend fun signIn(credentials: SignInCredentials): AuthResult = error("not used")

    override suspend fun refresh(refreshToken: String): AuthTokens {
        count.incrementAndGet()
        lastRefreshToken = refreshToken
        onRefresh()
        failure?.let { throw it }
        return next
    }
}
