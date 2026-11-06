package com.primestrider.kotlinstarter.feature.example.sdk.sqlite

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModelTest {
    // One dispatcher for Main, Room queries and runTest, so no arch-IO thread resumes via Main during setMain/resetMain.
    private val main = StandardTestDispatcher()
    private lateinit var database: DevicesDatabase
    private var now = 0L
    private val created = mutableListOf<TransactionsViewModel>()

    @Before
    fun setUp() {
        Dispatchers.setMain(main)
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), DevicesDatabase::class.java)
                .allowMainThreadQueries()
                .setQueryCoroutineContext(main)
                .build()
    }

    @After
    fun tearDown() {
        created.forEach { it.viewModelScope.cancel() }
        database.close()
        Dispatchers.resetMain()
    }

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) =
        TransactionsViewModel(database.transactionDao(), handle) { ++now }.also { created += it }

    @Test
    fun defaults_are_top_up_of_fifty_thousand() {
        val state = viewModel().state.value
        assertEquals(TransactionKind.TOP_UP, state.kind)
        assertEquals(50_000L, state.amount)
    }

    @Test
    fun payment_is_stored_as_a_negative_amount() =
        runTest(main) {
            val vm = viewModel()
            vm.selectKind(TransactionKind.PAYMENT)
            vm.selectAmount(50_000)
            vm.add()
            val state = vm.state.first { it.items.isNotEmpty() }
            assertEquals(-50_000L, state.items.single().amount)
            assertEquals("PAYMENT", state.items.single().kind)
            assertEquals(-50_000L, vm.state.first { it.balance != 0L }.balance)
        }

    @Test
    fun clear_removes_everything() =
        runTest(main) {
            val vm = viewModel()
            vm.add()
            vm.state.first { it.items.isNotEmpty() }
            vm.clear()
            vm.state.first { it.loaded && it.items.isEmpty() && it.balance == 0L }
        }

    @Test
    fun selection_survives_a_new_view_model_on_the_same_handle() {
        val handle = SavedStateHandle()
        viewModel(handle).apply {
            selectKind(TransactionKind.TRANSFER)
            selectAmount(100_000)
        }
        val restored = viewModel(handle).state.value
        assertEquals(TransactionKind.TRANSFER, restored.kind)
        assertEquals(100_000L, restored.amount)
    }
}
