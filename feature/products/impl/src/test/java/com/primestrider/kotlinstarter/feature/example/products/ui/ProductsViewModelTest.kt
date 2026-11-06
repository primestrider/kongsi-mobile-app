package com.primestrider.kotlinstarter.feature.example.products.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProductsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val created = mutableListOf<ViewModel>()

    private fun ProductsViewModel.tracked() = also { created += it }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() {
        created.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
    }

    @Test
    fun first_page_loads_immediately_then_typing_is_debounced() =
        runTest(dispatcher) {
            val source = FakeProductsRepository()
            val viewModel = ProductsViewModel(source, SavedStateHandle()).tracked()
            backgroundScope.launch { viewModel.products.collect {} }
            runCurrent()
            assertEquals(listOf(""), source.queries)

            viewModel.onQueryChange("p")
            advanceTimeBy(100)
            viewModel.onQueryChange("ph")
            advanceTimeBy(100)
            viewModel.onQueryChange("pho")
            advanceTimeBy(349)
            runCurrent()
            assertEquals(listOf(""), source.queries)
            advanceTimeBy(2)
            runCurrent()
            assertEquals(listOf("", "pho"), source.queries)
        }

    @Test
    fun whitespace_only_query_does_not_reload() =
        runTest(dispatcher) {
            val source = FakeProductsRepository()
            val viewModel = ProductsViewModel(source, SavedStateHandle()).tracked()
            backgroundScope.launch { viewModel.products.collect {} }
            runCurrent()
            viewModel.onQueryChange("   ")
            advanceTimeBy(1_000)
            runCurrent()
            assertEquals(listOf(""), source.queries)
            assertEquals("   ", viewModel.query.value)
        }

    @Test
    fun query_survives_recreation_from_saved_state() =
        runTest(dispatcher) {
            val handle = SavedStateHandle()
            ProductsViewModel(FakeProductsRepository(), handle).tracked().onQueryChange("laptop")
            val source = FakeProductsRepository()
            val restored = ProductsViewModel(source, handle).tracked()
            backgroundScope.launch { restored.products.collect {} }
            runCurrent()
            assertEquals("laptop", restored.query.value)
            assertEquals(listOf("laptop"), source.queries)
        }
}
