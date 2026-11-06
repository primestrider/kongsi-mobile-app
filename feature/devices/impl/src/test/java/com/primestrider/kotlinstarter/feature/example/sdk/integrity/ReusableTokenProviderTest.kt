package com.primestrider.kotlinstarter.feature.example.sdk.integrity

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ReusableTokenProviderTest {
    @Test
    fun provider_is_prepared_once_and_reused() =
        runTest {
            var prepared = 0
            val tokens = ReusableTokenProvider(prepare = { ++prepared }, request = { provider, hash -> "p$provider:$hash" })
            assertEquals("p1:a", tokens.token("a"))
            assertEquals("p1:b", tokens.token("b"))
            assertEquals(1, prepared)
        }

    @Test
    fun failed_request_prepares_a_new_provider_and_retries_once() =
        runTest {
            var prepared = 0
            val tokens =
                ReusableTokenProvider(prepare = { ++prepared }, request = { provider, hash ->
                    if (provider == 1) error("provider invalid") else "p$provider:$hash"
                })
            assertEquals("p2:a", tokens.token("a"))
            assertEquals(2, prepared)
        }
}
