package com.primestrider.kongsi.feature.bill.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.primestrider.kongsi.core.designsystem.component.AppBottomSheet
import com.primestrider.kongsi.core.designsystem.component.AppButton
import com.primestrider.kongsi.core.designsystem.component.AppScaffold
import com.primestrider.kongsi.core.designsystem.component.AppTextField
import com.primestrider.kongsi.core.designsystem.component.ButtonVariant
import com.primestrider.kongsi.core.designsystem.theme.PreviewTheme
import com.primestrider.kongsi.core.designsystem.theme.Radii
import com.primestrider.kongsi.core.designsystem.theme.Spacing
import com.primestrider.kongsi.core.designsystem.theme.ThemePreviews
import com.primestrider.kongsi.core.util.currentLocale
import com.primestrider.kongsi.core.util.formatDate
import com.primestrider.kongsi.core.util.formatMoney
import com.primestrider.kongsi.feature.bill.impl.R
import com.primestrider.kongsi.feature.bill.model.DEFAULT_CURRENCY

/**
 * Kongsi's Home: the bill history, newest first ("New bill" lives in the navigation bar). While empty it explains the
 * three steps of a split. Until a payment account is saved, a card suggests adding one (accounts also live in
 * Profile). Each bill's menu renames, duplicates or deletes it; deleting happens at once and the ViewModel offers Undo.
 */
@Composable
fun BillHomeScreen(
    state: BillHomeUiState,
    onOpenPaymentAccounts: () -> Unit,
    onDeleteBill: (Long) -> Unit,
    onOpenBill: (Long) -> Unit,
    onRenameBill: (id: Long, title: String) -> Unit,
    onDuplicateBill: (Long) -> Unit,
) {
    var renaming by rememberSaveable { mutableStateOf<Long?>(null) }
    AppScaffold(title = stringResource(R.string.bill_home_title)) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = Spacing.s4, end = Spacing.s4, top = Spacing.s2, bottom = Spacing.s6),
            verticalArrangement = Arrangement.spacedBy(Spacing.s3),
        ) {
            if (state.showAccountHint) {
                item(key = "hint") { AccountHintCard(onOpenPaymentAccounts, Modifier.padding(bottom = Spacing.s3)) }
            }
            when {
                !state.loaded -> Unit
                state.bills.isEmpty() -> item(key = "empty") { EmptyHistory() }
                else -> {
                    item(key = "header") {
                        Text(
                            stringResource(R.string.bill_home_history_title),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.semantics { heading() },
                        )
                    }
                    items(state.bills, key = { it.id }) { bill ->
                        BillCard(
                            bill,
                            onOpen = { onOpenBill(bill.id) },
                            onRename = { renaming = bill.id },
                            onDuplicate = { onDuplicateBill(bill.id) },
                            onDelete = { onDeleteBill(bill.id) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }

    state.bills.firstOrNull { it.id == renaming }?.let { bill ->
        RenameSheet(
            current = bill.title,
            onSave = { title ->
                onRenameBill(bill.id, title)
                renaming = null
            },
            onDismiss = { renaming = null },
        )
    }
}

/** A new name for a bill, starting from the current one. */
@Composable
private fun RenameSheet(
    current: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by rememberSaveable { mutableStateOf(current) }
    AppBottomSheet(onDismiss = onDismiss, title = stringResource(R.string.bill_home_rename_title)) {
        AppTextField(
            value = title,
            onValueChange = { title = it.take(MAX_TITLE_LENGTH) },
            label = stringResource(R.string.bill_home_rename_label),
        )
        AppButton(text = stringResource(R.string.bill_account_save), onClick = { onSave(title) }, fullWidth = true)
    }
}

/** Long enough for any bill name, short enough to stay readable in the history. */
private const val MAX_TITLE_LENGTH = 60

/** "1 of 2 paid", or "All paid" once everyone who owes has paid; nothing for a bill no one owes on. */
@Composable
private fun paidText(bill: BillRow): String? =
    when {
        bill.owingCount == 0 -> null
        bill.paidCount >= bill.owingCount -> stringResource(R.string.bill_home_all_paid)
        else -> stringResource(R.string.bill_home_paid_count, bill.paidCount, bill.owingCount)
    }

@Composable
private fun BillCard(
    bill: BillRow,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = currentLocale()
    val title = bill.title.ifBlank { stringResource(R.string.bill_home_untitled) }
    Surface(
        shape = RoundedCornerShape(Radii.xl),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .clickable(role = Role.Button, onClick = onOpen)
                .padding(start = Spacing.s4, top = Spacing.s3, bottom = Spacing.s3, end = Spacing.s1),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.s0_5)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(
                        formatDate(bill.createdAt, locale),
                        pluralStringResource(R.plurals.bill_home_people, bill.peopleCount, bill.peopleCount),
                        paidText(bill),
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                formatMoney(bill.totalMinor, DEFAULT_CURRENCY, locale),
                style = MaterialTheme.typography.titleMedium,
            )
            BillMenu(title, onRename, onDuplicate, onDelete)
        }
    }
}

/** A bill's "⋮" menu: Rename, Duplicate, Delete. */
@Composable
private fun BillMenu(
    title: String,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.bill_home_more_cd, title),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            listOf(
                Triple(R.string.bill_home_rename, Icons.Filled.Edit, onRename),
                Triple(R.string.bill_home_duplicate, Icons.Filled.Add, onDuplicate),
                Triple(R.string.bill_home_delete, Icons.Filled.Delete, onDelete),
            ).forEach { (label, icon, action) ->
                DropdownMenuItem(
                    text = { Text(stringResource(label)) },
                    leadingIcon = { Icon(icon, contentDescription = null) },
                    onClick = {
                        open = false
                        action()
                    },
                )
            }
        }
    }
}

@Composable
private fun AccountHintCard(
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(Radii.xl),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Spacing.s4), verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
            Text(stringResource(R.string.bill_home_account_hint_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.bill_home_account_hint_body), style = MaterialTheme.typography.bodyMedium)
            Box(Modifier.padding(top = Spacing.s2)) {
                AppButton(
                    text = stringResource(R.string.bill_home_account_hint_action),
                    onClick = onAdd,
                    variant = ButtonVariant.PRIMARY,
                    leadingIcon = Icons.Filled.Add,
                )
            }
        }
    }
}

@Composable
private fun EmptyHistory() {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
            Text(
                stringResource(R.string.bill_home_empty_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(R.string.bill_home_empty_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            shape = RoundedCornerShape(Radii.xl),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(vertical = Spacing.s2)) {
                Text(
                    stringResource(R.string.bill_home_steps_title).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier
                            .padding(horizontal = Spacing.s4, vertical = Spacing.s2)
                            .semantics { heading() },
                )
                val steps =
                    listOf(
                        R.string.bill_home_step_capture,
                        R.string.bill_home_step_assign,
                        R.string.bill_home_step_send,
                    )
                steps.forEachIndexed { index, step ->
                    if (index >
                        0
                    ) {
                        HorizontalDivider(Modifier.padding(start = Spacing.s16), color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    StepRow(number = index + 1, text = stringResource(step))
                }
            }
        }
    }
}

@Composable
private fun StepRow(
    number: Int,
    text: String,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.s4, vertical = Spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s4),
    ) {
        // The list order already says which step it is; the number is decoration for TalkBack.
        Box(
            Modifier
                .size(Spacing.s8)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "$number",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}

@ThemePreviews
@Composable
private fun BillHomeScreenPreview() {
    PreviewTheme {
        BillHomeScreen(
            state =
                BillHomeUiState(
                    loaded = true,
                    showAccountHint = true,
                    bills =
                        listOf(
                            BillRow(1, "Makan Malam Sushi", createdAt = 1_793_986_200_000, totalMinor = 152_460, peopleCount = 3),
                            BillRow(2, "", createdAt = 1_793_900_000_000, totalMinor = 30_000, peopleCount = 1),
                        ),
                ),
            onOpenPaymentAccounts = {},
            onDeleteBill = {},
            onRenameBill = { _, _ -> },
            onDuplicateBill = {},
            onOpenBill = {},
        )
    }
}

@ThemePreviews
@Composable
private fun BillHomeEmptyPreview() {
    PreviewTheme {
        BillHomeScreen(state = BillHomeUiState(loaded = true, showAccountHint = true), onOpenPaymentAccounts = {
        }, onDeleteBill = {}, onOpenBill = {}, onRenameBill = { _, _ -> }, onDuplicateBill = {})
    }
}
