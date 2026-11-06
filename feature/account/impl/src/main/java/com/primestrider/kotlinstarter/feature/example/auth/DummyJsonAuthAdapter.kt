package com.primestrider.kotlinstarter.feature.example.auth

import com.primestrider.kotlinstarter.core.auth.AuthAdapter
import com.primestrider.kotlinstarter.core.auth.AuthResult
import com.primestrider.kotlinstarter.core.auth.AuthTokens
import com.primestrider.kotlinstarter.core.auth.AuthUser
import com.primestrider.kotlinstarter.core.auth.SignInCredentials
import javax.inject.Inject
import javax.inject.Singleton

private const val SHORT_LIVED_MINUTES = 1

/** In-memory demo switch (example only): ask DummyJSON for 1-minute tokens to see the refresh flow. */
@Singleton
class DemoTokenSettings
    @Inject
    constructor() {
        @Volatile
        var shortLivedTokens: Boolean = false
    }

/** Maps DummyJSON's auth endpoints to [AuthAdapter]. [refreshApi] must be built on `@RefreshRetrofit`. */
class DummyJsonAuthAdapter
    @Inject
    constructor(
        private val api: DummyJsonApi,
        private val refreshApi: DummyJsonRefreshApi,
        private val settings: DemoTokenSettings,
    ) : AuthAdapter {
        private fun expiresInMins(): Int? = if (settings.shortLivedTokens) SHORT_LIVED_MINUTES else null

        override suspend fun signIn(credentials: SignInCredentials): AuthResult {
            val response = api.login(LoginRequest(credentials.username, credentials.password, expiresInMins()))
            return AuthResult(
                user =
                    AuthUser(
                        id = response.id,
                        username = response.username,
                        email = response.email,
                        firstName = response.firstName,
                        lastName = response.lastName,
                        image = response.image,
                    ),
                tokens = AuthTokens(response.accessToken, response.refreshToken),
            )
        }

        override suspend fun refresh(refreshToken: String): AuthTokens {
            val response = refreshApi.refresh(RefreshRequest(refreshToken, expiresInMins()))
            return AuthTokens(response.accessToken, response.refreshToken)
        }
    }
