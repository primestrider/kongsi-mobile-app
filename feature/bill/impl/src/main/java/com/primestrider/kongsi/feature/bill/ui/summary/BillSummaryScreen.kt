package com.primestrider.kongsi.feature.bill.ui.summary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.primestrider.kongsi.core.designsystem.component.AppBottomSheet
import com.primestrider.kongsi.core.designsystem.component.AppScaffold
import com.primestrider.kongsi.core.designsystem.theme.PreviewTheme
import com.primestrider.kongsi.core.designsystem.theme.Radii
import com.primestrider.kongsi.core.designsystem.theme.Spacing
import com.primestrider.kongsi.core.designsystem.theme.ThemePreviews
import com.primestrider.kongsi.core.util.currentLocale
import com.primestrider.kongsi.core.util.formatMoney
import com.primestrider.kongsi.core.util.formatPercent
import com.primestrider.kongsi.feature.bill.impl.R
import com.primestrider.kongsi.feature.bill.model.DEFAULT_CURRENCY
import com.primestrider.kongsi.feature.bill.model.Person
import com.primestrider.kongsi.feature.bill.ui.flow.FlowStep
import com.primestrider.kongsi.feature.bill.ui.flow.PersonAvatar
import com.primestrider.kongsi.feature.bill.ui.flow.RunningTotal
import com.primestrider.kongsi.feature.bill.ui.flow.RunningTotalBar
import com.primestrider.kongsi.feature.bill.ui.flow.StepIndicator
import com.primestrider.kongsi.feature.bill.ui.people.PersonRow
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

/** Callbacks of [BillSummaryScreen]. */
data class BillSummaryActions(
    val onBack: () -> Unit,
    val onStepClick: (FlowStep) -> Unit,
    val onOpenBreakdown: (Long) -> Unit,
    val onCloseBreakdown: () -> Unit,
    val onDone: () -> Unit,
)

/**
 * Summary: what everyone owes, the payer first and marked. Tapping a person opens "How was this calculated?"; the bar
 * confirms that the shares add up to the bill total.
 */
@Composable
fun BillSummaryScreen(
    state: BillSummaryUiState,
    actions: BillSummaryActions,
) {
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
                PersonCard(person, onClick = { actions.onOpenBreakdown(person.row.person.id) })
            }
        }
    }

    state.people.firstOrNull { it.row.person.id == state.openPersonId }?.let { person ->
        AppBottomSheet(onDismiss = actions.onCloseBreakdown, title = stringResource(R.string.bill_breakdown_title)) {
            Breakdown(person)
        }
    }
}

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

@Composable
private fun PersonCard(
    person: SummaryPerson,
    onClick: () -> Unit,
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
        modifier =
            Modifier
                .padding(horizontal = Spacing.s4)
                .fillMaxWidth()
                // One sentence for TalkBack ("Budi owes 41,580 rupiah") instead of the name, items and amount apart.
                .clearAndSetSemantics {
                    contentDescription = description
                    role = Role.Button
                    onClick(label = sheetLabel) {
                        onClick()
                        true
                    }
                }.clickable(onClick = onClick),
    ) {
        Row(
            Modifier.heightIn(min = 64.dp).padding(Spacing.s3),
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
    }
}

/** "How was this calculated?": the person and their total, then one line per item and charge. */
@Composable
private fun Breakdown(person: SummaryPerson) {
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
            Text(lineText(line, locale), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
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
                    line.sharedWith.map { it.otherName() }.joinToString(", "),
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

/** One person's part of an item shared by [sharers]: "½", "⅓", … "⅒", then "1/11". */
internal fun fractionText(sharers: Int): String = VULGAR_FRACTIONS.getOrNull(sharers - 2) ?: "1/$sharers"

private val VULGAR_FRACTIONS = listOf("½", "⅓", "¼", "⅕", "⅙", "⅐", "⅛", "⅑", "⅒")

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
                        BreakdownLine.Item("Ocha", 1, listOf(previewPeople[2]), 2, 6_000),
                        BreakdownLine.Service(500, 1_800),
                        BreakdownLine.Tax(1_000, onService = true, 3_780),
                    ),
                ),
                SummaryPerson(
                    previewPeople[2],
                    110_880,
                    listOf(
                        BreakdownLine.Item("Steak", 1, emptyList(), 1, 90_000),
                        BreakdownLine.Item("Ocha", 1, listOf(previewPeople[1]), 2, 6_000),
                        BreakdownLine.Service(500, 4_800),
                        BreakdownLine.Tax(1_000, onService = true, 10_080),
                    ),
                ),
            ),
        totalMinor = 152_460,
        assignedMinor = 152_460,
        complete = true,
    )

private val previewActions = BillSummaryActions({}, {}, {}, {}, {})

@ThemePreviews
@Composable
private fun BillSummaryScreenPreview() {
    PreviewTheme { BillSummaryScreen(previewState, previewActions) }
}

@ThemePreviews
@Composable
private fun BillSummaryBreakdownPreview() {
    PreviewTheme { BillSummaryScreen(previewState.copy(openPersonId = 2), previewActions) }
}
