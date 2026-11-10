package com.primestrider.kongsi.feature.bill.ui.flow

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.primestrider.kongsi.core.designsystem.component.AppButton
import com.primestrider.kongsi.core.designsystem.component.AppTextField
import com.primestrider.kongsi.core.designsystem.component.Avatar
import com.primestrider.kongsi.core.designsystem.component.AvatarSize
import com.primestrider.kongsi.core.designsystem.theme.AppThemeTokens
import com.primestrider.kongsi.core.designsystem.theme.PreviewTheme
import com.primestrider.kongsi.core.designsystem.theme.Radii
import com.primestrider.kongsi.core.designsystem.theme.Spacing
import com.primestrider.kongsi.core.designsystem.theme.ThemePreviews
import com.primestrider.kongsi.core.designsystem.theme.personTone
import com.primestrider.kongsi.core.util.currentLocale
import com.primestrider.kongsi.core.util.formatAmount
import com.primestrider.kongsi.feature.bill.impl.R
import java.text.DecimalFormatSymbols

/** The steps of making a bill, in order. */
enum class FlowStep(
    @StringRes val label: Int,
) {
    ITEMS(R.string.bill_step_items),
    PEOPLE(R.string.bill_step_people),
    ASSIGN(R.string.bill_step_assign),
    SUMMARY(R.string.bill_step_summary),
}

/**
 * Where the user is in the flow (Items · People · Assign · Summary). Steps before [current] are done and open again
 * on a tap; later steps are shown but not reachable yet, so the order of the flow stays clear.
 */
@Composable
fun StepIndicator(
    current: FlowStep,
    onStepClick: (FlowStep) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = Spacing.s4, vertical = Spacing.s2),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FlowStep.entries.forEachIndexed { index, step ->
            if (index > 0) {
                HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
            }
            StepChip(
                step = step,
                state =
                    when {
                        step == current -> StepState.CURRENT
                        step.ordinal < current.ordinal -> StepState.DONE
                        else -> StepState.LATER
                    },
                onClick = { onStepClick(step) },
            )
        }
    }
}

private enum class StepState { DONE, CURRENT, LATER }

@Composable
private fun StepChip(
    step: FlowStep,
    state: StepState,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val (container, content) =
        when (state) {
            StepState.CURRENT -> scheme.primary to scheme.onPrimary
            StepState.DONE -> scheme.primaryContainer to scheme.onPrimaryContainer
            StepState.LATER -> scheme.surface to scheme.onSurfaceVariant
        }
    val shape = RoundedCornerShape(Radii.full)
    Surface(
        shape = shape,
        color = container,
        contentColor = content,
        modifier =
            Modifier
                .then(
                    if (state == StepState.DONE) {
                        Modifier.clickable(role = Role.Button, onClick = onClick)
                    } else {
                        Modifier.semantics(mergeDescendants = true) {}
                    },
                ).semantics { if (state == StepState.CURRENT) selected = true },
    ) {
        Text(
            stringResource(step.label),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = Spacing.s3, vertical = Spacing.s1_5),
        )
    }
}

/** What the running-total bar says on each step. */
sealed interface RunningTotal {
    /** Review: the items and charges against the total printed on the receipt, when known. */
    data class Receipt(
        val totalMinor: Long,
        val receiptMinor: Long?,
    ) : RunningTotal

    /** Assign: how much of the bill is assigned to someone so far. */
    data class Assigned(
        val assignedMinor: Long,
        val totalMinor: Long,
    ) : RunningTotal

    /** Summary: every share is known and they add up to the bill total. */
    data class Shares(
        val totalMinor: Long,
    ) : RunningTotal
}

/**
 * The bar at the bottom of every step from Review to Summary, so the user always knows whether things add up, with
 * the step's main action (usually "Next") on the end.
 */
@Composable
fun RunningTotalBar(
    total: RunningTotal,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    actionEnabled: Boolean = true,
    onAction: () -> Unit = {},
) {
    val locale = currentLocale()
    val money = { minor: Long -> formatAmount(minor, locale) }
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
            if (total is RunningTotal.Assigned) {
                LinearProgressIndicator(
                    progress = { if (total.totalMinor == 0L) 0f else total.assignedMinor.toFloat() / total.totalMinor },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(
                Modifier.padding(horizontal = Spacing.s4, vertical = Spacing.s3),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.s0_5)) {
                    when (total) {
                        is RunningTotal.Receipt -> {
                            Text(money(total.totalMinor), style = MaterialTheme.typography.titleLarge)
                            val receipt = total.receiptMinor
                            when {
                                receipt == null -> Unit
                                receipt == total.totalMinor ->
                                    StatusLine(stringResource(R.string.bill_total_matches_receipt), ok = true)
                                else -> StatusLine(stringResource(R.string.bill_total_receipt_says, money(receipt)), ok = false)
                            }
                        }
                        is RunningTotal.Assigned ->
                            Text(
                                stringResource(R.string.bill_total_assigned, money(total.assignedMinor), money(total.totalMinor)),
                                style = MaterialTheme.typography.titleSmall,
                            )
                        is RunningTotal.Shares ->
                            StatusLine(stringResource(R.string.bill_total_shares_add_up, money(total.totalMinor)), ok = true)
                    }
                }
                if (actionLabel != null) AppButton(actionLabel, onClick = onAction, enabled = actionEnabled)
            }
        }
    }
}

@Composable
private fun StatusLine(
    text: String,
    ok: Boolean,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s1)) {
        // The words carry the meaning; the icon and its colour only repeat it.
        Icon(
            if (ok) Icons.Filled.CheckCircle else Icons.Filled.Warning,
            contentDescription = null,
            tint = if (ok) AppThemeTokens.colors.success else AppThemeTokens.colors.warning,
            modifier = Modifier.size(18.dp),
        )
        Text(text, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * A person's avatar in their own tone (by [index], their position on the bill), the same on every screen, so chips on
 * an item read at a glance. TalkBack reads the [name].
 */
@Composable
fun PersonAvatar(
    name: String,
    index: Int,
    modifier: Modifier = Modifier,
    size: AvatarSize = AvatarSize.MEDIUM,
) {
    val tone = personTone(index)
    Avatar(name = name, modifier = modifier, size = size, shape = CircleShape, containerColor = tone.container, contentColor = tone.content)
}

/**
 * A chip that is ticked or not, such as a person on an item or an account on a bill: ticked chips are filled and carry a
 * check mark, so the colour is never the only cue. TalkBack reads it as a checkbox with its [text].
 */
@Composable
fun CheckChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(Radii.full),
        color = if (selected) scheme.secondaryContainer else scheme.surface,
        contentColor = if (selected) scheme.onSecondaryContainer else scheme.onSurface,
        border = BorderStroke(1.dp, if (selected) scheme.secondaryContainer else scheme.outlineVariant),
        modifier = modifier.toggleable(value = selected, role = Role.Checkbox, onValueChange = { onClick() }),
    ) {
        Row(
            Modifier.heightIn(min = 48.dp).padding(start = if (leading == null) Spacing.s3 else Spacing.s1, end = Spacing.s3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
        ) {
            leading?.invoke()
            Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (selected) Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

/**
 * An amount field, without a currency (the app shows plain amounts). [value] is the text as typed: digits are shown
 * grouped ("45.000") while typing, shorthand such as "45rb" or "1,5jt" is accepted and explained below the field
 * ("= 45.000"), and text that is not an amount
 * says how to type one. Read the amount with [parseMoneyInput].
 */
@Composable
fun MoneyField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
) {
    val locale = currentLocale()
    val separator = DecimalFormatSymbols.getInstance(locale).groupingSeparator
    val amount = parseMoneyInput(value)
    AppTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        keyboardType = KeyboardType.Number,
        visualTransformation = ThousandsTransformation(separator),
        supportingText =
            if (amount != null && needsExplanation(value)) {
                "= " + formatAmount(amount, locale)
            } else {
                supportingText
            },
        errorText = if (value.isNotBlank() && amount == null) stringResource(R.string.bill_money_invalid) else null,
    )
}

@ThemePreviews
@Composable
private fun FlowChromePreview() {
    PreviewTheme {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
            StepIndicator(current = FlowStep.ASSIGN, onStepClick = {})
            Row(Modifier.padding(horizontal = Spacing.s4), horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                listOf("Budi", "Ani Lestari", "Citra", "Dimas").forEachIndexed { index, name -> PersonAvatar(name, index) }
            }
            MoneyField(value = "45rb", onValueChange = {}, label = "Price", modifier = Modifier.padding(horizontal = Spacing.s4))
            RunningTotalBar(RunningTotal.Receipt(152_460, receiptMinor = 152_460), actionLabel = "Next")
            RunningTotalBar(RunningTotal.Assigned(96_000, 152_460), actionLabel = "Next")
            RunningTotalBar(RunningTotal.Shares(152_460))
        }
    }
}
