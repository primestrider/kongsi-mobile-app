package com.primestrider.kongsi.feature.bill.ui.summary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kongsi.feature.bill.model.Bill
import com.primestrider.kongsi.feature.bill.model.BillCharge
import com.primestrider.kongsi.feature.bill.model.BillItem
import com.primestrider.kongsi.feature.bill.model.Person
import com.primestrider.kongsi.feature.bill.model.SplitCalculator
import com.primestrider.kongsi.feature.bill.ui.FakeBillRepository
import com.primestrider.kongsi.feature.bill.ui.FakeFrequentPeopleRepository
import com.primestrider.kongsi.feature.bill.ui.people.rowsOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
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
class BillSummaryViewModelTest {
    private val main = StandardTestDispatcher()
    private val created = mutableListOf<BillSummaryViewModel>()

    private val me = 1L
    private val budi = 2L
    private val ani = 3L

    // The plan's worked example: service 5%, then PB1 10% on subtotal + service, Rp152.460 in all. The payer had nothing.
    private val example =
        Bill(
            id = 7,
            title = "Dinner",
            createdAt = 1,
            payerPersonId = me,
            people = listOf(Person(me, "Ricky"), Person(budi, "Budi", "6281234567890"), Person(ani, "Ani")),
            items =
                listOf(
                    BillItem(10, "Nasi Goreng", 30_000, sharedBy = setOf(budi)),
                    BillItem(11, "Steak", 90_000, sharedBy = setOf(ani)),
                    BillItem(12, "Ocha", 12_000, sharedBy = setOf(budi, ani)),
                ),
            service = BillCharge(6_600, 500),
            tax = BillCharge(13_860, 1_000),
        )

    private val frequent = FakeFrequentPeopleRepository()

    @Before
    fun setUp() = Dispatchers.setMain(main)

    @After
    fun tearDown() {
        created.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(bill: Bill = example): BillSummaryViewModel =
        BillSummaryViewModel(SavedStateHandle(mapOf("billId" to bill.id)), FakeBillRepository(listOf(bill)), frequent, this).also {
            created += it
            backgroundScope.launch { it.state.collect {} }
        }

    private fun BillSummaryUiState.person(id: Long) = people.single { it.row.person.id == id }

    @Test
    fun worked_example_breaks_down_into_items_service_and_tax() =
        runTest(main) {
            val viewModel = viewModel()
            advanceUntilIdle()

            val state = viewModel.state.value
            assertTrue(state.complete)
            assertEquals(152_460, state.totalMinor)
            assertEquals(listOf(me, budi, ani), state.people.map { it.row.person.id })

            val budiShare = state.person(budi)
            assertEquals(41_580, budiShare.totalMinor)
            val lines = budiShare.lines
            assertEquals(BreakdownLine.Item("Nasi Goreng", 1, emptyList(), 1, 30_000), lines[0])
            val ocha = lines[1] as BreakdownLine.Item
            assertEquals(listOf("Ani"), ocha.sharedWith.map { it.label })
            assertEquals(2, ocha.sharers)
            assertEquals(6_000, ocha.amountMinor)
            assertEquals(BreakdownLine.Service(500, 1_800), lines[2])
            assertEquals(BreakdownLine.Tax(1_000, onService = true, 3_780), lines[3])
            assertEquals(4, lines.size)
            assertEquals(110_880, state.person(ani).totalMinor)
        }

    @Test
    fun the_payer_is_first_and_owes_nothing_when_they_had_nothing() =
        runTest(main) {
            val viewModel = viewModel()
            advanceUntilIdle()

            val payer =
                viewModel.state.value.people
                    .first()
            assertTrue(payer.row.isMe)
            assertEquals(0, payer.totalMinor)
            assertTrue(payer.lines.isEmpty())
        }

    @Test
    fun rounding_is_a_line_only_when_above_one_rupiah() {
        val people = listOf(Person(budi, "Budi"), Person(ani, "Ani"))

        // Each part is Rp5.000,5 and shows as Rp5.001: three such items miss Budi's total by Rp1, five by Rp2.
        fun linesFor(count: Long): List<BreakdownLine> {
            val bill = Bill(people = people, items = (1L..count).map { BillItem(it, "Item $it", 10_001, setOf(budi, ani)) })
            return breakdownOf(bill, SplitCalculator.split(bill).shares.first(), rowsOf(bill))
        }

        assertTrue(linesFor(3).none { it is BreakdownLine.Rounding })
        assertEquals(BreakdownLine.Rounding(-2), linesFor(5).last())
    }

    @Test
    fun discount_and_charges_without_a_rate_are_their_own_lines() =
        runTest(main) {
            val bill =
                example.copy(
                    service = BillCharge(),
                    // Budi's items are Rp36.000 of Rp132.000, so his part of each is 36/132.
                    tax = BillCharge(13_200),
                    discountMinor = 6_600,
                )
            val viewModel = viewModel(bill)
            advanceUntilIdle()

            val lines =
                viewModel.state.value
                    .person(budi)
                    .lines
            assertEquals(BreakdownLine.Tax(null, onService = false, 3_600), lines[2])
            assertEquals(BreakdownLine.Discount(1_800), lines[3])
        }

    @Test
    fun the_breakdown_sheet_opens_for_one_person_and_closes() =
        runTest(main) {
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onOpenBreakdown(budi)
            advanceUntilIdle()
            assertEquals(budi, viewModel.state.value.openPersonId)

            viewModel.onCloseBreakdown()
            advanceUntilIdle()
            assertNull(viewModel.state.value.openPersonId)
        }

    @Test
    fun an_unassigned_item_is_not_complete() =
        runTest(main) {
            val bill = example.copy(items = example.items + BillItem(13, "Es Teh", 8_000, emptySet()))
            val viewModel = viewModel(bill)
            advanceUntilIdle()

            assertFalse(viewModel.state.value.complete)
        }

    @Test
    fun finishing_remembers_everyone_but_the_payer() =
        runTest(main) {
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.finish()
            advanceUntilIdle()

            assertEquals(listOf(listOf("Budi", "Ani")), frequent.remembered.map { people -> people.map { it.name } })
        }

    @Test
    fun shared_item_fractions_read_as_people_say_them() {
        assertEquals("½", fractionText(2))
        assertEquals("⅓", fractionText(3))
        assertEquals("⅒", fractionText(10))
        assertEquals("1/11", fractionText(11))
    }
}
