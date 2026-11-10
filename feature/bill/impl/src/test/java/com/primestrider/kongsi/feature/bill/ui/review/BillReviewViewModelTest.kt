package com.primestrider.kongsi.feature.bill.ui.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kongsi.core.designsystem.component.Messenger
import com.primestrider.kongsi.core.designsystem.component.UiText
import com.primestrider.kongsi.feature.bill.impl.R
import com.primestrider.kongsi.feature.bill.model.Bill
import com.primestrider.kongsi.feature.bill.model.BillCharge
import com.primestrider.kongsi.feature.bill.model.BillItem
import com.primestrider.kongsi.feature.bill.model.ChargeType
import com.primestrider.kongsi.feature.bill.model.SplitIssue
import com.primestrider.kongsi.feature.bill.ui.FakeBillRepository
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
@OptIn(ExperimentalCoroutinesApi::class)
class BillReviewViewModelTest {
    private val main = StandardTestDispatcher()
    private val messenger = Messenger()
    private val created = mutableListOf<BillReviewViewModel>()

    private val dinner =
        Bill(
            id = 5,
            title = "Makan Malam",
            createdAt = 1,
            people = emptyList(),
            items =
                listOf(
                    BillItem(10, "Nasi Goreng", 30_000, sharedBy = emptySet()),
                    BillItem(11, "Steak", 90_000, sharedBy = emptySet()),
                    BillItem(12, "Ocha", 12_000, sharedBy = emptySet()),
                ),
            service = BillCharge(6_600, rateBps = 500),
            tax = BillCharge(13_860, rateBps = 1000),
            receiptTotalMinor = 152_460,
        )

    @Before
    fun setUp() = Dispatchers.setMain(main)

    @After
    fun tearDown() {
        created.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(
        repository: FakeBillRepository,
        billId: Long = 0,
        handle: SavedStateHandle = SavedStateHandle(mapOf("billId" to billId)),
    ): BillReviewViewModel =
        BillReviewViewModel(handle, repository, messenger, backgroundScope).also {
            created += it
            backgroundScope.launch { it.state.collect {} }
        }

    private fun BillReviewViewModel.addItem(
        name: String,
        price: String,
        quantity: String = "1",
    ) {
        startNewItem()
        updateEditor { it.copy(name = name, price = price, quantity = quantity) }
        saveItem()
    }

    @Test
    fun new_bill_is_not_saved_until_it_has_something_in_it() =
        runTest(main) {
            val repository = FakeBillRepository()
            val handle = SavedStateHandle(mapOf("billId" to 0L))
            val viewModel = viewModel(repository, handle = handle)
            advanceUntilIdle()
            assertTrue(viewModel.state.value.loaded)
            assertTrue(repository.saves.isEmpty())

            viewModel.addItem("Nasi Goreng", "30.000")
            advanceUntilIdle()

            val saved = repository.bills.value.single()
            assertEquals(listOf("Nasi Goreng"), saved.items.map { it.name })
            assertEquals(30_000L, saved.items.single().totalMinor)
            // Process death comes back to the same draft.
            assertEquals(saved.id, handle.get<Long>("billId"))
            assertEquals(
                listOf("Nasi Goreng"),
                viewModel.state.value.items
                    .map { it.name },
            )
            assertNull(viewModel.state.value.editor)
        }

    @Test
    fun item_needs_a_name_and_a_price() =
        runTest(main) {
            val repository = FakeBillRepository()
            val viewModel = viewModel(repository)
            advanceUntilIdle()

            viewModel.addItem(name = " ", price = "0")
            advanceUntilIdle()

            val editor = requireNotNull(viewModel.state.value.editor)
            assertTrue(editor.nameError)
            assertTrue(editor.priceError)
            assertTrue(repository.saves.isEmpty())

            viewModel.updateEditor { it.copy(name = "Es Teh", price = "45rb") }
            viewModel.saveItem()
            advanceUntilIdle()
            assertEquals(
                45_000L,
                repository.bills.value
                    .single()
                    .items
                    .single()
                    .totalMinor,
            )
        }

    @Test
    fun editing_an_item_keeps_its_place() =
        runTest(main) {
            val repository = FakeBillRepository(listOf(dinner))
            val viewModel = viewModel(repository, billId = dinner.id)
            advanceUntilIdle()

            viewModel.editItem(11)
            advanceUntilIdle()
            assertEquals(
                "Steak",
                viewModel.state.value.editor
                    ?.name,
            )
            assertEquals(
                "90000",
                viewModel.state.value.editor
                    ?.price,
            )
            viewModel.updateEditor { it.copy(name = "Wagyu") }
            viewModel.saveItem()
            advanceUntilIdle()

            assertEquals(listOf("Nasi Goreng", "Wagyu", "Ocha"), repository.get(dinner.id)?.items?.map { it.name })
        }

    @Test
    fun turning_tax_off_on_an_item_warns_about_the_rate_and_points_at_that_item() =
        runTest(main) {
            val repository = FakeBillRepository(listOf(dinner))
            val viewModel = viewModel(repository, billId = dinner.id)
            advanceUntilIdle()
            assertTrue(
                viewModel.state.value.warnings
                    .isEmpty(),
            )

            viewModel.editItem(12)
            viewModel.updateEditor { it.copy(taxable = false) }
            viewModel.saveItem()
            advanceUntilIdle()

            val warnings = viewModel.state.value.warnings
            assertEquals(
                listOf(SplitIssue.RateMismatch(ChargeType.TAX, printedMinor = 13_860, calculatedMinor = 12_600)),
                warnings.map { it.issue },
            )
            viewModel.onWarningClick(warnings.single())
            advanceUntilIdle()
            assertEquals(ReviewFocus.Item(12), viewModel.state.value.focus)
            viewModel.onFocusShown()
            advanceUntilIdle()
            assertNull(viewModel.state.value.focus)
        }

    @Test
    fun tax_with_no_taxable_item_blocks_the_next_step() =
        runTest(main) {
            val allTaxFree = dinner.copy(items = dinner.items.map { it.copy(taxable = false) }, tax = BillCharge(13_860))
            val viewModel = viewModel(FakeBillRepository(listOf(allTaxFree)), billId = dinner.id)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(listOf(SplitIssue.NoEligibleItem(ChargeType.TAX)), state.warnings.map { it.issue })
            assertTrue(state.warnings.single().blocking)
            assertFalse(state.canContinue)
        }

    @Test
    fun receipt_total_that_does_not_match_warns_and_points_at_the_receipt_total() =
        runTest(main) {
            val viewModel = viewModel(FakeBillRepository(listOf(dinner)), billId = dinner.id)
            advanceUntilIdle()

            viewModel.onChargeChange(ChargeField.RECEIPT_TOTAL, "150.000")
            advanceUntilIdle()

            val warning =
                viewModel.state.value.warnings
                    .single()
            assertEquals(SplitIssue.ReceiptTotalMismatch(receiptMinor = 150_000, calculatedMinor = 152_460), warning.issue)
            assertEquals(ReviewFocus.Charge(ChargeField.RECEIPT_TOTAL), warning.focus)
            assertTrue(viewModel.state.value.canContinue)
        }

    @Test
    fun percent_fills_the_charge_and_typing_an_amount_drops_the_rate() =
        runTest(main) {
            val repository = FakeBillRepository(listOf(dinner.copy(service = BillCharge(), tax = BillCharge())))
            val viewModel = viewModel(repository, billId = dinner.id)
            advanceUntilIdle()

            viewModel.onRateChange(ChargeField.SERVICE, "5")
            advanceUntilIdle()
            assertEquals(BillCharge(6_600, rateBps = 500), repository.get(dinner.id)?.service)
            assertEquals("6600", viewModel.charges.getValue(ChargeField.SERVICE).amount)

            viewModel.onRateChange(ChargeField.TAX, "10")
            advanceUntilIdle()
            assertEquals(BillCharge(13_860, rateBps = 1000), repository.get(dinner.id)?.tax)

            viewModel.onChargeChange(ChargeField.TAX, "14.000")
            advanceUntilIdle()
            assertEquals(BillCharge(14_000), repository.get(dinner.id)?.tax)
            assertEquals("", viewModel.charges.getValue(ChargeField.TAX).rate)
        }

    @Test
    fun deleting_an_item_offers_undo_that_puts_it_back_in_place() =
        runTest(main) {
            val repository = FakeBillRepository(listOf(dinner))
            val viewModel = viewModel(repository, billId = dinner.id)
            advanceUntilIdle()

            viewModel.deleteItem(11)
            advanceUntilIdle()
            assertEquals(
                listOf("Nasi Goreng", "Ocha"),
                viewModel.state.value.items
                    .map { it.name },
            )
            val message = messenger.messages.first()
            assertEquals(UiText.Resource(R.string.bill_item_deleted, listOf("Steak")), message.text)

            viewModel.viewModelScope.cancel()
            requireNotNull(message.onAction).invoke()
            advanceUntilIdle()
            assertEquals(listOf("Nasi Goreng", "Steak", "Ocha"), repository.get(dinner.id)?.items?.map { it.name })
            assertEquals(
                11L,
                repository
                    .get(dinner.id)
                    ?.items
                    ?.get(1)
                    ?.id,
            )
        }

    @Test
    fun typing_a_rate_shows_it_at_once_before_the_amount_is_worked_out() =
        runTest(main) {
            val viewModel = viewModel(FakeBillRepository(listOf(dinner.copy(tax = BillCharge()))), billId = dinner.id)
            advanceUntilIdle()

            viewModel.onRateChange(ChargeField.TAX, "1")
            assertEquals("1", viewModel.charges.getValue(ChargeField.TAX).rate)
            viewModel.onRateChange(ChargeField.TAX, "10")
            assertEquals("10", viewModel.charges.getValue(ChargeField.TAX).rate)
            viewModel.onTitleChange("Sushi")
            assertEquals("Sushi", viewModel.title)
        }

    @Test
    fun an_empty_bill_cannot_continue() =
        runTest(main) {
            val viewModel = viewModel(FakeBillRepository())
            advanceUntilIdle()

            assertFalse(viewModel.state.value.canContinue)
        }

    @Test
    fun title_and_charges_of_a_saved_bill_are_shown_as_typed() =
        runTest(main) {
            val viewModel = viewModel(FakeBillRepository(listOf(dinner)), billId = dinner.id)
            advanceUntilIdle()

            // Text inputs are Compose state, read synchronously by the fields (a flow would make the cursor jump).
            assertEquals("Makan Malam", viewModel.title)
            assertEquals(ChargeInput(amount = "6600", rate = "5"), viewModel.charges.getValue(ChargeField.SERVICE))
            assertEquals("152460", viewModel.charges.getValue(ChargeField.RECEIPT_TOTAL).amount)
            assertEquals(152_460L, viewModel.state.value.totalMinor)
        }
}
