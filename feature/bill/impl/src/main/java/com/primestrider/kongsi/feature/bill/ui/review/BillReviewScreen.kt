package com.primestrider.kongsi.feature.bill.ui.review

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.primestrider.kongsi.core.designsystem.component.AppBottomSheet
import com.primestrider.kongsi.core.designsystem.component.AppButton
import com.primestrider.kongsi.core.designsystem.component.AppScaffold
import com.primestrider.kongsi.core.designsystem.component.AppTextField
import com.primestrider.kongsi.core.designsystem.component.Banner
import com.primestrider.kongsi.core.designsystem.component.BannerVariant
import com.primestrider.kongsi.core.designsystem.component.ButtonVariant
import com.primestrider.kongsi.core.designsystem.component.SwitchRow
import com.primestrider.kongsi.core.designsystem.theme.PreviewTheme
import com.primestrider.kongsi.core.designsystem.theme.Radii
import com.primestrider.kongsi.core.designsystem.theme.Spacing
import com.primestrider.kongsi.core.designsystem.theme.ThemePreviews
import com.primestrider.kongsi.core.util.currentLocale
import com.primestrider.kongsi.core.util.formatAmount
import com.primestrider.kongsi.feature.bill.impl.R
import com.primestrider.kongsi.feature.bill.model.BillItem
import com.primestrider.kongsi.feature.bill.model.ChargeType
import com.primestrider.kongsi.feature.bill.model.SplitIssue
import com.primestrider.kongsi.feature.bill.ui.flow.FlowStep
import com.primestrider.kongsi.feature.bill.ui.flow.MoneyField
import com.primestrider.kongsi.feature.bill.ui.flow.RunningTotal
import com.primestrider.kongsi.feature.bill.ui.flow.RunningTotalBar
import com.primestrider.kongsi.feature.bill.ui.flow.StepIndicator
import kotlinx.coroutines.delay

/** Callbacks of [BillReviewScreen]. */
data class BillReviewActions(
    val onBack: () -> Unit,
    val onTitleChange: (String) -> Unit,
    val onChargeChange: (ChargeField, String) -> Unit,
    val onRateChange: (ChargeField, String) -> Unit,
    val onAddItem: () -> Unit,
    val onEditItem: (Long) -> Unit,
    val onEditorChange: ((ItemEditor) -> ItemEditor) -> Unit,
    val onSaveItem: () -> Unit,
    val onDeleteItem: (Long) -> Unit,
    val onDismissEditor: () -> Unit,
    val onWarningClick: (ReviewWarning) -> Unit,
    val onFocusShown: () -> Unit,
    val onNext: () -> Unit,
)

private const val MAX_NAME_LENGTH = 60
private const val MAX_RATE_LENGTH = 5
private const val MAX_QUANTITY_LENGTH = 3

/** How long a warning's target stays highlighted after the list scrolled to it. */
private const val FOCUS_HIGHLIGHT_MS = 1_500L

/**
 * Review, the first step of a bill: its name, items and charges, with the checks from the Split Rules worded as the
 * user would say them. A warning opens what it is about; one that blocks the next step keeps "Next" disabled.
 */
@Composable
fun BillReviewScreen(
    state: BillReviewUiState,
    actions: BillReviewActions,
) {
    val listState = rememberLazyListState()
    // Positions in the list below: title, the warnings, the items header, the items, the add button, then charges.
    val firstItemIndex = 1 + state.warnings.size + 1
    val firstChargeIndex = firstItemIndex + state.items.size + 2
    LaunchedEffect(state.focus) {
        val focus = state.focus ?: return@LaunchedEffect
        val index =
            when (focus) {
                is ReviewFocus.Item -> firstItemIndex + state.items.indexOfFirst { it.id == focus.id }.coerceAtLeast(0)
                is ReviewFocus.Charge -> firstChargeIndex + focus.field.ordinal
            }
        listState.animateScrollToItem(index)
        delay(FOCUS_HIGHLIGHT_MS)
        actions.onFocusShown()
    }

    AppScaffold(
        title = state.title.ifBlank { stringResource(R.string.bill_review_title_new) },
        onBack = actions.onBack,
        bottomBar = {
            RunningTotalBar(
                RunningTotal.Receipt(state.totalMinor, state.receiptMinor),
                actionLabel = stringResource(R.string.bill_review_next),
                actionEnabled = state.canContinue,
                onAction = actions.onNext,
            )
        },
    ) { padding ->
        if (!state.loaded) return@AppScaffold
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            state = listState,
            contentPadding = PaddingValues(bottom = Spacing.s6),
            verticalArrangement = Arrangement.spacedBy(Spacing.s3),
        ) {
            item(key = "title") {
                Column {
                    StepIndicator(current = FlowStep.ITEMS, onStepClick = {})
                    AppTextField(
                        value = state.title,
                        // Kept short without a counter under the field: a name, not a note.
                        onValueChange = { actions.onTitleChange(it.take(MAX_NAME_LENGTH)) },
                        label = stringResource(R.string.bill_review_name),
                        placeholder = stringResource(R.string.bill_review_name_hint),
                        modifier = Modifier.padding(horizontal = Spacing.s4),
                    )
                }
            }
            state.warnings.forEach { warning ->
                item(key = "warning ${warning.issue}") {
                    WarningCard(warning, state, onClick = { actions.onWarningClick(warning) })
                }
            }
            item(key = "items header") { SectionTitle(stringResource(R.string.bill_review_items)) }
            if (state.items.isEmpty()) {
                item(key = "items empty") {
                    Text(
                        stringResource(R.string.bill_review_items_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Spacing.s4),
                    )
                }
            }
            itemsIndexed(state.items, key = { _, item -> "item ${item.id}" }) { _, item ->
                ItemRow(item, highlighted = state.focus == ReviewFocus.Item(item.id), onClick = { actions.onEditItem(item.id) })
            }
            item(key = "add item") {
                AppButton(
                    text = stringResource(R.string.bill_review_add_item),
                    onClick = actions.onAddItem,
                    variant = ButtonVariant.SECONDARY,
                    leadingIcon = Icons.Filled.Add,
                    modifier = Modifier.padding(horizontal = Spacing.s4),
                )
            }
            item(key = "charges header") { SectionTitle(stringResource(R.string.bill_review_charges)) }
            ChargeField.entries.forEach { field ->
                item(key = "charge $field") {
                    ChargeRow(
                        field = field,
                        input = state.charges.getValue(field),
                        highlighted = state.focus == ReviewFocus.Charge(field),
                        onAmountChange = { actions.onChargeChange(field, it) },
                        onRateChange = { actions.onRateChange(field, it) },
                    )
                }
            }
        }
    }

    state.editor?.let { editor ->
        ItemSheet(
            editor = editor,
            onChange = actions.onEditorChange,
            onSave = actions.onSaveItem,
            onDelete = { actions.onDeleteItem(editor.itemId) },
            onDismiss = actions.onDismissEditor,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = Spacing.s4, end = Spacing.s4, top = Spacing.s3).semantics { heading() },
    )
}

@Composable
private fun highlight(on: Boolean): Color {
    val color by animateColorAsState(if (on) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, label = "highlight")
    return color
}

@Composable
private fun ItemRow(
    item: BillItem,
    highlighted: Boolean,
    onClick: () -> Unit,
) {
    val locale = currentLocale()
    Surface(
        shape = RoundedCornerShape(Radii.xl),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.padding(horizontal = Spacing.s4).fillMaxWidth(),
    ) {
        Row(
            Modifier
                .background(highlight(highlighted))
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = Spacing.s4, vertical = Spacing.s3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
                Text(
                    if (item.quantity > 1) stringResource(R.string.bill_item_quantity_name, item.quantity, item.name) else item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!item.serviceable || !item.taxable) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s1)) {
                        if (!item.serviceable) Tag(stringResource(R.string.bill_item_tag_no_service))
                        if (!item.taxable) Tag(stringResource(R.string.bill_item_tag_no_tax))
                    }
                }
            }
            Text(formatAmount(item.totalMinor, locale), style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun Tag(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            Modifier
                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(Radii.full))
                .padding(horizontal = Spacing.s2, vertical = Spacing.s0_5),
    )
}

@Composable
private fun ChargeRow(
    field: ChargeField,
    input: ChargeInput,
    highlighted: Boolean,
    onAmountChange: (String) -> Unit,
    onRateChange: (String) -> Unit,
) {
    Row(
        Modifier
            .padding(horizontal = Spacing.s2)
            .background(highlight(highlighted), RoundedCornerShape(Radii.xl))
            .padding(horizontal = Spacing.s2),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
    ) {
        MoneyField(
            value = input.amount,
            onValueChange = onAmountChange,
            label = stringResource(field.label()),
            supportingText = if (field == ChargeField.RECEIPT_TOTAL) stringResource(R.string.bill_review_receipt_total_support) else null,
            modifier = Modifier.weight(1f),
        )
        if (field.charge == ChargeType.SERVICE || field.charge == ChargeType.TAX) {
            AppTextField(
                value = input.rate,
                onValueChange = { onRateChange(it.take(MAX_RATE_LENGTH)) },
                label = stringResource(R.string.bill_review_rate),
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.width(88.dp),
            )
        }
    }
}

private fun ChargeField.label(): Int =
    when (this) {
        ChargeField.SERVICE -> R.string.bill_review_service
        ChargeField.TAX -> R.string.bill_review_tax
        ChargeField.DISCOUNT -> R.string.bill_review_discount
        ChargeField.RECEIPT_TOTAL -> R.string.bill_review_receipt_total
    }

@Composable
private fun WarningCard(
    warning: ReviewWarning,
    state: BillReviewUiState,
    onClick: () -> Unit,
) {
    val locale = currentLocale()
    val money = { minor: Long -> formatAmount(minor, locale) }
    val (title, message) =
        when (val issue = warning.issue) {
            is SplitIssue.ReceiptTotalMismatch ->
                stringResource(R.string.bill_warning_title_total) to
                    stringResource(R.string.bill_warning_receipt_total, money(issue.calculatedMinor), money(issue.receiptMinor))
            is SplitIssue.RateMismatch -> {
                val field = if (issue.charge == ChargeType.TAX) ChargeField.TAX else ChargeField.SERVICE
                val rate = state.charges.getValue(field).rate + "%"
                if (issue.charge == ChargeType.TAX) {
                    stringResource(R.string.bill_warning_title_tax) to
                        stringResource(R.string.bill_warning_rate_tax, money(issue.printedMinor), rate, money(issue.calculatedMinor))
                } else {
                    stringResource(R.string.bill_warning_title_service) to
                        stringResource(R.string.bill_warning_rate_service, money(issue.printedMinor), rate, money(issue.calculatedMinor))
                }
            }
            is SplitIssue.NoEligibleItem ->
                when (issue.charge) {
                    ChargeType.TAX -> stringResource(R.string.bill_warning_title_tax) to stringResource(R.string.bill_warning_no_taxable)
                    ChargeType.SERVICE ->
                        stringResource(R.string.bill_warning_title_service) to stringResource(R.string.bill_warning_no_serviceable)
                    ChargeType.DISCOUNT ->
                        stringResource(R.string.bill_warning_title_discount) to stringResource(R.string.bill_warning_no_discountable)
                }
            // Not shown on Review: people are added in the next step.
            is SplitIssue.UnassignedItems -> return
        }
    Banner(
        title = title,
        message = message,
        variant = if (warning.blocking) BannerVariant.ERROR else BannerVariant.WARNING,
        modifier =
            Modifier
                .padding(horizontal = Spacing.s4)
                .clickable(role = Role.Button, onClick = onClick),
    )
}

@Composable
private fun ItemSheet(
    editor: ItemEditor,
    onChange: ((ItemEditor) -> ItemEditor) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppBottomSheet(
        onDismiss = onDismiss,
        title = stringResource(if (editor.itemId == 0L) R.string.bill_item_add_title else R.string.bill_item_edit_title),
    ) {
        // Scrolls when the keyboard or a small screen leaves too little room for the whole form.
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.s4)
                .padding(bottom = Spacing.s4),
            verticalArrangement = Arrangement.spacedBy(Spacing.s3),
        ) {
            AppTextField(
                value = editor.name,
                onValueChange = { text -> onChange { it.copy(name = text.take(MAX_NAME_LENGTH), nameError = false) } },
                label = stringResource(R.string.bill_item_name),
                errorText = if (editor.nameError) stringResource(R.string.bill_item_name_error) else null,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                AppTextField(
                    value = editor.quantity,
                    onValueChange = { text ->
                        onChange { it.copy(quantity = text.filter(Char::isDigit).take(MAX_QUANTITY_LENGTH), quantityError = false) }
                    },
                    label = stringResource(R.string.bill_item_quantity),
                    errorText = if (editor.quantityError) stringResource(R.string.bill_item_quantity_error) else null,
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.width(104.dp),
                )
                MoneyField(
                    value = editor.price,
                    onValueChange = { text -> onChange { it.copy(price = text, priceError = false) } },
                    label = stringResource(R.string.bill_item_price),
                    modifier = Modifier.weight(1f),
                )
            }
            if (editor.priceError) {
                Text(
                    stringResource(R.string.bill_item_price_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            SwitchRow(
                label = stringResource(R.string.bill_item_serviceable),
                checked = editor.serviceable,
                onCheckedChange = { checked -> onChange { it.copy(serviceable = checked) } },
            )
            SwitchRow(
                label = stringResource(R.string.bill_item_taxable),
                checked = editor.taxable,
                onCheckedChange = { checked -> onChange { it.copy(taxable = checked) } },
            )
            AppButton(stringResource(R.string.bill_item_save), onClick = onSave, fullWidth = true)
            if (editor.itemId != 0L) {
                AppButton(
                    stringResource(R.string.bill_item_delete),
                    onClick = onDelete,
                    variant = ButtonVariant.TEXT,
                    fullWidth = true,
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun BillReviewScreenPreview() {
    PreviewTheme {
        BillReviewScreen(
            state =
                BillReviewUiState(
                    loaded = true,
                    title = "Makan Malam Sushi",
                    items =
                        listOf(
                            BillItem(1, "Nasi Goreng", 30_000, sharedBy = emptySet()),
                            BillItem(2, "Steak", 90_000, sharedBy = emptySet()),
                            BillItem(3, "Ocha", 12_000, sharedBy = emptySet(), quantity = 2, taxable = false),
                        ),
                    charges =
                        BillReviewUiState().charges +
                            mapOf(
                                ChargeField.SERVICE to ChargeInput("6600", "5"),
                                ChargeField.TAX to ChargeInput("13860", "10"),
                            ),
                    totalMinor = 152_460,
                    receiptMinor = 152_460,
                    warnings =
                        listOf(
                            ReviewWarning(SplitIssue.RateMismatch(ChargeType.TAX, 13_860, 12_600), ReviewFocus.Item(3)),
                        ),
                    canContinue = true,
                ),
            actions = BillReviewActions({}, {}, { _, _ -> }, { _, _ -> }, {}, {}, {}, {}, {}, {}, {}, {}, {}),
        )
    }
}
