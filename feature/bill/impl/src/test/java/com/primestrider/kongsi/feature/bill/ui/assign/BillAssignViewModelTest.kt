package com.primestrider.kongsi.feature.bill.ui.assign

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kongsi.core.designsystem.component.Messenger
import com.primestrider.kongsi.core.designsystem.component.UiText
import com.primestrider.kongsi.feature.bill.impl.R
import com.primestrider.kongsi.feature.bill.model.Bill
import com.primestrider.kongsi.feature.bill.model.BillCharge
import com.primestrider.kongsi.feature.bill.model.BillItem
import com.primestrider.kongsi.feature.bill.model.Person
import com.primestrider.kongsi.feature.bill.model.SplitCalculator
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
class BillAssignViewModelTest {
    private val main = StandardTestDispatcher()
    private val messenger = Messenger()
    private val created = mutableListOf<BillAssignViewModel>()

    private val budi = 1L
    private val ani = 2L
    private val nasi = 10L
    private val steak = 11L
    private val ocha = 12L

    // The plan's worked example: service 5%, then PB1 10% on subtotal + service, Rp152.460 in all.
    private val example =
        Bill(
            id = 7,
            title = "Dinner",
            createdAt = 1,
            payerPersonId = budi,
            people = listOf(Person(budi, "Budi"), Person(ani, "Ani")),
            items =
                listOf(
                    BillItem(nasi, "Nasi Goreng", 30_000, sharedBy = emptySet()),
                    BillItem(steak, "Steak", 90_000, sharedBy = emptySet()),
                    BillItem(ocha, "Ocha", 12_000, sharedBy = emptySet()),
                ),
            service = BillCharge(6_600, 500),
            tax = BillCharge(13_860, 1_000),
        )

    private val bills = FakeBillRepository(listOf(example))

    @Before
    fun setUp() = Dispatchers.setMain(main)

    @After
    fun tearDown() {
        created.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(): BillAssignViewModel =
        BillAssignViewModel(SavedStateHandle(mapOf("billId" to example.id)), bills, messenger, this).also {
            created += it
            backgroundScope.launch { it.state.collect {} }
        }

    private suspend fun stored() = requireNotNull(bills.get(example.id))

    private suspend fun sharers() = stored().items.associate { it.id to it.sharedBy }

    @Test
    fun nothing_assigned_blocks_the_next_step_and_progress_follows_each_share() =
        runTest(main) {
            val viewModel = viewModel()
            advanceUntilIdle()

            with(viewModel.state.value) {
                assertEquals(0L, assignedMinor)
                assertEquals(152_460L, totalMinor)
                assertEquals(listOf(nasi, steak, ocha), unassignedItemIds)
                assertFalse(canContinue)
            }

            viewModel.toggle(nasi, budi)
            advanceUntilIdle()

            // Nasi Goreng with its service and tax: 30.000 + 1.500 + 3.150.
            with(viewModel.state.value) {
                assertEquals(34_650L, assignedMinor)
                assertEquals(listOf(steak, ocha), unassignedItemIds)
                assertEquals(listOf("Budi"), items.first().sharers.map { it.person.name })
                assertFalse(canContinue)
            }
        }

    @Test
    fun ticking_by_item_and_by_person_gives_the_same_split() =
        runTest(main) {
            val byItem = viewModel()
            advanceUntilIdle()
            byItem.toggle(nasi, budi)
            byItem.toggle(steak, ani)
            byItem.toggle(ocha, budi)
            byItem.toggle(ocha, ani)
            advanceUntilIdle()
            val itemFirst = sharers()
            val itemFirstState = byItem.state.value

            bills.save(example)
            val byPerson = viewModel()
            byPerson.onModeChange(AssignMode.BY_PERSON)
            byPerson.onSelectPerson(ani)
            advanceUntilIdle()
            assertEquals(ani, byPerson.state.value.selectedPersonId)
            byPerson.toggle(ocha, ani)
            byPerson.toggle(steak, ani)
            byPerson.onSelectPerson(budi)
            byPerson.toggle(ocha, budi)
            byPerson.toggle(nasi, budi)
            advanceUntilIdle()

            assertEquals(itemFirst, sharers())
            assertEquals(mapOf(nasi to setOf(budi), steak to setOf(ani), ocha to setOf(budi, ani)), itemFirst)
            assertEquals(listOf(41_580L, 110_880L), SplitCalculator.split(stored()).shares.map { it.totalMinor })
            with(byPerson.state.value) {
                assertEquals(itemFirstState.assignedMinor, assignedMinor)
                assertEquals(152_460L, assignedMinor)
                assertTrue(unassignedItemIds.isEmpty())
                assertTrue(canContinue)
            }
        }

    @Test
    fun ticking_someone_again_takes_them_off_the_item() =
        runTest(main) {
            val viewModel = viewModel()
            viewModel.toggle(ocha, ani)
            viewModel.toggle(ocha, ani)
            advanceUntilIdle()

            assertEquals(emptySet<Long>(), sharers().getValue(ocha))
        }

    @Test
    fun everyone_gives_the_item_to_all_and_again_to_no_one() =
        runTest(main) {
            val viewModel = viewModel()
            viewModel.toggle(ocha, ani)
            viewModel.toggleEveryone(ocha)
            advanceUntilIdle()
            assertEquals(setOf(budi, ani), sharers().getValue(ocha))

            viewModel.toggleEveryone(ocha)
            advanceUntilIdle()
            assertEquals(emptySet<Long>(), sharers().getValue(ocha))
        }

    @Test
    fun split_equally_shares_every_item_and_undo_brings_back_the_ticks() =
        runTest(main) {
            val viewModel = viewModel()
            viewModel.toggle(nasi, budi)
            advanceUntilIdle()

            viewModel.splitEqually()
            advanceUntilIdle()
            assertTrue(sharers().values.all { it == setOf(budi, ani) })
            assertTrue(viewModel.state.value.canContinue)

            val message = messenger.messages.first()
            assertEquals(UiText.Resource(R.string.bill_assign_split_done), message.text)
            requireNotNull(message.onAction).invoke()
            advanceUntilIdle()
            assertEquals(mapOf(nasi to setOf(budi), steak to emptySet(), ocha to emptySet()), sharers())
        }

    @Test
    fun split_equally_when_already_equal_changes_nothing() =
        runTest(main) {
            bills.save(example.copy(items = example.items.map { it.copy(sharedBy = setOf(budi, ani)) }))
            val viewModel = viewModel()
            val saves = bills.saves.size
            viewModel.splitEqually()
            advanceUntilIdle()

            assertEquals(saves, bills.saves.size)
        }

    @Test
    fun tapping_not_assigned_yet_shows_the_first_such_item_by_item() =
        runTest(main) {
            val viewModel = viewModel()
            viewModel.toggle(nasi, budi)
            viewModel.onModeChange(AssignMode.BY_PERSON)
            advanceUntilIdle()

            viewModel.onUnassignedClick()
            advanceUntilIdle()
            assertEquals(AssignMode.BY_ITEM, viewModel.state.value.mode)
            assertEquals(steak, viewModel.state.value.focusItemId)

            viewModel.onFocusShown()
            advanceUntilIdle()
            assertNull(viewModel.state.value.focusItemId)
        }

    @Test
    fun the_payer_comes_first_and_is_picked_by_default() =
        runTest(main) {
            bills.save(example.copy(payerPersonId = ani))
            val viewModel = viewModel()
            advanceUntilIdle()

            assertEquals(
                listOf(ani, budi),
                viewModel.state.value.people
                    .map { it.person.id },
            )
            assertEquals(ani, viewModel.state.value.selectedPersonId)
        }

    @Test
    fun a_failed_save_says_so() =
        runTest(main) {
            val viewModel = viewModel()
            advanceUntilIdle()
            bills.failWrites = true

            viewModel.toggle(nasi, budi)
            advanceUntilIdle()

            assertEquals(UiText.Resource(R.string.bill_save_failed), messenger.messages.first().text)
        }
}
