package com.primestrider.kotlinstarter.core.auth

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException

class NotConfiguredAuthAdapterTest {
    @Test
    fun not_configured_adapter_reports_not_set_up() =
        runTest {
            val signIn = runCatching { NotConfiguredAuthAdapter.signIn(SignInCredentials("u", "p")) }.exceptionOrNull()
            assertThrows(UnsupportedOperationException::class.java) { throw signIn!! }
            // Not an IOException: SessionRefresher treats any other failure as a rejected token and signs out.
            val refresh = runCatching { NotConfiguredAuthAdapter.refresh("token") }.exceptionOrNull()
            assertFalse(refresh is IOException)
            assertThrows(UnsupportedOperationException::class.java) { throw refresh!! }
        }
}
