package com.primestrider.kongsi.feature.bill.ui.summary

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kongsi.core.designsystem.component.Messenger
import com.primestrider.kongsi.core.designsystem.component.UiText
import com.primestrider.kongsi.feature.bill.contacts.ContactMatch
import com.primestrider.kongsi.feature.bill.data.FrequentPerson
import com.primestrider.kongsi.feature.bill.impl.R
import com.primestrider.kongsi.feature.bill.model.Bill
import com.primestrider.kongsi.feature.bill.model.BillCharge
import com.primestrider.kongsi.feature.bill.model.BillItem
import com.primestrider.kongsi.feature.bill.model.BreakdownLine
import com.primestrider.kongsi.feature.bill.model.PaymentAccount
import com.primestrider.kongsi.feature.bill.model.PaymentAccountType
import com.primestrider.kongsi.feature.bill.model.Person
import com.primestrider.kongsi.feature.bill.model.SplitCalculator
import com.primestrider.kongsi.feature.bill.model.breakdownOf
import com.primestrider.kongsi.feature.bill.model.fractionText
import com.primestrider.kongsi.feature.bill.share.BillMessageBuilder
import com.primestrider.kongsi.feature.bill.ui.FakeBillRepository
import com.primestrider.kongsi.feature.bill.ui.FakeContactsRepository
import com.primestrider.kongsi.feature.bill.ui.FakeFrequentPeopleRepository
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Locale

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
            people = listOf(Person(me, "Ricky"), Person(budi, "Budi", "6281234567890"), Person(ani, "Ani Lestari")),
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
    private val bills = FakeBillRepository(listOf(example))

    private val bca = PaymentAccount(1, PaymentAccountType.BANK, "BCA", "1234567890", "Ricky", isDefault = true)
    private val gopay = PaymentAccount(2, PaymentAccountType.EWALLET, "GoPay", "081234567890", "Ricky", isDefault = false)
    private val accounts = FakePaymentAccountRepository(listOf(bca, gopay))
    private val messenger = Messenger()
    private val aniContact =
        ContactMatch(5, "Ani Lestari", "0857-1111-2222", "6285711112222", mobile = true, photoUri = null, hasWhatsApp = true)
    private val contacts = FakeContactsRepository(listOf(aniContact))

    @Before
    fun setUp() = Dispatchers.setMain(main)

    @After
    fun tearDown() {
        created.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(bill: Bill? = null): BillSummaryViewModel {
        val repository = if (bill == null) bills else FakeBillRepository(listOf(bill))
        return BillSummaryViewModel(
            SavedStateHandle(mapOf("billId" to (bill ?: example).id)),
            repository,
            frequent,
            accounts,
            contacts,
            messenger,
            this,
        ).also {
            created += it
            backgroundScope.launch { it.state.collect {} }
        }
    }

    private suspend fun storedAccounts() = requireNotNull(bills.get(example.id)).paymentAccountIds

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
            assertEquals(listOf(ani), ocha.sharedWith)
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
            return breakdownOf(bill, SplitCalculator.split(bill).shares.first())
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

            assertEquals(listOf(listOf("Budi", "Ani Lestari")), frequent.remembered.map { people -> people.map { it.name } })
        }

    @Test
    fun a_new_bill_starts_with_the_default_accounts_once() =
        runTest(main) {
            val viewModel = viewModel()
            advanceUntilIdle()

            assertEquals(setOf(bca.id), storedAccounts())
            assertEquals(listOf(bca, gopay), viewModel.state.value.accounts)
            assertEquals(setOf(bca.id), viewModel.state.value.selectedAccountIds)

            // Taking the default off is a choice: opening the Summary again does not bring it back.
            viewModel.toggleAccount(bca.id)
            advanceUntilIdle()
            viewModel(null)
            advanceUntilIdle()
            assertEquals(emptySet<Long>(), storedAccounts())
        }

    @Test
    fun the_choice_is_saved_with_the_bill_and_survives_reopening() =
        runTest(main) {
            viewModel().toggleAccount(gopay.id)
            advanceUntilIdle()

            assertEquals(setOf(bca.id, gopay.id), storedAccounts())
            val reopened = viewModel()
            advanceUntilIdle()
            assertEquals(setOf(bca.id, gopay.id), reopened.state.value.selectedAccountIds)
        }

    @Test
    fun an_account_added_from_the_summary_becomes_the_bills_default() =
        runTest(main) {
            accounts.delete(bca.id)
            accounts.delete(gopay.id)
            val viewModel = viewModel()
            advanceUntilIdle()
            assertNull(storedAccounts())

            val added = accounts.save(bca.copy(id = 0))
            advanceUntilIdle()

            assertEquals(setOf(added), storedAccounts())
            assertEquals(setOf(added), viewModel.state.value.selectedAccountIds)
        }

    @Test
    fun sending_to_someone_gives_their_message_and_number() =
        runTest(main) {
            val viewModel = viewModel()
            advanceUntilIdle()
            val sent = mutableListOf<Pair<String, String?>>()

            viewModel.send(budi, Locale.US) { message, phone -> sent += message to phone }
            advanceUntilIdle()

            val expected = BillMessageBuilder.forPerson(requireNotNull(bills.get(example.id)), budi, listOf(bca), Locale.US)
            assertEquals(listOf(expected to "6281234567890"), sent)
        }

    @Test
    fun sending_to_the_group_gives_the_summary_without_a_number() =
        runTest(main) {
            val viewModel = viewModel()
            advanceUntilIdle()
            val sent = mutableListOf<Pair<String, String?>>()

            viewModel.send(null, Locale.US) { message, phone -> sent += message to phone }
            advanceUntilIdle()

            val expected = BillMessageBuilder.forGroup(requireNotNull(bills.get(example.id)), listOf(bca), Locale.US)
            assertEquals(listOf(expected to null), sent)
        }

    @Test
    fun add_number_searches_for_the_person_by_name() =
        runTest(main) {
            frequent.stored =
                listOf(
                    FrequentPerson(1, "Ani Lestari", "6289999888877", useCount = 2),
                    FrequentPerson(2, "Ani Lestari", null, useCount = 1),
                )
            val viewModel = viewModel()
            viewModel.onContactsAllowed(true)
            advanceUntilIdle()

            viewModel.onAddNumber(ani)
            advanceUntilIdle()

            assertEquals(ani, viewModel.state.value.numberForId)
            assertEquals("Ani Lestari", viewModel.numberQuery)
            // Only people with a number are offered: remembered first, then the address book.
            assertEquals(
                listOf(NumberOption("Ani Lestari", "6289999888877"), NumberOption("Ani Lestari", "6285711112222", whatsApp = true)),
                viewModel.state.value.numberOptions,
            )
        }

    @Test
    fun a_typed_mobile_number_is_offered_first() =
        runTest(main) {
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onAddNumber(ani)
            viewModel.onNumberQueryChange("0812 9999 0000")
            advanceUntilIdle()

            assertEquals(
                NumberOption(null, "6281299990000"),
                viewModel.state.value.numberOptions
                    .first(),
            )
        }

    @Test
    fun picking_a_number_saves_it_on_the_person_and_closes_the_search() =
        runTest(main) {
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onAddNumber(ani)
            advanceUntilIdle()

            viewModel.pickNumber(NumberOption("Ani Lestari", "6285711112222"))
            advanceUntilIdle()

            assertEquals("6285711112222", requireNotNull(bills.get(example.id)).people.single { it.id == ani }.phone)
            assertNull(viewModel.state.value.numberForId)
        }

    @Test
    fun a_number_already_on_the_bill_is_not_given_to_someone_else() =
        runTest(main) {
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onAddNumber(ani)

            viewModel.pickNumber(NumberOption(null, "081234567890"))
            advanceUntilIdle()

            assertNull(requireNotNull(bills.get(example.id)).people.single { it.id == ani }.phone)
            assertEquals(UiText.Resource(R.string.bill_summary_number_taken, listOf("Budi")), messenger.messages.first().text)
        }

    @Test
    fun the_system_picker_fills_in_the_number() =
        runTest(main) {
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onAddNumber(ani)

            viewModel.onNumberPickerResult(Uri.parse("content://com.android.contacts/data/phones/5"))
            advanceUntilIdle()

            assertEquals("6285711112222", requireNotNull(bills.get(example.id)).people.single { it.id == ani }.phone)
        }

    @Test
    fun shared_item_fractions_read_as_people_say_them() {
        assertEquals("½", fractionText(2))
        assertEquals("⅓", fractionText(3))
        assertEquals("⅒", fractionText(10))
        assertEquals("1/11", fractionText(11))
    }
}
