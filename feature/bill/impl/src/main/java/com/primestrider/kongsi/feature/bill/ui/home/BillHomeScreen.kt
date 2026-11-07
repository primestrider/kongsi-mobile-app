package com.primestrider.kongsi.feature.bill.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.primestrider.kongsi.core.designsystem.component.AppButton
import com.primestrider.kongsi.core.designsystem.component.AppScaffold
import com.primestrider.kongsi.core.designsystem.component.ButtonVariant
import com.primestrider.kongsi.core.designsystem.theme.PreviewTheme
import com.primestrider.kongsi.core.designsystem.theme.Radii
import com.primestrider.kongsi.core.designsystem.theme.Spacing
import com.primestrider.kongsi.core.designsystem.theme.ThemePreviews
import com.primestrider.kongsi.feature.bill.impl.R

/**
 * Kongsi's Home: the bill history (empty until bills exist; "New bill" lives in the navigation bar). While empty it
 * explains the three steps of a split. Until a payment account is saved, a card suggests adding one (accounts also live in Profile).
 */
@Composable
fun BillHomeScreen(
    state: BillHomeUiState,
    onOpenPaymentAccounts: () -> Unit,
) {
    AppScaffold(title = stringResource(R.string.bill_home_title)) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = Spacing.s4, end = Spacing.s4, top = Spacing.s2, bottom = Spacing.s6),
            verticalArrangement = Arrangement.spacedBy(Spacing.s6),
        ) {
            if (state.showAccountHint) AccountHintCard(onOpenPaymentAccounts)
            EmptyHistory()
        }
    }
}

@Composable
private fun AccountHintCard(onAdd: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(Radii.xl),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth(),
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
        BillHomeScreen(state = BillHomeUiState(showAccountHint = true), onOpenPaymentAccounts = {})
    }
}
