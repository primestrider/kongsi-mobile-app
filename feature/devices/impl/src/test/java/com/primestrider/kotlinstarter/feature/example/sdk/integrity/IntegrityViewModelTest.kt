package com.primestrider.kotlinstarter.feature.example.sdk.integrity

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class IntegrityViewModelTest {
    private val hashes = mutableListOf<String>()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun project_zero_is_not_configured_and_requests_nothing() {
        val viewModel =
            IntegrityViewModel(0) {
                hashes += it
                "token"
            }
        assertEquals(IntegrityState.NotConfigured, viewModel.state.value)
        viewModel.request("transfer:100")
        assertEquals(IntegrityState.NotConfigured, viewModel.state.value)
        assertEquals(emptyList<String>(), hashes)
    }

    @Test
    fun token_is_shown_shortened_with_its_length() {
        val token = "eyJhbGciOiJBMjU2S1ciLCJlbmMiOiJBMjU2R0NNIn0.payload.signature"
        val viewModel =
            IntegrityViewModel(123L) {
                hashes += it
                token
            }
        assertEquals(IntegrityState.Ready, viewModel.state.value)
        viewModel.request("transfer:100")
        assertEquals(IntegrityState.Token(preview = token.take(24) + "…", length = token.length), viewModel.state.value)
        assertEquals(listOf(requestHash("transfer:100")), hashes)
    }

    @Test
    fun failure_keeps_the_message() {
        val viewModel = IntegrityViewModel(123L) { error("Play Store not available") }
        viewModel.request("transfer:100")
        assertEquals(IntegrityState.Failed("Play Store not available"), viewModel.state.value)
    }

    @Test
    fun request_hash_is_the_sha256_of_the_action() {
        assertEquals("47DEQpj8HBSa-_TImW-5JCeuQeRkm5NMpJWZG3hSuFU", requestHash(""))
    }
}
