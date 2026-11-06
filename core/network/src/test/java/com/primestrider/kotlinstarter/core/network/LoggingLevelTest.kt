package com.primestrider.kotlinstarter.core.network

import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Assert.assertEquals
import org.junit.Test

class LoggingLevelTest {
    @Test
    fun debugLogsHeadersOnly() = assertEquals(HttpLoggingInterceptor.Level.HEADERS, loggingLevel(true))

    @Test
    fun releaseLogsNothing() = assertEquals(HttpLoggingInterceptor.Level.NONE, loggingLevel(false))
}
