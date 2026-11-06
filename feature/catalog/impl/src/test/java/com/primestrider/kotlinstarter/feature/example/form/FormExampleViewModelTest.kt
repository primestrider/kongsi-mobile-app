package com.primestrider.kotlinstarter.feature.example.form

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FormExampleViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun FormExampleViewModel.fillValid() {
        onChange(FormField.NAME, "Ada Lovelace")
        onChange(FormField.EMAIL, "ada@example.com")
        onChange(FormField.PHONE, "0812345678")
        onChange(FormField.PASSWORD, "analytical")
    }

    @Test
    fun errors_hidden_before_first_submit() {
        val vm = FormExampleViewModel()
        vm.onChange(FormField.EMAIL, "nope")
        assertTrue(
            vm.state.value.errors
                .isEmpty(),
        )
    }

    @Test
    fun invalid_submit_shows_errors_then_revalidates_live() {
        val vm = FormExampleViewModel()
        vm.submit()
        assertEquals(4, vm.state.value.errors.size)
        assertNull(vm.state.value.submitted)
        vm.onChange(FormField.NAME, "Ada")
        assertFalse(FormField.NAME in vm.state.value.errors)
        assertEquals(3, vm.state.value.errors.size)
    }

    @Test
    fun valid_submit_completes_after_delay() =
        runTest(dispatcher) {
            val vm = FormExampleViewModel()
            vm.fillValid()
            vm.submit()
            runCurrent()
            assertTrue(vm.state.value.submitting)
            advanceTimeBy(SUBMIT_DELAY_MS)
            runCurrent()
            assertFalse(vm.state.value.submitting)
            assertEquals(vm.state.value.values, vm.state.value.submitted)
        }

    @Test
    fun double_submit_runs_once() =
        runTest(dispatcher) {
            val vm = FormExampleViewModel()
            vm.fillValid()
            vm.submit()
            runCurrent()
            vm.onChange(FormField.NAME, "Changed")
            vm.submit()
            advanceTimeBy(SUBMIT_DELAY_MS)
            runCurrent()
            assertEquals(
                "Ada Lovelace",
                vm.state.value.submitted
                    ?.name,
            )
            assertFalse(vm.state.value.submitting)
        }

    @Test
    fun clear_during_submit_discards_result() =
        runTest(dispatcher) {
            val vm = FormExampleViewModel()
            vm.fillValid()
            vm.submit()
            runCurrent()
            vm.clear()
            advanceTimeBy(SUBMIT_DELAY_MS)
            runCurrent()
            assertEquals(FormUiState(), vm.state.value)
        }

    @Test
    fun clear_resets_everything() {
        val vm = FormExampleViewModel()
        vm.submit()
        vm.clear()
        assertEquals(FormUiState(), vm.state.value)
    }
}
