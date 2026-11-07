package com.primestrider.kongsi.feature.bill.ui.review

import android.database.sqlite.SQLiteException
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.primestrider.kongsi.core.common.ApplicationScope
import com.primestrider.kongsi.core.designsystem.component.MessageVariant
import com.primestrider.kongsi.core.designsystem.component.Messenger
import com.primestrider.kongsi.core.designsystem.component.UiMessage
import com.primestrider.kongsi.core.designsystem.component.UiText
import com.primestrider.kongsi.feature.bill.data.BillRepository
import com.primestrider.kongsi.feature.bill.impl.R
import com.primestrider.kongsi.feature.bill.model.Bill
import com.primestrider.kongsi.feature.bill.model.BillCharge
import com.primestrider.kongsi.feature.bill.model.BillItem
import com.primestrider.kongsi.feature.bill.model.ChargeType
import com.primestrider.kongsi.feature.bill.model.SplitCalculator
import com.primestrider.kongsi.feature.bill.model.SplitIssue
import com.primestrider.kongsi.feature.bill.ui.flow.parseMoneyInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

/** The amounts typed on Review besides the items. */
enum class ChargeField(
    /** The charge it is, for warnings; the receipt total is not a charge. */
    val charge: ChargeType?,
) {
    SERVICE(ChargeType.SERVICE),
    TAX(ChargeType.TAX),
    DISCOUNT(ChargeType.DISCOUNT),
    RECEIPT_TOTAL(null),
}

/** A charge field's text as typed: [amount] in rupiah, [rate] in percent (service and tax only, empty when unused). */
data class ChargeInput(
    val amount: String = "",
    val rate: String = "",
)

/** The add/edit item sheet. [itemId] 0 adds a new item. [price] is the line total (price × quantity) as typed. */
data class ItemEditor(
    val itemId: Long = 0,
    val name: String = "",
    val quantity: String = "1",
    val price: String = "",
    val serviceable: Boolean = true,
    val taxable: Boolean = true,
    val nameError: Boolean = false,
    val quantityError: Boolean = false,
    val priceError: Boolean = false,
)

/** What tapping a warning shows: an item, or one of the charge fields. */
sealed interface ReviewFocus {
    data class Item(
        val id: Long,
    ) : ReviewFocus

    data class Charge(
        val field: ChargeField,
    ) : ReviewFocus
}

/** A Split Rules check to show on Review, and what tapping it should show. */
data class ReviewWarning(
    val issue: SplitIssue,
    val focus: ReviewFocus,
) {
    val blocking: Boolean get() = issue.blocking
}

data class BillReviewUiState(
    /** False until the bill has been read, so fields do not flash empty. */
    val loaded: Boolean = false,
    val title: String = "",
    val items: List<BillItem> = emptyList(),
    val charges: Map<ChargeField, ChargeInput> = ChargeField.entries.associateWith { ChargeInput() },
    val totalMinor: Long = 0,
    val receiptMinor: Long? = null,
    val warnings: List<ReviewWarning> = emptyList(),
    /** True when there are items and no warning blocks the next step. */
    val canContinue: Boolean = false,
    val editor: ItemEditor? = null,
    /** Set by tapping a warning; the screen scrolls to it, highlights it, then calls `onFocusShown`. */
    val focus: ReviewFocus? = null,
)

/**
 * Review: the bill's items and charges, checked against the receipt. A new bill ([BILL_ID] 0) is only saved once it
 * has something in it; from then on every change is saved right away, so leaving the flow loses nothing, and the new
 * id goes into the [SavedStateHandle] so the screen comes back to the same draft.
 */
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class BillReviewViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val repository: BillRepository,
        private val messenger: Messenger,
        @ApplicationScope private val appScope: CoroutineScope,
    ) : ViewModel() {
        private val billId = MutableStateFlow(savedStateHandle.get<Long>(BILL_ID) ?: 0L)

        /** The bill being edited: the stored one, or an empty draft until the first save. */
        private val bill =
            billId.flatMapLatest { id -> if (id == 0L) flowOf(Bill(people = emptyList(), items = emptyList())) else repository.observe(id) }

        /**
         * The name and charge fields as typed. They are Compose state, read synchronously by the text fields: going
         * through a flow would hand the field a stale value for a frame and make the cursor jump while typing.
         */
        var title by mutableStateOf("")
            private set
        var charges by mutableStateOf(ChargeField.entries.associateWith { ChargeInput() })
            private set
        private var inputsLoaded = false

        private val editor = MutableStateFlow<ItemEditor?>(null)
        private val focus = MutableStateFlow<ReviewFocus?>(null)

        /** Saves run one after another, each on top of the last saved bill. */
        private val writes = Mutex()

        val state: StateFlow<BillReviewUiState> =
            combine(bill, editor, focus) { stored, sheet, focused ->
                if (stored == null) return@combine BillReviewUiState(loaded = true)
                if (!inputsLoaded) {
                    // The fields start from the stored bill once; after that they are what the user types.
                    inputsLoaded = true
                    title = stored.title
                    charges = stored.chargeInputs()
                }
                val result = SplitCalculator.split(stored)
                val warnings =
                    result.issues
                        .filterNot { it is SplitIssue.UnassignedItems }
                        .map { ReviewWarning(it, focusOf(it, stored)) }
                BillReviewUiState(
                    loaded = true,
                    title = title,
                    items = stored.items,
                    charges = charges,
                    totalMinor = result.totalMinor,
                    receiptMinor = stored.receiptTotalMinor,
                    warnings = warnings,
                    canContinue = stored.items.isNotEmpty() && warnings.none { it.blocking },
                    editor = sheet,
                    focus = focused,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BillReviewUiState())

        fun onTitleChange(text: String) {
            title = text
            change { it.copy(title = text.trim()) }
        }

        /** Typing an amount replaces any rate the charge had: the amount is what the receipt says. */
        fun onChargeChange(
            field: ChargeField,
            text: String,
        ) {
            val amount = parseMoneyInput(text)
            setCharge(field, ChargeInput(amount = text, rate = ""))
            if (text.isNotBlank() && amount == null) return
            change { it.withCharge(field, BillCharge(amount ?: 0)) }
        }

        /** The "%" helper: fills the charge from a rate of its base and keeps the rate to check the receipt against. */
        fun onRateChange(
            field: ChargeField,
            text: String,
        ) {
            val type = field.charge ?: return
            val current = charges.getValue(field)
            val bps = parseRateBps(text)
            // The field shows what was typed at once; the amount follows once it is worked out.
            setCharge(field, current.copy(rate = text))
            if (bps == null) return
            change { stored ->
                val amount = SplitCalculator.chargeAmount(stored, type, bps)
                setCharge(field, ChargeInput(amount = amount.toString(), rate = text))
                stored.withCharge(field, BillCharge(amount, bps))
            }
        }

        fun startNewItem() {
            editor.value = ItemEditor()
        }

        fun editItem(id: Long) {
            val item = state.value.items.firstOrNull { it.id == id } ?: return
            editor.value =
                ItemEditor(
                    itemId = item.id,
                    name = item.name,
                    quantity = item.quantity.toString(),
                    price = item.totalMinor.toString(),
                    serviceable = item.serviceable,
                    taxable = item.taxable,
                )
        }

        fun updateEditor(update: (ItemEditor) -> ItemEditor) {
            editor.update { current -> current?.let(update) }
        }

        fun dismissEditor() {
            editor.value = null
        }

        /** Saves the sheet's item, or marks what is missing and keeps the sheet open. */
        fun saveItem() {
            val draft = editor.value ?: return
            val price = parseMoneyInput(draft.price)
            val quantity = draft.quantity.trim().toIntOrNull()
            val checked =
                draft.copy(
                    nameError = draft.name.isBlank(),
                    quantityError = quantity == null || quantity < 1,
                    priceError = price == null || price <= 0,
                )
            if (checked.nameError || checked.quantityError || checked.priceError) {
                editor.value = checked
                return
            }
            editor.value = null
            change { stored ->
                val existing = stored.items.firstOrNull { it.id == draft.itemId }
                val item =
                    (existing ?: BillItem(id = 0, name = "", totalMinor = 0, sharedBy = emptySet())).copy(
                        name = draft.name.trim(),
                        quantity = requireNotNull(quantity),
                        totalMinor = requireNotNull(price),
                        serviceable = draft.serviceable,
                        taxable = draft.taxable,
                    )
                val items = if (existing == null) stored.items + item else stored.items.map { if (it.id == item.id) item else it }
                stored.copy(items = items)
            }
        }

        /** Deletes at once and offers Undo, which puts the item back where it was (also after leaving the screen). */
        fun deleteItem(id: Long) {
            editor.value = null
            viewModelScope.launch {
                val stored = state.value
                val index = stored.items.indexOfFirst { it.id == id }
                if (index < 0) return@launch
                val removed = stored.items[index]
                val saved = save { it.copy(items = it.items.filterNot { item -> item.id == id }) } ?: return@launch
                messenger.show(
                    UiMessage(
                        text = UiText.Resource(R.string.bill_item_deleted, listOf(removed.name)),
                        variant = MessageVariant.INFO,
                        actionLabel = UiText.Resource(R.string.bill_undo),
                        onAction = { appScope.launch { restoreItem(saved, removed, index) } },
                    ),
                )
            }
        }

        fun onWarningClick(warning: ReviewWarning) {
            focus.value = warning.focus
        }

        fun onFocusShown() {
            focus.value = null
        }

        private suspend fun restoreItem(
            billId: Long,
            item: BillItem,
            index: Int,
        ) {
            try {
                val stored = repository.get(billId) ?: return
                val items = stored.items.toMutableList().apply { add(index.coerceAtMost(size), item) }
                repository.save(stored.copy(items = items))
            } catch (e: SQLiteException) {
                messenger.error(UiText.Resource(R.string.bill_save_failed))
            }
        }

        private fun setCharge(
            field: ChargeField,
            input: ChargeInput,
        ) {
            charges = charges + (field to input)
        }

        private fun change(update: (Bill) -> Bill) {
            viewModelScope.launch { save(update) }
        }

        /** Applies [update] to the latest bill and saves it; the first save of a new bill gives it its id. */
        private suspend fun save(update: (Bill) -> Bill): Long? =
            writes.withLock {
                try {
                    val id = billId.value
                    val current = (if (id == 0L) null else repository.get(id)) ?: Bill(people = emptyList(), items = emptyList())
                    val updated = update(current)
                    if (id == 0L && updated.isEmptyDraft()) return@withLock null
                    val savedId = repository.save(updated)
                    if (id == 0L) {
                        savedStateHandle[BILL_ID] = savedId
                        billId.value = savedId
                    }
                    savedId
                } catch (e: CancellationException) {
                    throw e
                } catch (e: SQLiteException) {
                    messenger.error(UiText.Resource(R.string.bill_save_failed))
                    null
                }
            }

        companion object {
            /** The route argument (`BillEditRoute.billId`); 0 starts a new bill. */
            const val BILL_ID = "billId"
        }
    }

/** Where a warning points: the item most likely to explain it, or the field to fix. */
private fun focusOf(
    issue: SplitIssue,
    bill: Bill,
): ReviewFocus =
    when (issue) {
        is SplitIssue.RateMismatch -> {
            val suspect =
                bill.items.firstOrNull {
                    when (issue.charge) {
                        ChargeType.SERVICE -> !it.serviceable
                        ChargeType.TAX -> !it.taxable
                        ChargeType.DISCOUNT -> false
                    }
                }
            suspect?.let { ReviewFocus.Item(it.id) } ?: ReviewFocus.Charge(issue.charge.field())
        }
        is SplitIssue.NoEligibleItem ->
            bill.items
                .firstOrNull()
                ?.takeIf { issue.charge != ChargeType.DISCOUNT }
                ?.let { ReviewFocus.Item(it.id) }
                ?: ReviewFocus.Charge(issue.charge.field())
        is SplitIssue.ReceiptTotalMismatch -> ReviewFocus.Charge(ChargeField.RECEIPT_TOTAL)
        is SplitIssue.UnassignedItems -> ReviewFocus.Item(issue.itemIds.first())
    }

private fun ChargeType.field(): ChargeField =
    when (this) {
        ChargeType.SERVICE -> ChargeField.SERVICE
        ChargeType.TAX -> ChargeField.TAX
        ChargeType.DISCOUNT -> ChargeField.DISCOUNT
    }

private fun Bill.isEmptyDraft() =
    items.isEmpty() &&
        title.isBlank() &&
        service.amountMinor == 0L &&
        tax.amountMinor == 0L &&
        discountMinor == 0L &&
        receiptTotalMinor == null

private fun Bill.withCharge(
    field: ChargeField,
    charge: BillCharge,
): Bill =
    when (field) {
        ChargeField.SERVICE -> copy(service = charge)
        ChargeField.TAX -> copy(tax = charge)
        ChargeField.DISCOUNT -> copy(discountMinor = charge.amountMinor)
        ChargeField.RECEIPT_TOTAL -> copy(receiptTotalMinor = charge.amountMinor.takeIf { it > 0 })
    }

private fun Bill.chargeInputs(): Map<ChargeField, ChargeInput> =
    mapOf(
        ChargeField.SERVICE to service.toInput(),
        ChargeField.TAX to tax.toInput(),
        ChargeField.DISCOUNT to ChargeInput(amount = discountMinor.takeIf { it > 0 }?.toString().orEmpty()),
        ChargeField.RECEIPT_TOTAL to ChargeInput(amount = receiptTotalMinor?.toString().orEmpty()),
    )

private fun BillCharge.toInput() =
    ChargeInput(
        amount = amountMinor.takeIf { it > 0 }?.toString().orEmpty(),
        rate = rateBps?.let { BigDecimal.valueOf(it.toLong(), 2).stripTrailingZeros().toPlainString() }.orEmpty(),
    )

/** "10" or "5,5" percent as basis points (1000, 550); null for anything else, including more than two decimals. */
private fun parseRateBps(text: String): Int? {
    val percent = text.trim().replace(',', '.').toBigDecimalOrNull() ?: return null
    if (percent.signum() < 0 || percent > BigDecimal(100)) return null
    return try {
        percent.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).toInt()
    } catch (e: ArithmeticException) {
        null
    }
}
