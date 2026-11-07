package com.primestrider.kongsi.feature.bill.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.primestrider.kongsi.core.designsystem.component.AppButton
import com.primestrider.kongsi.core.designsystem.component.AppScaffold
import com.primestrider.kongsi.core.designsystem.component.AppTextField
import com.primestrider.kongsi.core.designsystem.component.ButtonVariant
import com.primestrider.kongsi.core.designsystem.component.SegmentedChoice
import com.primestrider.kongsi.core.designsystem.component.SelectField
import com.primestrider.kongsi.core.designsystem.component.SelectOption
import com.primestrider.kongsi.core.designsystem.component.SwitchRow
import com.primestrider.kongsi.core.designsystem.theme.PreviewTheme
import com.primestrider.kongsi.core.designsystem.theme.Radii
import com.primestrider.kongsi.core.designsystem.theme.Spacing
import com.primestrider.kongsi.core.designsystem.theme.ThemePreviews
import com.primestrider.kongsi.feature.bill.impl.R
import com.primestrider.kongsi.feature.bill.model.HOLDER_NAME_MAX
import com.primestrider.kongsi.feature.bill.model.PROVIDER_NAME_MAX
import com.primestrider.kongsi.feature.bill.model.PaymentAccountType
import com.primestrider.kongsi.feature.bill.model.PaymentProviders

const val ACCOUNT_NUMBER_TAG = "account_number"
const val ACCOUNT_HOLDER_TAG = "account_holder"
const val ACCOUNT_OTHER_PROVIDER_TAG = "account_other_provider"

/** Callbacks of [PaymentAccountEditScreen], grouped so the screen signature stays readable. */
data class PaymentAccountEditActions(
    val onTypeChange: (PaymentAccountType) -> Unit,
    val onProviderChange: (String) -> Unit,
    val onOtherProviderChange: (String) -> Unit,
    val onNumberChange: (String) -> Unit,
    val onUseMyNumber: () -> Unit,
    val onHolderNameChange: (String) -> Unit,
    val onDefaultChange: (Boolean) -> Unit,
    val onSave: () -> Unit,
    val onBack: () -> Unit,
)

/**
 * Adds or edits a bank account or e-wallet. A preview at the bottom shows how it will appear in the WhatsApp
 * message, so the user sees what friends will get.
 */
@Composable
fun PaymentAccountEditScreen(
    state: PaymentAccountEditUiState,
    actions: PaymentAccountEditActions,
) {
    val isBank = state.type == PaymentAccountType.BANK
    AppScaffold(
        title = stringResource(if (state.isNew) R.string.bill_account_add_title else R.string.bill_account_edit_title),
        onBack = actions.onBack,
    ) { padding ->
        if (!state.loaded) return@AppScaffold
        val errors = state.shownErrors
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.s4),
            verticalArrangement = Arrangement.spacedBy(Spacing.s4),
        ) {
            val typeLabels =
                mapOf(
                    PaymentAccountType.BANK to stringResource(R.string.bill_account_type_bank),
                    PaymentAccountType.EWALLET to stringResource(R.string.bill_account_type_ewallet),
                )
            SegmentedChoice(
                options = PaymentAccountType.entries,
                selected = state.type,
                onSelect = actions.onTypeChange,
                label = { typeLabels.getValue(it) },
            )
            val otherLabel = stringResource(R.string.bill_account_provider_other)
            SelectField(
                label = stringResource(if (isBank) R.string.bill_account_bank else R.string.bill_account_ewallet),
                options =
                    PaymentProviders.of(state.type).map { SelectOption(it, it) } +
                        SelectOption(OTHER_PROVIDER, otherLabel),
                selected = state.providerChoice,
                onSelect = actions.onProviderChange,
                placeholder = stringResource(R.string.bill_account_provider_placeholder),
                errorText = errors.provider?.takeIf { state.providerChoice != OTHER_PROVIDER }?.let { stringResource(it) },
            )
            if (state.providerChoice == OTHER_PROVIDER) {
                AppTextField(
                    value = state.otherProvider,
                    onValueChange = actions.onOtherProviderChange,
                    label =
                        stringResource(
                            if (isBank) R.string.bill_account_other_bank_name else R.string.bill_account_other_ewallet_name,
                        ),
                    errorText = errors.provider?.let { stringResource(it) },
                    maxLength = PROVIDER_NAME_MAX,
                    imeAction = ImeAction.Next,
                    modifier = Modifier.testTag(ACCOUNT_OTHER_PROVIDER_TAG),
                )
            }
            AppTextField(
                value = state.number,
                onValueChange = actions.onNumberChange,
                label = stringResource(if (isBank) R.string.bill_account_number else R.string.bill_account_phone),
                placeholder = stringResource(if (isBank) R.string.bill_account_number_hint else R.string.bill_account_phone_hint),
                errorText = errors.number?.let { stringResource(it) },
                keyboardType = if (isBank) KeyboardType.Number else KeyboardType.Phone,
                imeAction = ImeAction.Next,
                modifier = Modifier.testTag(ACCOUNT_NUMBER_TAG),
            )
            val myPhone = state.myPhone
            if (!isBank && myPhone != null && myPhone != state.number.trim()) {
                AppButton(
                    text = stringResource(R.string.bill_account_use_my_number, myPhone),
                    onClick = actions.onUseMyNumber,
                    variant = ButtonVariant.TEXT,
                )
            }
            AppTextField(
                value = state.holderName,
                onValueChange = actions.onHolderNameChange,
                label = stringResource(R.string.bill_account_holder),
                supportingText = stringResource(R.string.bill_account_holder_support),
                errorText = errors.holderName?.let { stringResource(it) },
                maxLength = HOLDER_NAME_MAX,
                imeAction = ImeAction.Done,
                onImeAction = actions.onSave,
                modifier = Modifier.testTag(ACCOUNT_HOLDER_TAG),
            )
            Column {
                SwitchRow(
                    label = stringResource(R.string.bill_account_default),
                    checked = state.isDefault,
                    onCheckedChange = actions.onDefaultChange,
                )
                Text(
                    stringResource(R.string.bill_account_default_support),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            MessagePreview(state)
            AppButton(
                text = stringResource(R.string.bill_account_save),
                onClick = actions.onSave,
                loading = state.saving,
                fullWidth = true,
            )
        }
    }
}

/** The account as the WhatsApp message will show it; the number sits on its own line so it is easy to copy. */
@Composable
private fun MessagePreview(state: PaymentAccountEditUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        Text(
            stringResource(R.string.bill_account_preview_title),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.semantics { heading() },
        )
        Surface(
            shape = RoundedCornerShape(Radii.lg),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(),
        ) {
            val provider = state.provider.ifBlank { stringResource(R.string.bill_account_preview_provider) }
            val holder = state.holderName.trim().ifBlank { stringResource(R.string.bill_account_preview_holder) }
            val number = state.number.trim().ifBlank { stringResource(R.string.bill_account_preview_number) }
            Text(
                "$provider ${stringResource(R.string.bill_account_holder_line, holder)}\n$number",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(Spacing.s3),
            )
        }
    }
}

@ThemePreviews
@Composable
private fun PaymentAccountEditScreenPreview() {
    PreviewTheme {
        PaymentAccountEditScreen(
            state =
                PaymentAccountEditUiState(
                    isNew = true,
                    loaded = true,
                    providerChoice = "BCA",
                    number = "1234567890",
                    holderName = "Ricky",
                    isDefault = true,
                ),
            actions = PaymentAccountEditActions({}, {}, {}, {}, {}, {}, {}, {}, {}),
        )
    }
}
