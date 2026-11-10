package com.primestrider.kongsi.feature.bill.ui.assign

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.primestrider.kongsi.core.designsystem.component.AppButton
import com.primestrider.kongsi.core.designsystem.component.AppCheckbox
import com.primestrider.kongsi.core.designsystem.component.AppScaffold
import com.primestrider.kongsi.core.designsystem.component.AvatarSize
import com.primestrider.kongsi.core.designsystem.component.Banner
import com.primestrider.kongsi.core.designsystem.component.BannerVariant
import com.primestrider.kongsi.core.designsystem.component.ButtonVariant
import com.primestrider.kongsi.core.designsystem.component.SegmentedChoice
import com.primestrider.kongsi.core.designsystem.theme.PreviewTheme
import com.primestrider.kongsi.core.designsystem.theme.Radii
import com.primestrider.kongsi.core.designsystem.theme.Spacing
import com.primestrider.kongsi.core.designsystem.theme.ThemePreviews
import com.primestrider.kongsi.core.util.currentLocale
import com.primestrider.kongsi.core.util.formatAmount
import com.primestrider.kongsi.feature.bill.impl.R
import com.primestrider.kongsi.feature.bill.model.BillItem
import com.primestrider.kongsi.feature.bill.model.Person
import com.primestrider.kongsi.feature.bill.ui.flow.CheckChip
import com.primestrider.kongsi.feature.bill.ui.flow.FlowStep
import com.primestrider.kongsi.feature.bill.ui.flow.PersonAvatar
import com.primestrider.kongsi.feature.bill.ui.flow.RunningTotal
import com.primestrider.kongsi.feature.bill.ui.flow.RunningTotalBar
import com.primestrider.kongsi.feature.bill.ui.flow.StepIndicator
import com.primestrider.kongsi.feature.bill.ui.people.PersonRow
import kotlinx.coroutines.delay

/** Callbacks of [BillAssignScreen]. */
data class BillAssignActions(
    val onBack: () -> Unit,
    val onStepClick: (FlowStep) -> Unit,
    val onModeChange: (AssignMode) -> Unit,
    val onSelectPerson: (Long) -> Unit,
    val onToggle: (itemId: Long, personId: Long) -> Unit,
    val onToggleEveryone: (Long) -> Unit,
    val onSplitEqually: () -> Unit,
    val onUnassignedClick: () -> Unit,
    val onFocusShown: () -> Unit,
    val onNext: () -> Unit,
)

/** How long the item "not assigned yet" points at stays highlighted after the list scrolled to it. */
private const val FOCUS_HIGHLIGHT_MS = 1_500L

/** List positions before the items: the steps, then the controls, then the "not assigned yet" banner when shown. */
private const val ITEMS_AFTER = 2

/**
 * Assign: who had what. Item-first ticks people on each item; person-first picks a person and ticks their items. The
 * bar shows how much of the bill is assigned, and "Next" waits until every item has someone.
 */
@Composable
fun BillAssignScreen(
    state: BillAssignUiState,
    actions: BillAssignActions,
) {
    val listState = rememberLazyListState()
    val firstItemIndex = ITEMS_AFTER + if (state.unassignedItemIds.isEmpty()) 0 else 1
    LaunchedEffect(state.focusItemId) {
        val focus = state.focusItemId ?: return@LaunchedEffect
        listState.animateScrollToItem(firstItemIndex + state.items.indexOfFirst { it.item.id == focus }.coerceAtLeast(0))
        delay(FOCUS_HIGHLIGHT_MS)
        actions.onFocusShown()
    }

    AppScaffold(
        title = stringResource(R.string.bill_assign_title),
        onBack = actions.onBack,
        bottomBar = {
            RunningTotalBar(
                RunningTotal.Assigned(state.assignedMinor, state.totalMinor),
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
            verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        ) {
            item(key = "steps") { StepIndicator(current = FlowStep.ASSIGN, onStepClick = actions.onStepClick) }
            item(key = "controls") { Controls(state.mode, actions) }
            if (state.unassignedItemIds.isNotEmpty()) {
                item(key = "unassigned") {
                    Banner(
                        title =
                            pluralStringResource(
                                R.plurals.bill_assign_unassigned,
                                state.unassignedItemIds.size,
                                state.unassignedItemIds.size,
                            ),
                        message = stringResource(R.string.bill_assign_unassigned_action),
                        variant = BannerVariant.WARNING,
                        modifier =
                            Modifier
                                .padding(horizontal = Spacing.s4)
                                .clickable(role = Role.Button, onClick = actions.onUnassignedClick),
                    )
                }
            }
            when (state.mode) {
                AssignMode.BY_ITEM ->
                    items(state.items, key = { "item ${it.item.id}" }) { row ->
                        ItemCard(
                            row = row,
                            people = state.people,
                            highlighted = state.focusItemId == row.item.id,
                            onToggle = { personId -> actions.onToggle(row.item.id, personId) },
                            onToggleEveryone = { actions.onToggleEveryone(row.item.id) },
                        )
                    }
                AssignMode.BY_PERSON -> {
                    val selected = state.people.firstOrNull { it.person.id == state.selectedPersonId }
                    item(key = "people") { PersonPicker(state.people, state.selectedPersonId, actions.onSelectPerson) }
                    if (selected != null) {
                        item(key = "question") { Question(selected) }
                        items(state.items, key = { "pick ${it.item.id}" }) { row ->
                            ItemCheckRow(
                                row = row,
                                person = selected,
                                onToggle = { actions.onToggle(row.item.id, selected.person.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Controls(
    mode: AssignMode,
    actions: BillAssignActions,
) {
    Column(Modifier.padding(horizontal = Spacing.s4), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        val byItem = stringResource(R.string.bill_assign_by_item)
        val byPerson = stringResource(R.string.bill_assign_by_person)
        SegmentedChoice(
            options = AssignMode.entries,
            selected = mode,
            onSelect = actions.onModeChange,
            label = { if (it == AssignMode.BY_ITEM) byItem else byPerson },
        )
        AppButton(
            text = stringResource(R.string.bill_assign_split_equally),
            onClick = actions.onSplitEqually,
            variant = ButtonVariant.TEXT,
        )
    }
}

/** The name a person goes by on this step: "You" for the payer, otherwise their label. */
@Composable
private fun PersonRow.shownName(): String = if (isMe) stringResource(R.string.bill_people_me) else label

@Composable
private fun highlight(on: Boolean): Color {
    val color by animateColorAsState(if (on) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, label = "highlight")
    return color
}

@Composable
private fun ItemTitle(
    item: BillItem,
    modifier: Modifier = Modifier,
) {
    val locale = currentLocale()
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s3)) {
        Text(
            if (item.quantity > 1) stringResource(R.string.bill_item_quantity_name, item.quantity, item.name) else item.name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(formatAmount(item.totalMinor, locale), style = MaterialTheme.typography.titleSmall)
    }
}

/** Item-first: the item with a chip per person and "Everyone". */
@Composable
private fun ItemCard(
    row: AssignItemRow,
    people: List<PersonRow>,
    highlighted: Boolean,
    onToggle: (Long) -> Unit,
    onToggleEveryone: () -> Unit,
) {
    val sharers = row.sharers.map { it.person.id }.toSet()
    Surface(
        shape = RoundedCornerShape(Radii.xl),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.padding(horizontal = Spacing.s4).fillMaxWidth(),
    ) {
        Column(
            Modifier.background(highlight(highlighted)).padding(Spacing.s3),
            verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        ) {
            ItemTitle(row.item, Modifier.padding(horizontal = Spacing.s1))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                CheckChip(
                    text = stringResource(R.string.bill_assign_everyone),
                    selected = people.isNotEmpty() && sharers.size == people.size,
                    onClick = onToggleEveryone,
                )
                people.forEach { person ->
                    val name = person.shownName()
                    CheckChip(
                        text = name,
                        selected = person.person.id in sharers,
                        onClick = { onToggle(person.person.id) },
                        leading = { PersonAvatar(person.person.name.ifBlank { name }, person.index, size = AvatarSize.SMALL) },
                    )
                }
            }
        }
    }
}

/** Person-first: one chip per person, the chosen one filled. */
@Composable
private fun PersonPicker(
    people: List<PersonRow>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = Spacing.s4),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
    ) {
        people.forEach { row ->
            val selected = row.person.id == selectedId
            val name = row.shownName()
            Surface(
                shape = RoundedCornerShape(Radii.full),
                color = if (selected) scheme.primary else scheme.surface,
                contentColor = if (selected) scheme.onPrimary else scheme.onSurface,
                border = BorderStroke(1.dp, if (selected) scheme.primary else scheme.outlineVariant),
                modifier = Modifier.selectable(selected = selected, role = Role.Tab, onClick = { onSelect(row.person.id) }),
            ) {
                Row(
                    Modifier.heightIn(min = 48.dp).padding(start = Spacing.s1, end = Spacing.s3),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
                ) {
                    PersonAvatar(row.person.name.ifBlank { name }, row.index, size = AvatarSize.SMALL)
                    Text(name, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun Question(person: PersonRow) {
    Text(
        if (person.isMe) {
            stringResource(R.string.bill_assign_me_question)
        } else {
            stringResource(R.string.bill_assign_person_question, person.label)
        },
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = Spacing.s4, end = Spacing.s4, top = Spacing.s2).semantics { heading() },
    )
}

/** Person-first: an item to tick for [person], with who else had it. */
@Composable
private fun ItemCheckRow(
    row: AssignItemRow,
    person: PersonRow,
    onToggle: () -> Unit,
) {
    val checked = row.sharers.any { it.person.id == person.person.id }
    val others = row.sharers.filterNot { it.person.id == person.person.id }
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(horizontal = Spacing.s4, vertical = Spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
    ) {
        AppCheckbox(checked = checked, onCheckedChange = null)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.s0_5)) {
            ItemTitle(row.item)
            val names = others.map { it.shownName() }
            Text(
                when {
                    names.isNotEmpty() -> stringResource(R.string.bill_assign_shared_with, names.joinToString(", "))
                    !checked -> stringResource(R.string.bill_assign_nobody)
                    else -> ""
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private val previewPeople =
    listOf(
        PersonRow(Person(1, "Ricky", "6281111222233"), 0, "Ricky", isMe = true),
        PersonRow(Person(2, "Budi", "6281234567890"), 1, "Budi", isMe = false),
        PersonRow(Person(3, "Ani", null), 2, "Ani", isMe = false),
    )

private val previewState =
    BillAssignUiState(
        loaded = true,
        title = "Sushi",
        people = previewPeople,
        items =
            listOf(
                AssignItemRow(BillItem(10, "Nasi Goreng", 30_000, setOf(2)), listOf(previewPeople[1])),
                AssignItemRow(BillItem(11, "Steak", 90_000, emptySet()), emptyList()),
                AssignItemRow(BillItem(12, "Ocha", 12_000, setOf(2, 3), quantity = 3), previewPeople.drop(1)),
            ),
        selectedPersonId = 2,
        assignedMinor = 50_400,
        totalMinor = 152_460,
        unassignedItemIds = listOf(11),
    )

private val previewActions = BillAssignActions({}, {}, {}, {}, { _, _ -> }, {}, {}, {}, {}, {})

@ThemePreviews
@Composable
private fun BillAssignScreenPreview() {
    PreviewTheme { BillAssignScreen(previewState, previewActions) }
}

@ThemePreviews
@Composable
private fun BillAssignByPersonPreview() {
    PreviewTheme { BillAssignScreen(previewState.copy(mode = AssignMode.BY_PERSON), previewActions) }
}
