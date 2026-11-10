package com.primestrider.kongsi.feature.bill.ui.people

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kongsi.core.datastore.UserProfile
import com.primestrider.kongsi.core.datastore.UserProfileRepository
import com.primestrider.kongsi.core.designsystem.component.Messenger
import com.primestrider.kongsi.core.designsystem.component.UiText
import com.primestrider.kongsi.feature.bill.contacts.ContactMatch
import com.primestrider.kongsi.feature.bill.contacts.ContactsRepository
import com.primestrider.kongsi.feature.bill.data.FrequentPerson
import com.primestrider.kongsi.feature.bill.impl.R
import com.primestrider.kongsi.feature.bill.model.Bill
import com.primestrider.kongsi.feature.bill.model.BillItem
import com.primestrider.kongsi.feature.bill.model.Person
import com.primestrider.kongsi.feature.bill.ui.FakeBillRepository
import com.primestrider.kongsi.feature.bill.ui.FakeFrequentPeopleRepository
import com.primestrider.kongsi.testing.InMemoryPreferencesDataStore
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
class BillPeopleViewModelTest {
    private val main = StandardTestDispatcher()
    private val messenger = Messenger()
    private val created = mutableListOf<BillPeopleViewModel>()

    private val budiContact =
        ContactMatch(1, "Budi Santoso", "0812-3456-7890", "6281234567890", mobile = true, photoUri = null, hasWhatsApp = true)
    private val draft =
        Bill(
            id = 7,
            title = "Sushi",
            createdAt = 1,
            people = emptyList(),
            items = listOf(BillItem(20, "Roll", 90_000, sharedBy = emptySet())),
        )

    @Before
    fun setUp() = Dispatchers.setMain(main)

    @After
    fun tearDown() {
        created.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
    }

    private class FakeContacts(
        private val matches: List<ContactMatch>,
    ) : ContactsRepository {
        val searched = mutableListOf<String>()

        override suspend fun search(
            query: String,
            limit: Int,
        ): List<ContactMatch> {
            searched += query
            return matches.filter { it.name.contains(query, ignoreCase = true) }
        }

        override suspend fun fromPickerResult(uri: Uri): ContactMatch? = matches.firstOrNull()
    }

    private val bills = FakeBillRepository(listOf(draft))
    private val contacts = FakeContacts(listOf(budiContact))
    private val frequent = FakeFrequentPeopleRepository()

    private suspend fun TestScope.viewModel(profile: UserProfile = UserProfile("Ricky", "081111222233")): BillPeopleViewModel {
        val profiles = UserProfileRepository(InMemoryPreferencesDataStore()).apply { save(profile) }
        return BillPeopleViewModel(
            SavedStateHandle(mapOf("billId" to draft.id)),
            bills,
            contacts,
            frequent,
            profiles,
            messenger,
        ).also {
            created += it
            backgroundScope.launch { it.state.collect {} }
        }
    }

    private suspend fun stored() = requireNotNull(bills.get(draft.id))

    @Test
    fun the_payer_is_added_once_from_the_profile() =
        runTest(main) {
            val viewModel = viewModel()
            advanceUntilIdle()

            val bill = stored()
            val me = bill.people.single()
            assertEquals(Person(me.id, "Ricky", "6281111222233"), me)
            assertEquals(me.id, bill.payerPersonId)
            assertEquals(
                listOf(true),
                viewModel.state.value.people
                    .map { it.isMe },
            )
            assertFalse(viewModel.state.value.canContinue)

            // Opening the step again does not add a second "me".
            viewModel()
            advanceUntilIdle()
            assertEquals(1, stored().people.size)
        }

    @Test
    fun a_contact_is_added_with_its_number() =
        runTest(main) {
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onContactsAllowed(true)
            viewModel.onQueryChange("budi")
            advanceUntilIdle()

            val suggestion =
                viewModel.state.value.suggestions
                    .filterIsInstance<Suggestion.Contact>()
                    .single()
            viewModel.add(suggestion)
            advanceUntilIdle()

            assertEquals(listOf("Ricky", "Budi Santoso"), stored().people.map { it.name })
            assertEquals("6281234567890", stored().people[1].phone)
            assertEquals("", viewModel.query)
            assertTrue(viewModel.state.value.canContinue)
        }

    @Test
    fun the_same_number_cannot_be_added_twice() =
        runTest(main) {
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.add(Suggestion.Contact(budiContact))
            advanceUntilIdle()

            viewModel.add(Suggestion.Contact(budiContact.copy(name = "Pak Budi", phone = "+62 812 3456 7890")))
            advanceUntilIdle()

            assertEquals(2, stored().people.size)
            assertEquals(UiText.Resource(R.string.bill_people_already_added, listOf("Budi Santoso")), messenger.messages.first().text)
        }

    @Test
    fun the_payers_own_number_counts_as_already_added() =
        runTest(main) {
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.add(
                Suggestion.Contact(budiContact.copy(name = "Ricky HP", phone = "0811-1122-2233", international = "6281111222233")),
            )
            advanceUntilIdle()

            assertEquals(1, stored().people.size)
        }

    @Test
    fun the_same_name_twice_is_allowed_and_told_apart() =
        runTest(main) {
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onQueryChange("Budi")
            viewModel.add(Suggestion.Typed("Budi"))
            advanceUntilIdle()
            viewModel.add(Suggestion.Contact(budiContact.copy(name = "Budi")))
            advanceUntilIdle()
            viewModel.add(Suggestion.Typed("budi"))
            advanceUntilIdle()

            assertEquals(listOf("Ricky", "Budi", "Budi", "budi"), stored().people.map { it.name })
            assertEquals(
                listOf("Ricky", "Budi", "Budi · 7890", "budi 2"),
                viewModel.state.value.people
                    .map { it.label },
            )
        }

    @Test
    fun without_the_contacts_permission_only_remembered_people_and_the_typed_name_are_offered() =
        runTest(main) {
            frequent.stored = listOf(FrequentPerson(1, "Budi Lama", "6289999888877", useCount = 3))
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onContactsAllowed(false)

            viewModel.onQueryChange("bud")
            advanceUntilIdle()

            assertEquals(
                listOf(Suggestion.Frequent(frequent.stored.single()), Suggestion.Typed("bud")),
                viewModel.state.value.suggestions,
            )
            assertTrue(contacts.searched.isEmpty())
        }

    @Test
    fun remembered_people_come_before_contacts_and_people_on_the_bill_are_not_offered() =
        runTest(main) {
            frequent.stored =
                listOf(
                    FrequentPerson(1, "Budi Santoso", "6281234567890", useCount = 3),
                    FrequentPerson(2, "Budi Lama", "6289999888877", useCount = 1),
                )
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onContactsAllowed(true)
            viewModel.onQueryChange("budi")
            advanceUntilIdle()

            assertEquals(
                listOf("Frequent Budi Santoso", "Frequent Budi Lama", "Typed budi"),
                viewModel.state.value.suggestions
                    .map { it.describe() },
            )

            viewModel.add(
                viewModel.state.value.suggestions
                    .first(),
            )
            viewModel.onQueryChange("budi")
            advanceUntilIdle()
            assertEquals(
                listOf("Frequent Budi Lama", "Typed budi"),
                viewModel.state.value.suggestions
                    .map { it.describe() },
            )
        }

    @Test
    fun removing_someone_also_removes_them_from_the_items_they_shared() =
        runTest(main) {
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.add(Suggestion.Contact(budiContact))
            advanceUntilIdle()
            val (me, budi) = stored().people
            bills.save(stored().copy(items = stored().items.map { it.copy(sharedBy = setOf(me.id, budi.id)) }))
            advanceUntilIdle()

            viewModel.remove(budi.id)
            advanceUntilIdle()

            assertEquals(listOf(me.id), stored().people.map { it.id })
            assertEquals(setOf(me.id), stored().items.single().sharedBy)
            // The payer stays.
            viewModel.remove(me.id)
            advanceUntilIdle()
            assertEquals(1, stored().people.size)
        }

    @Test
    fun picking_from_the_system_picker_adds_that_number() =
        runTest(main) {
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onPickerResult(Uri.parse("content://com.android.contacts/data/phones/101"))
            advanceUntilIdle()

            assertEquals("Budi Santoso", stored().people[1].name)
        }

    private fun Suggestion.describe() =
        when (this) {
            is Suggestion.Frequent -> "Frequent ${person.name}"
            is Suggestion.Contact -> "Contact ${match.name}"
            is Suggestion.Typed -> "Typed $name"
        }
}
