package com.primestrider.kotlinstarter.feature.example.sdk.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentCodeViewModelTest {
    private val scheduler = TestCoroutineScheduler()
    private val created = mutableListOf<PaymentCodeViewModel>()

    @Before
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher(scheduler))

    @After
    fun tearDown() {
        created.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
    }

    private fun viewModel(
        handle: SavedStateHandle = SavedStateHandle(),
        seed: Int = 1,
    ) = PaymentCodeViewModel(handle, Random(seed)).also { created += it }

    private fun advance(millis: Long) {
        scheduler.advanceTimeBy(millis)
        scheduler.runCurrent()
    }

    @Test
    fun counts_down_to_one_then_refreshes_with_a_new_code() {
        val vm = viewModel(seed = 7)
        scheduler.runCurrent()
        val first = vm.state.value
        assertEquals(60, first.secondsLeft)

        advance(59_000)
        assertEquals(1, vm.state.value.secondsLeft)
        assertEquals(first.code, vm.state.value.code)

        advance(1_000)
        assertEquals(60, vm.state.value.secondsLeft)
        assertNotEquals(first.code, vm.state.value.code)
    }

    @Test
    fun amount_comes_from_the_navigation_arguments() {
        assertEquals(50_000L, viewModel(SavedStateHandle(mapOf("amount" to 50_000L))).amount)
        assertEquals(0L, viewModel(SavedStateHandle(mapOf("amount" to 0L))).amount)
        assertNull(viewModel().amount)
    }

    @Test
    fun a_negative_amount_from_outside_is_treated_as_absent() {
        assertNull(viewModel(SavedStateHandle(mapOf("amount" to -50_000L))).amount)
    }
}
