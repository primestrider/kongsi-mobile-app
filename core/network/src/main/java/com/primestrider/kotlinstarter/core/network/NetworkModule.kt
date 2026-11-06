package com.primestrider.kotlinstarter.core.network

import com.primestrider.kotlinstarter.core.network.BuildConfig
import dagger.BindsOptionalOf
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.CertificatePinner
import okhttp3.Dispatcher
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.Optional
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/** Interceptors every app (non-refresh) request goes through, contributed with `@IntoSet @AppInterceptors`. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppInterceptors

/** Declares the extension points of the app client, so it builds even when nobody contributes to them. */
@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkBindings {
    @Multibinds
    @AppInterceptors
    abstract fun appInterceptors(): Set<Interceptor>

    @BindsOptionalOf
    abstract fun authenticator(): Authenticator
}

/** The `@ApiBaseUrl` itself is provided by `:app`, which owns the per-flavour environment. */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val TIMEOUT_SECONDS = 30L
    private const val CALL_TIMEOUT_SECONDS = 60L

    @Provides
    @Singleton
    fun json(): Json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    /**
     * The app client. It knows nothing about auth: `core:auth` contributes its bearer interceptor to
     * [AppInterceptors] and binds its 401 refresh as the [Authenticator]; without `core:auth` the client is plain.
     */
    @Provides
    @Singleton
    fun okHttpClient(
        @AppInterceptors interceptors: Set<@JvmSuppressWildcards Interceptor>,
        authenticator: Optional<Authenticator>,
        @ApiBaseUrl baseUrl: HttpUrl,
        @CertificatePins pins: Set<@JvmSuppressWildcards String>,
    ): OkHttpClient =
        baseClient(baseUrl, pins)
            .apply { interceptors.forEach(::addInterceptor) }
            .apply { authenticator.ifPresent(::authenticator) }
            .build()

    /** See [RefreshClient]: a separate dispatcher, so a refresh never waits behind blocked app calls. */
    @Provides
    @Singleton
    @RefreshClient
    fun refreshOkHttpClient(
        @ApiBaseUrl baseUrl: HttpUrl,
        @CertificatePins pins: Set<@JvmSuppressWildcards String>,
    ): OkHttpClient = baseClient(baseUrl, pins).dispatcher(Dispatcher()).build()

    @Provides
    @Singleton
    fun retrofit(
        client: OkHttpClient,
        json: Json,
        @ApiBaseUrl baseUrl: HttpUrl,
    ): Retrofit = retrofitOn(client, json, baseUrl)

    @Provides
    @Singleton
    @RefreshRetrofit
    fun refreshRetrofit(
        @RefreshClient client: OkHttpClient,
        json: Json,
        @ApiBaseUrl baseUrl: HttpUrl,
    ): Retrofit = retrofitOn(client, json, baseUrl)

    /** Both clients talk to the API host, so both get its pins (when there are any). */
    private fun baseClient(
        baseUrl: HttpUrl,
        pins: Set<String>,
    ): OkHttpClient.Builder =
        OkHttpClient
            .Builder()
            .apply {
                if (pins.isNotEmpty()) certificatePinner(CertificatePinner.Builder().add(baseUrl.host, *pins.toTypedArray()).build())
            }.connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            // A network interceptor sees the final headers, including authenticator retries.
            .addNetworkInterceptor(
                HttpLoggingInterceptor().apply {
                    level = loggingLevel(BuildConfig.DEBUG)
                    redactHeader("Authorization")
                    redactHeader("Cookie")
                    redactHeader("Set-Cookie")
                },
            )

    private fun retrofitOn(
        client: OkHttpClient,
        json: Json,
        baseUrl: HttpUrl,
    ): Retrofit =
        Retrofit
            .Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
}

/** Headers only in debug: bodies carry passwords and tokens, so they are never logged. */
internal fun loggingLevel(debug: Boolean): HttpLoggingInterceptor.Level =
    if (debug) HttpLoggingInterceptor.Level.HEADERS else HttpLoggingInterceptor.Level.NONE
