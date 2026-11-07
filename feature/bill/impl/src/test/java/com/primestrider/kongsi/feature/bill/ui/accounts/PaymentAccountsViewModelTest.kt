package com.primestrider.kongsi.feature.bill.ui.accounts

import androidx.lifecycle.viewModelScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kongsi.core.designsystem.component.MessageVariant
import com.primestrider.kongsi.core.designsystem.component.Messenger
import com.primestrider.kongsi.core.designsystem.component.UiText
import com.primestrider.kongsi.feature.bill.impl.R
import com.primestrider.kongsi.feature.bill.model.PaymentAccount
import com.primestrider.kongsi.feature.bill.model.PaymentAccountType
import com.primestrider.kongsi.feature.bill.ui.FakePaymentAccountRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
@OptIn(ExperimentalCoroutinesApi::class)
class PaymentAccountsViewModelTest {
    private val main = StandardTestDispatcher()
    private val messenger = Messenger()
    private val created = mutableListOf<PaymentAccountsViewModel>()

    private val bca = PaymentAccount(1, PaymentAccountType.BANK, "BCA", "1234567890", "Ricky", isDefault = true, createdAt = 1)
    private val ovo = PaymentAccount(2, PaymentAccountType.EWALLET, "OVO", "081234567890", "Ricky", isDefault = false, createdAt = 2)

    @Before
    fun setUp() = Dispatchers.setMain(main)

    @After
    fun tearDown() {
        created.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
    }

    @Test
    fun delete_removes_at_once_and_undo_puts_it_back_in_place() =
        runTest(main) {
            val repository = FakePaymentAccountRepository(listOf(bca, ovo))
            val viewModel = PaymentAccountsViewModel(repository, messenger, backgroundScope).also { created += it }
            backgroundScope.launch { viewModel.state.collect {} }
            advanceUntilIdle()
            assertTrue(viewModel.state.value.loaded)

            viewModel.delete(1)
            advanceUntilIdle()
            assertEquals(
                listOf("OVO"),
                viewModel.state.value.accounts
                    .map { it.provider },
            )
            val message = messenger.messages.first()
            assertEquals(UiText.Resource(R.string.bill_account_deleted, listOf("BCA")), message.text)

            // The screen may be gone by the time Undo is tapped: the ViewModel's own scope no longer runs it.
            viewModel.viewModelScope.cancel()
            requireNotNull(message.onAction).invoke()
            advanceUntilIdle()
            assertEquals(listOf("BCA", "OVO"), repository.accounts.value.map { it.provider })
        }

    @Test
    fun failed_delete_reports_an_error_and_keeps_the_account() =
        runTest(main) {
            val repository = FakePaymentAccountRepository(listOf(bca)).apply { failWrites = true }
            val viewModel = PaymentAccountsViewModel(repository, messenger, backgroundScope).also { created += it }
            viewModel.delete(1)
            advanceUntilIdle()
            assertEquals(MessageVariant.ERROR, messenger.messages.first().variant)
            assertEquals(listOf("BCA"), repository.accounts.value.map { it.provider })
        }
}
