package com.primestrider.kongsi.feature.bill.ui.accounts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kongsi.core.datastore.UserProfile
import com.primestrider.kongsi.core.datastore.UserProfileRepository
import com.primestrider.kongsi.core.designsystem.component.Messenger
import com.primestrider.kongsi.feature.bill.impl.R
import com.primestrider.kongsi.feature.bill.model.PaymentAccount
import com.primestrider.kongsi.feature.bill.model.PaymentAccountType
import com.primestrider.kongsi.feature.bill.ui.FakePaymentAccountRepository
import com.primestrider.kongsi.testing.InMemoryPreferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
@OptIn(ExperimentalCoroutinesApi::class)
class PaymentAccountEditViewModelTest {
    private val main = StandardTestDispatcher()
    private val messenger = Messenger()
    private val created = mutableListOf<PaymentAccountEditViewModel>()

    private val bca =
        PaymentAccount(1, PaymentAccountType.BANK, "BCA", "1234567890", "Ricky", isDefault = true, createdAt = 5)
    private val custom =
        PaymentAccount(2, PaymentAccountType.EWALLET, "Flip", "081234567890", "Ricky", isDefault = false, createdAt = 6)

    @Before
    fun setUp() = Dispatchers.setMain(main)

    @After
    fun tearDown() {
        created.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
    }

    private fun viewModel(
        repository: FakePaymentAccountRepository,
        handle: SavedStateHandle = SavedStateHandle(),
        profile: UserProfile = UserProfile(),
    ): PaymentAccountEditViewModel {
        val profiles = UserProfileRepository(InMemoryPreferencesDataStore())
        runBlocking { profiles.save(profile) }
        return PaymentAccountEditViewModel(repository, handle, messenger, profiles).also { created += it }
    }

    private fun PaymentAccountEditViewModel.fillValidBank() {
        onProviderChange("BRI")
        onNumberChange("0012-3456-789")
        onHolderNameChange("  Ricky Tan ")
    }

    @Test
    fun first_account_is_default_and_later_ones_are_not() =
        runTest(main) {
            val first = viewModel(FakePaymentAccountRepository())
            advanceUntilIdle()
            assertTrue(first.state.value.isDefault)
            val second = viewModel(FakePaymentAccountRepository(listOf(bca)))
            advanceUntilIdle()
            assertFalse(second.state.value.isDefault)
        }

    @Test
    fun errors_show_only_after_save_and_nothing_is_stored() =
        runTest(main) {
            val repository = FakePaymentAccountRepository()
            val viewModel = viewModel(repository)
            advanceUntilIdle()
            assertTrue(viewModel.state.value.shownErrors.isValid)
            viewModel.save()
            advanceUntilIdle()
            val errors = viewModel.state.value.shownErrors
            assertEquals(R.string.bill_account_error_provider, errors.provider)
            assertEquals(R.string.bill_account_error_bank_number, errors.number)
            assertEquals(R.string.bill_account_error_holder, errors.holderName)
            assertTrue(repository.accounts.first().isEmpty())
            assertFalse(viewModel.state.value.closed)
        }

    @Test
    fun saving_stores_normalised_values_and_closes() =
        runTest(main) {
            val repository = FakePaymentAccountRepository()
            val viewModel = viewModel(repository)
            advanceUntilIdle()
            viewModel.fillValidBank()
            viewModel.save()
            advanceUntilIdle()
            val saved = repository.accounts.first().single()
            assertEquals("BRI", saved.provider)
            assertEquals("00123456789", saved.number)
            assertEquals("Ricky Tan", saved.holderName)
            assertTrue(saved.isDefault)
            assertTrue(viewModel.state.value.closed)
        }

    @Test
    fun ewallet_number_is_saved_as_local_mobile_number() =
        runTest(main) {
            val repository = FakePaymentAccountRepository()
            val viewModel = viewModel(repository)
            advanceUntilIdle()
            viewModel.onTypeChange(PaymentAccountType.EWALLET)
            viewModel.onProviderChange("DANA")
            viewModel.onNumberChange("+62 812 3456 7890")
            viewModel.onHolderNameChange("Ricky")
            viewModel.save()
            advanceUntilIdle()
            assertEquals(
                "081234567890",
                repository.accounts
                    .first()
                    .single()
                    .number,
            )
        }

    @Test
    fun switching_type_clears_the_provider_but_keeps_the_rest() =
        runTest(main) {
            val viewModel = viewModel(FakePaymentAccountRepository())
            advanceUntilIdle()
            viewModel.fillValidBank()
            viewModel.onTypeChange(PaymentAccountType.EWALLET)
            val state = viewModel.state.value
            assertNull(state.providerChoice)
            assertEquals("0012-3456-789", state.number)
            assertEquals("  Ricky Tan ", state.holderName)
        }

    @Test
    fun other_provider_uses_the_typed_name() =
        runTest(main) {
            val repository = FakePaymentAccountRepository()
            val viewModel = viewModel(repository)
            advanceUntilIdle()
            viewModel.fillValidBank()
            viewModel.onProviderChange(OTHER_PROVIDER)
            viewModel.save()
            advanceUntilIdle()
            assertEquals(R.string.bill_account_error_provider, viewModel.state.value.shownErrors.provider)
            viewModel.onOtherProviderChange(" Bank Neo ")
            viewModel.save()
            advanceUntilIdle()
            assertEquals(
                "Bank Neo",
                repository.accounts
                    .first()
                    .single()
                    .provider,
            )
        }

    @Test
    fun editing_loads_the_account_and_keeps_id_and_place() =
        runTest(main) {
            val repository = FakePaymentAccountRepository(listOf(bca, custom))
            val viewModel = viewModel(repository, SavedStateHandle(mapOf("id" to 1L)))
            assertFalse(viewModel.state.value.loaded)
            advanceUntilIdle()
            val state = viewModel.state.value
            assertFalse(state.isNew)
            assertEquals("BCA", state.providerChoice)
            assertEquals("1234567890", state.number)
            viewModel.onNumberChange("5555555555")
            viewModel.save()
            advanceUntilIdle()
            val all = repository.accounts.first()
            assertEquals(listOf(1L, 2L), all.map { it.id })
            assertEquals("5555555555", all[0].number)
            assertEquals(5L, all[0].createdAt)
        }

    @Test
    fun editing_an_unlisted_provider_shows_it_as_other() =
        runTest(main) {
            val viewModel = viewModel(FakePaymentAccountRepository(listOf(custom)), SavedStateHandle(mapOf("id" to 2L)))
            advanceUntilIdle()
            assertEquals(OTHER_PROVIDER, viewModel.state.value.providerChoice)
            assertEquals("Flip", viewModel.state.value.otherProvider)
        }

    @Test
    fun editing_a_deleted_account_closes() =
        runTest(main) {
            val viewModel = viewModel(FakePaymentAccountRepository(), SavedStateHandle(mapOf("id" to 9L)))
            advanceUntilIdle()
            assertTrue(viewModel.state.value.closed)
        }

    @Test
    fun form_survives_process_death() =
        runTest(main) {
            val handle = SavedStateHandle()
            val repository = FakePaymentAccountRepository()
            val before = viewModel(repository, handle)
            advanceUntilIdle()
            before.fillValidBank()
            before.onDefaultChange(false)
            val after = viewModel(repository, handle)
            advanceUntilIdle()
            val state = after.state.value
            assertEquals("BRI", state.providerChoice)
            assertEquals("0012-3456-789", state.number)
            assertFalse(state.isDefault)
        }

    @Test
    fun use_my_number_fills_the_number_from_profile() =
        runTest(main) {
            val viewModel = viewModel(FakePaymentAccountRepository(), profile = UserProfile("Ricky", "081234567890"))
            advanceUntilIdle()
            assertEquals("081234567890", viewModel.state.value.myPhone)
            viewModel.onTypeChange(PaymentAccountType.EWALLET)
            viewModel.onUseMyNumber()
            assertEquals("081234567890", viewModel.state.value.number)
        }

    @Test
    fun failed_write_keeps_the_form_open() =
        runTest(main) {
            val repository = FakePaymentAccountRepository().apply { failWrites = true }
            val viewModel = viewModel(repository)
            advanceUntilIdle()
            viewModel.fillValidBank()
            viewModel.save()
            advanceUntilIdle()
            assertFalse(viewModel.state.value.closed)
            assertFalse(viewModel.state.value.saving)
        }
}
