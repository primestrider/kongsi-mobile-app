package com.primestrider.kotlinstarter.core.network

import javax.inject.Qualifier

/** The [okhttp3.HttpUrl] of `BuildConfig.API_URL`. Only requests to its host carry the bearer token. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApiBaseUrl
