package com.primestrider.kongsi.feature.bill.ui.home

import androidx.lifecycle.viewModelScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kongsi.core.designsystem.component.MessageVariant
import com.primestrider.kongsi.core.designsystem.component.Messenger
import com.primestrider.kongsi.core.designsystem.component.UiText
import com.primestrider.kongsi.feature.bill.impl.R
import com.primestrider.kongsi.feature.bill.model.Bill
import com.primestrider.kongsi.feature.bill.model.BillCharge
import com.primestrider.kongsi.feature.bill.model.BillItem
import com.primestrider.kongsi.feature.bill.model.PaymentAccount
import com.primestrider.kongsi.feature.bill.model.PaymentAccountType
import com.primestrider.kongsi.feature.bill.model.Person
import com.primestrider.kongsi.feature.bill.ui.FakeBillRepository
import com.primestrider.kongsi.feature.bill.ui.FakePaymentAccountRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
@OptIn(ExperimentalCoroutinesApi::class)
class BillHomeViewModelTest {
    private val main = StandardTestDispatcher()
    private val messenger = Messenger()
    private val created = mutableListOf<BillHomeViewModel>()

    private val budi = Person(1, "Budi")
    private val ani = Person(2, "Ani")
    private val sushi =
        Bill(
            id = 1,
            title = "Sushi",
            createdAt = 100,
            people = listOf(budi, ani),
            items = listOf(BillItem(1, "Roll", 100_000, sharedBy = setOf(budi.id, ani.id))),
            tax = BillCharge(10_000),
        )
    private val coffee =
        Bill(
            id = 2,
            title = "Kopi",
            createdAt = 200,
            people = listOf(budi),
            items = listOf(BillItem(2, "Latte", 30_000, sharedBy = setOf(budi.id))),
        )
    private val paidLunch =
        Bill(
            id = 3,
            title = "Makan Siang",
            createdAt = 300,
            payerPersonId = budi.id,
            people = listOf(budi, ani.copy(paid = true), Person(3, "Citra")),
            items = listOf(BillItem(3, "Nasi", 60_000, sharedBy = setOf(budi.id, ani.id, 3))),
        )
    private val bca = PaymentAccount(1, PaymentAccountType.BANK, "BCA", "1234567890", "Ricky", isDefault = true)

    @Before
    fun setUp() = Dispatchers.setMain(main)

    @After
    fun tearDown() {
        created.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
    }

    @Test
    fun each_bill_says_how_many_of_those_who_owe_have_paid() =
        runTest(main) {
            val viewModel = viewModel(FakeBillRepository(listOf(paidLunch)))
            advanceUntilIdle()

            // Budi paid the bill, so only Ani and Citra owe; Ani has paid.
            val row =
                viewModel.state.value.bills
                    .single()
            assertEquals(2, row.owingCount)
            assertEquals(1, row.paidCount)
        }

    @Test
    fun renaming_saves_the_trimmed_name() =
        runTest(main) {
            val bills = FakeBillRepository(listOf(sushi))
            val viewModel = viewModel(bills)

            viewModel.rename(sushi.id, "  Sushi Jumat  ")
            advanceUntilIdle()

            assertEquals("Sushi Jumat", bills.get(sushi.id)?.title)
        }

    @Test
    fun duplicating_makes_a_copy_and_hands_back_its_id() =
        runTest(main) {
            val bills = FakeBillRepository(listOf(sushi))
            val viewModel = viewModel(bills)
            val opened = mutableListOf<Long>()

            viewModel.duplicate(sushi.id) { opened += it }
            advanceUntilIdle()

            val copy = requireNotNull(bills.get(opened.single()))
            assertEquals(listOf("Budi", "Ani"), copy.people.map { it.name })
            assertEquals(listOf("Roll"), copy.items.map { it.name })
        }

    private fun TestScope.viewModel(
        bills: FakeBillRepository,
        accounts: FakePaymentAccountRepository = FakePaymentAccountRepository(listOf(bca)),
    ): BillHomeViewModel =
        BillHomeViewModel(accounts, bills, messenger, backgroundScope).also {
            created += it
            backgroundScope.launch { it.state.collect {} }
        }

    @Test
    fun bills_are_listed_newest_first_with_their_total_and_people() =
        runTest(main) {
            val viewModel = viewModel(FakeBillRepository(listOf(sushi, coffee)))
            assertFalse(viewModel.state.value.loaded)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertTrue(state.loaded)
            assertEquals(
                listOf(
                    BillRow(id = 2, title = "Kopi", createdAt = 200, totalMinor = 30_000, peopleCount = 1, owingCount = 1),
                    BillRow(id = 1, title = "Sushi", createdAt = 100, totalMinor = 110_000, peopleCount = 2, owingCount = 2),
                ),
                state.bills,
            )
            assertFalse(state.showAccountHint)
        }

    @Test
    fun account_hint_shows_while_no_account_is_saved() =
        runTest(main) {
            val viewModel = viewModel(FakeBillRepository(), accounts = FakePaymentAccountRepository())
            advanceUntilIdle()

            assertTrue(viewModel.state.value.showAccountHint)
            assertTrue(
                viewModel.state.value.bills
                    .isEmpty(),
            )
        }

    @Test
    fun delete_removes_at_once_and_undo_puts_the_whole_bill_back() =
        runTest(main) {
            val repository = FakeBillRepository(listOf(sushi, coffee))
            val viewModel = viewModel(repository)
            advanceUntilIdle()

            viewModel.delete(sushi.id)
            advanceUntilIdle()
            assertEquals(
                listOf("Kopi"),
                viewModel.state.value.bills
                    .map { it.title },
            )
            val message = messenger.messages.first()
            assertEquals(UiText.Resource(R.string.bill_home_deleted, listOf("Sushi")), message.text)
            assertEquals(UiText.Resource(R.string.bill_undo), message.actionLabel)

            // The screen may be gone by the time Undo is tapped: the ViewModel's own scope no longer runs it.
            viewModel.viewModelScope.cancel()
            requireNotNull(message.onAction).invoke()
            advanceUntilIdle()
            assertEquals(sushi, repository.get(sushi.id))
        }

    @Test
    fun deleting_an_untitled_bill_does_not_show_a_blank_name() =
        runTest(main) {
            val viewModel = viewModel(FakeBillRepository(listOf(coffee.copy(title = ""))))
            advanceUntilIdle()

            viewModel.delete(coffee.id)
            advanceUntilIdle()

            assertEquals(UiText.Resource(R.string.bill_home_deleted_untitled), messenger.messages.first().text)
        }

    @Test
    fun failed_delete_says_so_and_keeps_the_bill() =
        runTest(main) {
            val repository = FakeBillRepository(listOf(sushi)).apply { failWrites = true }
            val viewModel = viewModel(repository)
            advanceUntilIdle()

            viewModel.delete(sushi.id)
            advanceUntilIdle()

            val message = messenger.messages.first()
            assertEquals(MessageVariant.ERROR, message.variant)
            assertEquals(UiText.Resource(R.string.bill_home_delete_failed), message.text)
            assertEquals(
                listOf("Sushi"),
                viewModel.state.value.bills
                    .map { it.title },
            )
        }
}
