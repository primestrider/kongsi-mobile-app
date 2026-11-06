package com.primestrider.kotlinstarter.core.designsystem.component

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MessengerTest {
    @Test
    fun message_sent_before_collection_is_delivered() =
        runTest {
            val messenger = Messenger()
            messenger.info(UiText.Plain("Early"))
            messenger.messages.test {
                assertEquals(UiText.Plain("Early"), awaitItem().text)
                expectNoEvents()
            }
        }

    @Test
    fun each_message_is_delivered_once() =
        runTest {
            val messenger = Messenger()
            messenger.messages.test {
                messenger.error(UiText.Plain("Boom"))
                assertEquals(MessageVariant.ERROR, awaitItem().variant)
                expectNoEvents()
            }
        }

    @Test
    fun shorthands_set_the_variant() =
        runTest {
            val messenger = Messenger()
            messenger.success(UiText.Plain("a"))
            messenger.warning(UiText.Plain("b"))
            messenger.messages.test {
                assertEquals(MessageVariant.SUCCESS, awaitItem().variant)
                assertEquals(MessageVariant.WARNING, awaitItem().variant)
            }
        }
}
