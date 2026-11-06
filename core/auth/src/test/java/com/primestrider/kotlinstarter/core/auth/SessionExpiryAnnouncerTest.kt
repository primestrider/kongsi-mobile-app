package com.primestrider.kotlinstarter.core.auth

import app.cash.turbine.test
import com.primestrider.kotlinstarter.core.auth.R
import com.primestrider.kotlinstarter.core.designsystem.component.MessageVariant
import com.primestrider.kotlinstarter.core.designsystem.component.Messenger
import com.primestrider.kotlinstarter.core.designsystem.component.UiText
import com.primestrider.kotlinstarter.testing.InMemoryPreferencesDataStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionExpiryAnnouncerTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val session = Session(AuthUser(1, "emilys", "e@x.com", "Emily", "Johnson"), AuthTokens("a1", "r1"))
    private val messenger = Messenger()

    private suspend fun TestScope.startedRepository(): SessionRepository {
        val repository = SessionRepository(InMemoryPreferencesDataStore(), FakeSessionCipher(), json, backgroundScope)
        SessionExpiryAnnouncer(repository, messenger, backgroundScope).start()
        runCurrent()
        repository.signIn(session)
        runCurrent()
        return repository
    }

    @Test
    fun expiredSignOutShowsOneErrorAndResetsReason() =
        runTest {
            val repository = startedRepository()
            messenger.messages.test {
                repository.signOut(SignOutReason.EXPIRED)
                runCurrent()
                val message = awaitItem()
                assertEquals(MessageVariant.ERROR, message.variant)
                assertEquals(UiText.Resource(R.string.core_auth_session_expired), message.text)
                expectNoEvents()
            }
            assertNull(repository.signOutReason.value)
        }

    @Test
    fun userSignOutShowsNothing() =
        runTest {
            val repository = startedRepository()
            messenger.messages.test {
                repository.signOut(SignOutReason.USER)
                runCurrent()
                expectNoEvents()
            }
            assertEquals(SignOutReason.USER, repository.signOutReason.value)
        }
}
