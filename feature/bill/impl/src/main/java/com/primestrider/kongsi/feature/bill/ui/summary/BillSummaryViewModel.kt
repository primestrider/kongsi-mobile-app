package com.primestrider.kongsi.feature.bill.ui.summary

import android.database.sqlite.SQLiteException
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.primestrider.kongsi.core.common.ApplicationScope
import com.primestrider.kongsi.core.designsystem.component.Messenger
import com.primestrider.kongsi.core.designsystem.component.UiText
import com.primestrider.kongsi.core.util.IndonesianPhone
import com.primestrider.kongsi.feature.bill.contacts.ContactsRepository
import com.primestrider.kongsi.feature.bill.data.BillRepository
import com.primestrider.kongsi.feature.bill.data.FrequentPeopleRepository
import com.primestrider.kongsi.feature.bill.data.PaymentAccountRepository
import com.primestrider.kongsi.feature.bill.impl.R
import com.primestrider.kongsi.feature.bill.model.Bill
import com.primestrider.kongsi.feature.bill.model.BreakdownLine
import com.primestrider.kongsi.feature.bill.model.PaymentAccount
import com.primestrider.kongsi.feature.bill.model.SplitCalculator
import com.primestrider.kongsi.feature.bill.model.breakdownOf
import com.primestrider.kongsi.feature.bill.share.BillMessageBuilder
import com.primestrider.kongsi.feature.bill.ui.people.PersonRow
import com.primestrider.kongsi.feature.bill.ui.people.rowsOf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale
import javax.inject.Inject

/** A person on the Summary: what they owe and how it was worked out. */
data class SummaryPerson(
    val row: PersonRow,
    val totalMinor: Long,
    val lines: List<BreakdownLine>,
)

/** A number "Add number" offers: someone found by the search, or the typed number itself when [name] is null. */
data class NumberOption(
    val name: String?,
    /** The `628…` form for an Indonesian mobile, otherwise as saved in the address book. */
    val phone: String,
    val whatsApp: Boolean = false,
)

data class BillSummaryUiState(
    val loaded: Boolean = false,
    val title: String = "",
    /** The payer first. */
    val people: List<SummaryPerson> = emptyList(),
    val totalMinor: Long = 0,
    val assignedMinor: Long = 0,
    /** Every item has someone and every charge an item to go on, so the shares add up to [totalMinor]. */
    val complete: Boolean = false,
    /** Whose "How was this calculated?" sheet is open. */
    val openPersonId: Long? = null,
    /** Every saved account, in the user's order; the message lists the [selectedAccountIds] ones. */
    val accounts: List<PaymentAccount> = emptyList(),
    val selectedAccountIds: Set<Long> = emptySet(),
    /** Whose number is being looked up ("Add number"), and what the search offers. */
    val numberForId: Long? = null,
    val numberOptions: List<NumberOption> = emptyList(),
)

/**
 * Summary: what each person owes, the payer's own share marked and not requested, and for everyone a breakdown in
 * plain words, and the payment accounts friends transfer to. A bill starts with the default accounts, applied once (as
 * soon as there is an account); after that the choice is the bill's own and saved with it. Each person's message, or
 * the summary for the group, is sent through WhatsApp; someone without a number gets one from the same contact search
 * as the People step. "Done" remembers the people for the next bill.
 */
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class BillSummaryViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val bills: BillRepository,
        private val frequent: FrequentPeopleRepository,
        private val paymentAccounts: PaymentAccountRepository,
        private val contacts: ContactsRepository,
        private val messenger: Messenger,
        @ApplicationScope private val appScope: CoroutineScope,
    ) : ViewModel() {
        private val billId = savedStateHandle.get<Long>(BILL_ID) ?: 0L
        private val writes = Mutex()

        /** The "Add number" search text, as Compose state so the field never gets a stale value while typing. */
        var numberQuery by mutableStateOf("")
            private set
        private val numberQueryFlow = MutableStateFlow("")
        private val contactsAllowed = MutableStateFlow(false)
        private val numberFor = savedStateHandle.getStateFlow<Long?>(NUMBER_FOR, null)

        private val numberOptions =
            combine(numberFor, numberQueryFlow, contactsAllowed) { person, text, allowed -> Triple(person, text, allowed) }
                .mapLatest { (person, text, allowed) -> if (person == null) emptyList() else numberOptionsFor(text, allowed) }

        val state: StateFlow<BillSummaryUiState> =
            combine(
                bills.observe(billId),
                savedStateHandle.getStateFlow<Long?>(OPEN_PERSON, null),
                paymentAccounts.accounts,
                numberFor,
                numberOptions,
            ) { stored, open, accounts, numberPerson, options ->
                if (stored == null) return@combine BillSummaryUiState(loaded = true)
                val result = SplitCalculator.split(stored)
                val rows = rowsOf(stored)
                val shares = result.shares.associateBy { it.personId }
                val known = { id: Long? -> id?.takeIf { rows.any { it.person.id == id } } }
                BillSummaryUiState(
                    loaded = true,
                    title = stored.title,
                    people =
                        rows.map { row ->
                            val share = shares.getValue(row.person.id)
                            SummaryPerson(row, share.totalMinor, breakdownOf(stored, share))
                        },
                    totalMinor = result.totalMinor,
                    assignedMinor = result.assignedMinor,
                    complete = result.isComplete,
                    openPersonId = known(open),
                    accounts = accounts,
                    selectedAccountIds = stored.paymentAccountIds ?: accounts.defaultIds(),
                    numberForId = known(numberPerson),
                    numberOptions = options,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BillSummaryUiState())

        init {
            viewModelScope.launch {
                // Waits for the first account, so one added from here ("Add account") is picked up too.
                val accounts = paymentAccounts.accounts.first { it.isNotEmpty() }
                save { stored -> if (stored.paymentAccountIds == null) stored.copy(paymentAccountIds = accounts.defaultIds()) else null }
            }
        }

        /** Adds the account to this bill's message, or takes it off. */
        fun toggleAccount(accountId: Long) {
            viewModelScope.launch {
                save { stored ->
                    val selected = stored.paymentAccountIds ?: paymentAccounts.accounts.first().defaultIds()
                    stored.copy(paymentAccountIds = if (accountId in selected) selected - accountId else selected + accountId)
                }
            }
        }

        /** Marks [personId] as having paid their part, or not any more. */
        fun togglePaid(personId: Long) {
            viewModelScope.launch {
                save { stored ->
                    if (stored.people.none { it.id == personId }) return@save null
                    stored.copy(people = stored.people.map { if (it.id == personId) it.copy(paid = !it.paid) else it })
                }
            }
        }

        fun onOpenBreakdown(personId: Long) {
            savedStateHandle[OPEN_PERSON] = personId
        }

        fun onCloseBreakdown() {
            savedStateHandle[OPEN_PERSON] = null
        }

        /**
         * Builds the message for [personId] (the group summary when null) in [locale] with this bill's accounts, and
         * hands it to [deliver] with the number to open the chat with, if any.
         */
        fun send(
            personId: Long?,
            locale: Locale,
            deliver: (message: String, phone: String?) -> Unit,
        ) {
            viewModelScope.launch {
                val stored = bills.get(billId) ?: return@launch
                val all = paymentAccounts.accounts.first()
                val chosen = stored.paymentAccountIds ?: all.defaultIds()
                val accounts = all.filter { it.id in chosen }
                if (personId == null) {
                    deliver(BillMessageBuilder.forGroup(stored, accounts, locale), null)
                } else {
                    val person = stored.people.firstOrNull { it.id == personId } ?: return@launch
                    deliver(BillMessageBuilder.forPerson(stored, personId, accounts, locale), person.phone)
                }
            }
        }

        /** "Add number": searches for [personId] by their name. */
        fun onAddNumber(personId: Long) {
            val name =
                state.value.people
                    .firstOrNull { it.row.person.id == personId }
                    ?.row
                    ?.person
                    ?.name
                    .orEmpty()
            onNumberQueryChange(name)
            savedStateHandle[NUMBER_FOR] = personId
        }

        fun onCloseNumber() {
            savedStateHandle[NUMBER_FOR] = null
        }

        fun onNumberQueryChange(text: String) {
            numberQuery = text
            numberQueryFlow.value = text
        }

        /** Whether the address book may be searched; the sheet asks for the permission and reports the answer. */
        fun onContactsAllowed(allowed: Boolean) {
            contactsAllowed.value = allowed
        }

        fun pickNumber(option: NumberOption) {
            val personId = numberFor.value ?: return
            viewModelScope.launch { setNumber(personId, option.phone) }
        }

        /** The number the system contact picker returned; null when the user backed out. */
        fun onNumberPickerResult(uri: Uri?) {
            val personId = numberFor.value ?: return
            uri ?: return
            viewModelScope.launch {
                val match = contacts.fromPickerResult(uri) ?: return@launch
                setNumber(personId, match.international ?: match.phone)
            }
        }

        /** The bill is done: remembers everyone but the payer for next time (also after the screen is gone). */
        fun finish() {
            appScope.launch {
                val stored = bills.get(billId) ?: return@launch
                frequent.remember(stored.people.filterNot { it.id == stored.payerPersonId })
            }
        }

        /** Gives [personId] the number [phone] and closes the search; a number someone else on the bill has is refused. */
        private suspend fun setNumber(
            personId: Long,
            phone: String,
        ) {
            val number = IndonesianPhone.international(phone) ?: phone
            var saved = false
            save { stored ->
                val owner = stored.people.firstOrNull { it.id != personId && it.phone?.let(IndonesianPhone::international) == number }
                if (owner != null) {
                    messenger.info(UiText.Resource(R.string.bill_summary_number_taken, listOf(owner.name)))
                    null
                } else {
                    saved = true
                    stored.copy(people = stored.people.map { if (it.id == personId) it.copy(phone = number) else it })
                }
            }
            if (saved) onCloseNumber()
        }

        /**
         * What "Add number" offers for [text]: the typed number when it is an Indonesian mobile, then people from
         * earlier bills who have a number, then address-book matches (with the permission); each number once.
         */
        private suspend fun numberOptionsFor(
            text: String,
            allowed: Boolean,
        ): List<NumberOption> {
            val typed = IndonesianPhone.international(text)?.let { listOf(NumberOption(null, it)) }.orEmpty()
            val remembered = frequent.suggestions(text).mapNotNull { f -> f.phone?.let { NumberOption(f.name, it) } }
            val found =
                if (allowed && text.isNotBlank()) {
                    contacts.search(text).map { NumberOption(it.name, it.international ?: it.phone, it.hasWhatsApp) }
                } else {
                    emptyList()
                }
            return (typed + remembered + found).distinctBy { IndonesianPhone.international(it.phone) ?: it.phone }
        }

        /** Applies [update] to the stored bill and saves it; an update returning null changes nothing. */
        private suspend fun save(update: suspend (Bill) -> Bill?) {
            writes.withLock {
                try {
                    val stored = bills.get(billId) ?: return@withLock
                    val updated = update(stored) ?: return@withLock
                    bills.save(updated)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: SQLiteException) {
                    messenger.error(UiText.Resource(R.string.bill_save_failed))
                }
            }
        }

        companion object {
            const val BILL_ID = "billId"
            private const val OPEN_PERSON = "openPerson"
            private const val NUMBER_FOR = "numberFor"
        }
    }

private fun List<PaymentAccount>.defaultIds(): Set<Long> = filter { it.isDefault }.map { it.id }.toSet()
