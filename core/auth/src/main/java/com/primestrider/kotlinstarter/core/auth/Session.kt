package com.primestrider.kotlinstarter.core.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthUser(
    val id: Long,
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val image: String? = null,
) {
    val fullName: String get() = "$firstName $lastName".trim()
}

@Serializable
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
)

@Serializable
data class Session(
    val user: AuthUser,
    val tokens: AuthTokens,
)

enum class SignOutReason { USER, EXPIRED }

data class SignInCredentials(
    val username: String,
    val password: String,
)
