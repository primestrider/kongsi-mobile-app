package com.primestrider.kotlinstarter.feature.example.sdk.sqlite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.primestrider.kotlinstarter.core.designsystem.component.AppButton
import com.primestrider.kotlinstarter.core.designsystem.component.ButtonVariant
import com.primestrider.kotlinstarter.core.designsystem.component.SegmentedChoice
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.core.util.currentLocale
import com.primestrider.kotlinstarter.core.util.formatCurrency
import com.primestrider.kotlinstarter.feature.devices.impl.R
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold
import com.primestrider.kotlinstarter.feature.example.sdk.SdkSection
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val AMOUNTS = listOf(10_000L, 50_000L, 100_000L)

/** Offline transaction history in Room: pick a kind and amount, add, and watch the balance. */
@Composable
fun TransactionsScreen(
    onBack: () -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TransactionsContent(
        state = state,
        onKind = viewModel::selectKind,
        onAmount = viewModel::selectAmount,
        onAdd = viewModel::add,
        onClear = viewModel::clear,
        onBack = onBack,
    )
}

@Composable
internal fun TransactionsContent(
    state: TransactionsUiState,
    onKind: (TransactionKind) -> Unit,
    onAmount: (Long) -> Unit,
    onAdd: () -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
) {
    val locale = currentLocale()
    val kindLabels = TransactionKind.entries.associateWith { stringResource(it.label()) }
    ExamplePageScaffold(title = R.string.sdk_sqlite_title, subtitle = R.string.sdk_sqlite_subtitle, onBack = onBack) {
        SdkSection(R.string.sdk_sqlite_history_title, R.string.sdk_sqlite_history_description) {
            SegmentedChoice(
                options = TransactionKind.entries,
                selected = state.kind,
                onSelect = onKind,
                label = { kindLabels.getValue(it) },
            )
            SegmentedChoice(
                options = AMOUNTS,
                selected = state.amount,
                onSelect = onAmount,
                label = { formatCurrency(it.toDouble(), "IDR", locale, fractionDigits = 0) },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                AppButton(stringResource(R.string.sdk_sqlite_add), onClick = onAdd)
                AppButton(stringResource(R.string.sdk_sqlite_clear), onClick = onClear, variant = ButtonVariant.TEXT)
            }
            Text(
                stringResource(R.string.sdk_sqlite_balance, formatCurrency(state.balance.toDouble(), "IDR", locale, fractionDigits = 0)),
                style = MaterialTheme.typography.titleMedium,
            )
            if (state.loaded && state.items.isEmpty()) {
                Text(
                    stringResource(R.string.sdk_sqlite_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                TransactionList(state.items, locale, kindLabels)
            }
        }
    }
}

@Composable
private fun TransactionList(
    items: List<TransactionEntity>,
    locale: Locale,
    kindLabels: Map<TransactionKind, String>,
) {
    if (items.isEmpty()) return
    val format = SimpleDateFormat("dd MMM, HH:mm", locale)
    OutlinedCard(Modifier.fillMaxWidth()) {
        items.forEachIndexed { index, item ->
            if (index > 0) HorizontalDivider()
            val kind = TransactionKind.entries.firstOrNull { it.name == item.kind }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Spacing.s4, vertical = Spacing.s3),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "${kindLabels[kind] ?: item.kind} · ${format.format(Date(item.createdAt))}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(signed(item.amount, locale), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun signed(
    amount: Long,
    locale: Locale,
): String = (if (amount > 0) "+" else "") + formatCurrency(amount.toDouble(), "IDR", locale, fractionDigits = 0)

private fun TransactionKind.label(): Int =
    when (this) {
        TransactionKind.TOP_UP -> R.string.sdk_sqlite_kind_top_up
        TransactionKind.PAYMENT -> R.string.sdk_sqlite_kind_payment
        TransactionKind.TRANSFER -> R.string.sdk_sqlite_kind_transfer
    }

@ThemePreviews
@Composable
private fun TransactionsScreenPreview() {
    PreviewTheme {
        TransactionsContent(
            state =
                TransactionsUiState(
                    items =
                        listOf(
                            TransactionEntity(1, "TOP_UP", 50_000, 1_700_000_000_000L),
                            TransactionEntity(2, "PAYMENT", -12_000, 1_700_000_100_000L),
                        ),
                    balance = 38_000,
                    loaded = true,
                ),
            onKind = {},
            onAmount = {},
            onAdd = {},
            onClear = {},
            onBack = {},
        )
    }
}
