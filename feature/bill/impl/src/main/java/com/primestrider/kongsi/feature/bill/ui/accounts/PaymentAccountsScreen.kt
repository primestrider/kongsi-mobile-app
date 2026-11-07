package com.primestrider.kongsi.feature.bill.ui.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.primestrider.kongsi.core.designsystem.component.AppScaffold
import com.primestrider.kongsi.core.designsystem.component.EmptyState
import com.primestrider.kongsi.core.designsystem.theme.PreviewTheme
import com.primestrider.kongsi.core.designsystem.theme.Spacing
import com.primestrider.kongsi.core.designsystem.theme.ThemePreviews
import com.primestrider.kongsi.feature.bill.impl.R
import com.primestrider.kongsi.feature.bill.model.PaymentAccount
import com.primestrider.kongsi.feature.bill.model.PaymentAccountType

/** The bill creator's bank accounts and e-wallets: tap to edit, delete with undo, add with the button. */
@Composable
fun PaymentAccountsScreen(
    state: PaymentAccountsUiState,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onBack: () -> Unit,
) {
    AppScaffold(
        title = stringResource(R.string.bill_accounts_title),
        onBack = onBack,
        floatingActionButton = {
            if (state.accounts.isNotEmpty()) {
                val label = stringResource(R.string.bill_accounts_add)
                ExtendedFloatingActionButton(
                    onClick = onAdd,
                    // The FAB clears its text's semantics; name the button so TalkBack reads it.
                    modifier = Modifier.semantics { contentDescription = label },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(label) },
                )
            }
        },
    ) { padding ->
        when {
            !state.loaded -> Unit
            state.accounts.isEmpty() ->
                EmptyState(
                    title = stringResource(R.string.bill_accounts_empty_title),
                    description = stringResource(R.string.bill_accounts_empty_body),
                    icon = Icons.Filled.AccountBox,
                    actionLabel = stringResource(R.string.bill_accounts_add),
                    onAction = onAdd,
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
            else ->
                LazyColumn(
                    Modifier.fillMaxSize().padding(padding),
                    // Room under the button for the last row.
                    contentPadding = PaddingValues(bottom = 96.dp),
                ) {
                    item {
                        Text(
                            stringResource(R.string.bill_accounts_intro),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = Spacing.s4, vertical = Spacing.s2),
                        )
                    }
                    items(state.accounts, key = { it.id }) { account ->
                        PaymentAccountRow(account, onEdit = { onEdit(account.id) }, onDelete = { onDelete(account.id) })
                        HorizontalDivider(Modifier.padding(horizontal = Spacing.s4))
                    }
                }
        }
    }
}

@Composable
private fun PaymentAccountRow(
    account: PaymentAccount,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.bill_accounts_edit), onClick = onEdit)
            .padding(start = Spacing.s4, top = Spacing.s3, bottom = Spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
    ) {
        Icon(
            if (account.type == PaymentAccountType.BANK) Icons.Filled.AccountBox else Icons.Filled.Phone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2), verticalAlignment = Alignment.CenterVertically) {
                Text(account.provider, style = MaterialTheme.typography.titleMedium)
                if (account.isDefault) {
                    Text(
                        stringResource(R.string.bill_account_default_badge),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Text(account.number, style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.bill_account_holder_line, account.holderName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = stringResource(R.string.bill_accounts_delete_cd, account.provider, account.number),
            )
        }
    }
}

@ThemePreviews
@Composable
private fun PaymentAccountsScreenPreview() {
    PreviewTheme {
        PaymentAccountsScreen(
            state =
                PaymentAccountsUiState(
                    accounts =
                        listOf(
                            PaymentAccount(1, PaymentAccountType.BANK, "BCA", "1234567890", "Ricky", isDefault = true),
                            PaymentAccount(2, PaymentAccountType.EWALLET, "GoPay", "081234567890", "Ricky", isDefault = false),
                        ),
                    loaded = true,
                ),
            onAdd = {},
            onEdit = {},
            onDelete = {},
            onBack = {},
        )
    }
}
