package com.primestrider.kongsi.feature.bill.ui.summary

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.primestrider.kongsi.core.designsystem.component.AppBottomSheet
import com.primestrider.kongsi.core.designsystem.component.AppButton
import com.primestrider.kongsi.core.designsystem.component.AppScaffold
import com.primestrider.kongsi.core.designsystem.component.ButtonVariant
import com.primestrider.kongsi.core.designsystem.theme.PreviewTheme
import com.primestrider.kongsi.core.designsystem.theme.Radii
import com.primestrider.kongsi.core.designsystem.theme.Spacing
import com.primestrider.kongsi.core.designsystem.theme.ThemePreviews
import com.primestrider.kongsi.core.util.currentLocale
import com.primestrider.kongsi.core.util.formatMoney
import com.primestrider.kongsi.core.util.formatPercent
import com.primestrider.kongsi.feature.bill.impl.R
import com.primestrider.kongsi.feature.bill.model.BreakdownLine
import com.primestrider.kongsi.feature.bill.model.DEFAULT_CURRENCY
import com.primestrider.kongsi.feature.bill.model.PaymentAccount
import com.primestrider.kongsi.feature.bill.model.PaymentAccountType
import com.primestrider.kongsi.feature.bill.model.Person
import com.primestrider.kongsi.feature.bill.model.fractionText
import com.primestrider.kongsi.feature.bill.ui.flow.CheckChip
import com.primestrider.kongsi.feature.bill.ui.flow.FlowStep
import com.primestrider.kongsi.feature.bill.ui.flow.PersonAvatar
import com.primestrider.kongsi.feature.bill.ui.flow.RunningTotal
import com.primestrider.kongsi.feature.bill.ui.flow.RunningTotalBar
import com.primestrider.kongsi.feature.bill.ui.flow.StepIndicator
import com.primestrider.kongsi.feature.bill.ui.people.ChoiceRow
import com.primestrider.kongsi.feature.bill.ui.people.ContactSearchField
import com.primestrider.kongsi.feature.bill.ui.people.NameAndNumber
import com.primestrider.kongsi.feature.bill.ui.people.PersonRow
import com.primestrider.kongsi.feature.bill.ui.people.PickPhoneNumber
import com.primestrider.kongsi.feature.bill.ui.people.shownNumber
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

/** Callbacks of [BillSummaryScreen]. */
data class BillSummaryActions(
    val onBack: () -> Unit,
    val onStepClick: (FlowStep) -> Unit,
    val onOpenBreakdown: (Long) -> Unit,
    val onCloseBreakdown: () -> Unit,
    val onToggleAccount: (Long) -> Unit,
    val onAddAccount: () -> Unit,
    /** Sends a person their message, or the group its summary when the id is null. */
    val onSend: (Long?) -> Unit,
    val onAddNumber: (Long) -> Unit,
    val onCloseNumber: () -> Unit,
    val onNumberQueryChange: (String) -> Unit,
    val onContactsAllowed: (Boolean) -> Unit,
    val onPickNumber: (NumberOption) -> Unit,
    val onNumberPickerResult: (Uri?) -> Unit,
    val onDone: () -> Unit,
)

/**
 * Summary: what everyone owes, the payer first and marked. Tapping a person opens "How was this calculated?"; each
 * person can be sent their part on WhatsApp, or given a number first, and the group its summary. The bar confirms that
 * the shares add up to the bill total. [numberQuery] is the "Add number" search text, read straight from the ViewModel.
 */
@Composable
fun BillSummaryScreen(
    state: BillSummaryUiState,
    numberQuery: String,
    actions: BillSummaryActions,
) {
    val picker = rememberLauncherForActivityResult(PickPhoneNumber) { uri -> actions.onNumberPickerResult(uri) }
    AppScaffold(
        title = stringResource(R.string.bill_summary_title),
        onBack = actions.onBack,
        bottomBar = {
            RunningTotalBar(
                if (state.complete) RunningTotal.Shares(state.totalMinor) else RunningTotal.Assigned(state.assignedMinor, state.totalMinor),
                actionLabel = stringResource(R.string.bill_summary_done),
                actionEnabled = state.complete,
                onAction = actions.onDone,
            )
        },
    ) { padding ->
        if (!state.loaded) return@AppScaffold
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = Spacing.s6),
            verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        ) {
            item(key = "steps") { StepIndicator(current = FlowStep.SUMMARY, onStepClick = actions.onStepClick) }
            item(key = "hint") {
                Text(
                    stringResource(R.string.bill_summary_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.s4),
                )
            }
            items(state.people, key = { it.row.person.id }) { person ->
                val id = person.row.person.id
                PersonCard(
                    person = person,
                    canSend = state.complete,
                    onOpen = { actions.onOpenBreakdown(id) },
                    onSend = { actions.onSend(id) },
                    onAddNumber = { actions.onAddNumber(id) },
                )
            }
            item(key = "group") {
                AppButton(
                    text = stringResource(R.string.bill_summary_send_group),
                    onClick = { actions.onSend(null) },
                    variant = ButtonVariant.OUTLINED,
                    enabled = state.complete,
                    fullWidth = true,
                    modifier = Modifier.padding(horizontal = Spacing.s4, vertical = Spacing.s2),
                )
            }
            item(key = "accounts") { Accounts(state.accounts, state.selectedAccountIds, actions) }
        }
    }

    state.people.firstOrNull { it.row.person.id == state.openPersonId }?.let { person ->
        AppBottomSheet(onDismiss = actions.onCloseBreakdown, title = stringResource(R.string.bill_breakdown_title)) {
            Breakdown(person, state.people.associate { it.row.person.id to it.row })
        }
    }

    state.people.firstOrNull { it.row.person.id == state.numberForId }?.let { person ->
        AppBottomSheet(
            onDismiss = actions.onCloseNumber,
            title = stringResource(R.string.bill_summary_number_title, person.row.label),
        ) {
            NumberSearch(state.numberOptions, numberQuery, actions, onPickContact = { picker.launch(Unit) })
        }
    }
}

/** "Add number": the contact search of the People step, offering numbers only, with the system picker below. */
@Composable
private fun NumberSearch(
    options: List<NumberOption>,
    query: String,
    actions: BillSummaryActions,
    onPickContact: () -> Unit,
) {
    ContactSearchField(
        query = query,
        onQueryChange = actions.onNumberQueryChange,
        placeholder = stringResource(R.string.bill_summary_number_search),
        onContactsAllowed = actions.onContactsAllowed,
    )
    Column {
        options.take(MAX_NUMBER_OPTIONS).forEach { option ->
            ChoiceRow(onClick = { actions.onPickNumber(option) }, horizontalPadding = 0.dp) {
                if (option.name == null) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        stringResource(R.string.bill_summary_use_number, shownNumber(option.phone)),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                } else {
                    NameAndNumber(option.name, shownNumber(option.phone), option.whatsApp)
                }
            }
        }
    }
    AppButton(
        text = stringResource(R.string.bill_people_pick_contact),
        onClick = onPickContact,
        variant = ButtonVariant.SECONDARY,
        leadingIcon = Icons.Filled.Person,
    )
}

/** The sheet does not scroll, so it shows the best few matches; typing more narrows them down. */
private const val MAX_NUMBER_OPTIONS = 5

/** The name a person goes by here: "You (Ricky)" for the payer, otherwise their label. */
@Composable
private fun PersonRow.title(): String =
    when {
        !isMe -> label
        person.name.isBlank() -> stringResource(R.string.bill_people_me)
        else -> stringResource(R.string.bill_people_me_named, person.name)
    }

/** How someone else on the bill is named inside a breakdown: by name, so "you" always means the reader. */
@Composable
private fun PersonRow.otherName(): String = if (isMe && person.name.isBlank()) stringResource(R.string.bill_people_me) else label

/** "41,580 rupiah", so TalkBack reads the amount as a number instead of spelling out "R p". */
@Composable
private fun spokenMoney(
    minor: Long,
    locale: Locale,
): String = stringResource(R.string.bill_money_spoken, NumberFormat.getIntegerInstance(locale).format(minor))

/**
 * A person and what they owe; tapping it opens their breakdown. Everyone but the payer can be sent their part, and
 * someone without a number can be given one.
 */
@Composable
private fun PersonCard(
    person: SummaryPerson,
    canSend: Boolean,
    onOpen: () -> Unit,
    onSend: () -> Unit,
    onAddNumber: () -> Unit,
) {
    val locale = currentLocale()
    val row = person.row
    val description =
        if (row.isMe) {
            stringResource(R.string.bill_summary_my_share_cd, spokenMoney(person.totalMinor, locale))
        } else {
            stringResource(R.string.bill_summary_owes_cd, row.label, spokenMoney(person.totalMinor, locale))
        }
    val sheetLabel = stringResource(R.string.bill_breakdown_title)
    Surface(
        shape = RoundedCornerShape(Radii.xl),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.padding(horizontal = Spacing.s4).fillMaxWidth(),
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    // One sentence for TalkBack ("Budi owes 41,580 rupiah") instead of the name, items and amount apart.
                    .clearAndSetSemantics {
                        contentDescription = description
                        role = Role.Button
                        onClick(label = sheetLabel) {
                            onOpen()
                            true
                        }
                    }.clickable(onClick = onOpen)
                    .heightIn(min = 64.dp)
                    .padding(Spacing.s3),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
            ) {
                PersonAvatar(row.person.name.ifBlank { row.title() }, row.index)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.s0_5)) {
                    Text(row.title(), style = MaterialTheme.typography.titleMedium)
                    Text(
                        when {
                            row.isMe -> stringResource(R.string.bill_people_me_pays)
                            else ->
                                person.lines
                                    .filterIsInstance<BreakdownLine.Item>()
                                    .joinToString(", ") { it.name }
                                    .ifEmpty { stringResource(R.string.bill_summary_nothing) }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                // Never truncated, also at large font sizes: the amount is the point of this screen.
                Text(formatMoney(person.totalMinor, DEFAULT_CURRENCY, locale), style = MaterialTheme.typography.titleMedium)
            }
            if (!row.isMe) SendActions(row, canSend, onSend, onAddNumber)
        }
    }
}

/** "Send" (to their chat, or to a chat picked in WhatsApp without a number) and "Add number" when they have none. */
@Composable
private fun SendActions(
    row: PersonRow,
    canSend: Boolean,
    onSend: () -> Unit,
    onAddNumber: () -> Unit,
) {
    val sendDescription = stringResource(R.string.bill_summary_send_cd, row.label)
    val addDescription = stringResource(R.string.bill_summary_add_number_cd, row.label)
    FlowRow(
        Modifier.padding(start = Spacing.s3, end = Spacing.s3, bottom = Spacing.s3),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
    ) {
        AppButton(
            text = stringResource(R.string.bill_summary_send),
            onClick = onSend,
            variant = ButtonVariant.SECONDARY,
            enabled = canSend,
            modifier = Modifier.semantics { contentDescription = sendDescription },
        )
        if (row.person.phone == null) {
            AppButton(
                text = stringResource(R.string.bill_summary_add_number),
                onClick = onAddNumber,
                variant = ButtonVariant.TEXT,
                modifier = Modifier.semantics { contentDescription = addDescription },
            )
        }
    }
}

/** The payer's accounts to list in the message, as chips; "Add account" while none is saved. */
@Composable
private fun Accounts(
    accounts: List<PaymentAccount>,
    selected: Set<Long>,
    actions: BillSummaryActions,
) {
    Column(
        Modifier.padding(start = Spacing.s4, end = Spacing.s4, top = Spacing.s4),
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
    ) {
        Text(
            stringResource(R.string.bill_summary_accounts),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        if (accounts.isEmpty()) {
            Text(
                stringResource(R.string.bill_accounts_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AppButton(stringResource(R.string.bill_accounts_add), onClick = actions.onAddAccount, variant = ButtonVariant.OUTLINED)
        } else {
            Text(
                stringResource(R.string.bill_summary_accounts_support),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                accounts.forEach { account ->
                    CheckChip(
                        text = stringResource(R.string.bill_summary_account_chip, account.provider, account.number),
                        selected = account.id in selected,
                        onClick = { actions.onToggleAccount(account.id) },
                    )
                }
            }
        }
    }
}

/** "How was this calculated?": the person and their total, then one line per item and charge. */
@Composable
private fun Breakdown(
    person: SummaryPerson,
    rows: Map<Long, PersonRow>,
) {
    val locale = currentLocale()
    val money = { minor: Long -> formatMoney(minor, DEFAULT_CURRENCY, locale) }
    Row(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
    ) {
        PersonAvatar(
            person.row.person.name
                .ifBlank { person.row.title() },
            person.row.index,
        )
        Text(person.row.title(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Text(money(person.totalMinor), style = MaterialTheme.typography.titleMedium)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    person.lines.forEach { line ->
        Row(
            Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        ) {
            Text(lineText(line, locale, rows), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                when (line) {
                    is BreakdownLine.Discount -> "−" + money(line.amountMinor)
                    is BreakdownLine.Rounding -> if (line.amountMinor < 0) "−" + money(-line.amountMinor) else money(line.amountMinor)
                    else -> money(line.amountMinor)
                },
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun lineText(
    line: BreakdownLine,
    locale: Locale,
    rows: Map<Long, PersonRow>,
): String {
    val rate = { bps: Int -> formatPercent(bps / BPS_PER_UNIT, locale) }
    return when (line) {
        is BreakdownLine.Item -> {
            val name = if (line.quantity > 1) stringResource(R.string.bill_item_quantity_name, line.quantity, line.name) else line.name
            if (line.sharedWith.isEmpty()) {
                name
            } else {
                stringResource(
                    R.string.bill_breakdown_shared,
                    name,
                    line.sharedWith.mapNotNull { rows[it]?.otherName() }.joinToString(", "),
                    fractionText(line.sharers),
                )
            }
        }
        is BreakdownLine.Service ->
            line.rateBps?.let { stringResource(R.string.bill_breakdown_service_rate, rate(it)) }
                ?: stringResource(R.string.bill_breakdown_service)
        is BreakdownLine.Tax ->
            when {
                line.rateBps != null && line.onService -> stringResource(R.string.bill_breakdown_tax_rate_service, rate(line.rateBps))
                line.rateBps != null -> stringResource(R.string.bill_breakdown_tax_rate, rate(line.rateBps))
                line.onService -> stringResource(R.string.bill_breakdown_tax_service)
                else -> stringResource(R.string.bill_breakdown_tax)
            }
        is BreakdownLine.Discount -> stringResource(R.string.bill_breakdown_discount)
        is BreakdownLine.Rounding -> {
            val amount = formatMoney(abs(line.amountMinor), DEFAULT_CURRENCY, locale)
            if (line.amountMinor > 0) {
                stringResource(R.string.bill_breakdown_rounding_added, amount)
            } else {
                stringResource(R.string.bill_breakdown_rounding_taken, amount)
            }
        }
    }
}

private const val BPS_PER_UNIT = 10_000.0

private val previewPeople =
    listOf(
        PersonRow(Person(1, "Ricky"), 0, "Ricky", isMe = true),
        PersonRow(Person(2, "Budi"), 1, "Budi", isMe = false),
        PersonRow(Person(3, "Ani"), 2, "Ani", isMe = false),
    )

private val previewState =
    BillSummaryUiState(
        loaded = true,
        title = "Sushi",
        people =
            listOf(
                SummaryPerson(previewPeople[0], 0, emptyList()),
                SummaryPerson(
                    previewPeople[1],
                    41_580,
                    listOf(
                        BreakdownLine.Item("Nasi Goreng", 1, emptyList(), 1, 30_000),
                        BreakdownLine.Item("Ocha", 1, listOf(3), 2, 6_000),
                        BreakdownLine.Service(500, 1_800),
                        BreakdownLine.Tax(1_000, onService = true, 3_780),
                    ),
                ),
                SummaryPerson(
                    previewPeople[2],
                    110_880,
                    listOf(
                        BreakdownLine.Item("Steak", 1, emptyList(), 1, 90_000),
                        BreakdownLine.Item("Ocha", 1, listOf(2), 2, 6_000),
                        BreakdownLine.Service(500, 4_800),
                        BreakdownLine.Tax(1_000, onService = true, 10_080),
                    ),
                ),
            ),
        totalMinor = 152_460,
        assignedMinor = 152_460,
        complete = true,
        accounts =
            listOf(
                PaymentAccount(1, PaymentAccountType.BANK, "BCA", "1234567890", "Ricky", isDefault = true),
                PaymentAccount(2, PaymentAccountType.EWALLET, "GoPay", "081234567890", "Ricky", isDefault = false),
            ),
        selectedAccountIds = setOf(1),
    )

private val previewActions = BillSummaryActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})

@ThemePreviews
@Composable
private fun BillSummaryScreenPreview() {
    PreviewTheme { BillSummaryScreen(previewState, "", previewActions) }
}

@ThemePreviews
@Composable
private fun BillSummaryNoAccountsPreview() {
    PreviewTheme { BillSummaryScreen(previewState.copy(accounts = emptyList(), selectedAccountIds = emptySet()), "", previewActions) }
}

@ThemePreviews
@Composable
private fun BillSummaryAddNumberPreview() {
    PreviewTheme {
        BillSummaryScreen(
            previewState.copy(
                numberForId = 3,
                numberOptions =
                    listOf(
                        NumberOption(null, "6285711112222"),
                        NumberOption("Ani Lestari", "6285711112222", whatsApp = true),
                    ),
            ),
            "0857 1111 2222",
            previewActions,
        )
    }
}

@ThemePreviews
@Composable
private fun BillSummaryBreakdownPreview() {
    PreviewTheme { BillSummaryScreen(previewState.copy(openPersonId = 2), "", previewActions) }
}
