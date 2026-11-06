package com.primestrider.kotlinstarter.core.network

import javax.inject.Qualifier

/**
 * The [okhttp3.OkHttpClient] for token refresh: its own dispatcher, no bearer, no authenticator.
 * The app client's authenticator blocks its dispatcher threads while a refresh runs, so a refresh
 * sent through the app client could wait forever for a free slot.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RefreshClient

/** The [retrofit2.Retrofit] on [RefreshClient]. An `AuthAdapter` must create its refresh endpoint from it. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RefreshRetrofit
