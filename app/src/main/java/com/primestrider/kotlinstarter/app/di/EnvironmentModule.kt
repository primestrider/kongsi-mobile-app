package com.primestrider.kotlinstarter.app.di

import com.primestrider.kotlinstarter.BuildConfig
import com.primestrider.kotlinstarter.core.network.ApiBaseUrl
import com.primestrider.kotlinstarter.core.network.CertificatePins
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import javax.inject.Singleton

/** The app owns the environment: `BuildConfig.API_URL` is the base of both Retrofits and the only host that gets the bearer. */
@Module
@InstallIn(SingletonComponent::class)
object EnvironmentModule {
    @Provides
    @Singleton
    @ApiBaseUrl
    fun apiBaseUrl(): HttpUrl = BuildConfig.API_URL.toHttpUrl()

    /** `BuildConfig.API_PINS` (comma-separated `sha256/…`, from `api.pins` / `API_PINS`); empty turns pinning off. */
    @Provides
    @CertificatePins
    fun certificatePins(): Set<String> = parsePins(BuildConfig.API_PINS)
}

internal fun parsePins(value: String): Set<String> =
    value
        .split(',')
        .map(String::trim)
        .filter(String::isNotEmpty)
        .toSet()
