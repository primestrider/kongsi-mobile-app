package com.primestrider.kongsi.feature.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.primestrider.kongsi.core.designsystem.component.AppButton
import com.primestrider.kongsi.core.designsystem.component.AppScaffold
import com.primestrider.kongsi.core.designsystem.component.AppTextField
import com.primestrider.kongsi.core.designsystem.theme.PreviewTheme
import com.primestrider.kongsi.core.designsystem.theme.Spacing
import com.primestrider.kongsi.core.designsystem.theme.ThemePreviews
import com.primestrider.kongsi.feature.profile.impl.R

const val PROFILE_NAME_TAG = "profile_name"
const val PROFILE_PHONE_TAG = "profile_phone"

/** Edits the user's own name (shown as "you" in bills) and mobile number (offered for e-wallet accounts). */
@Composable
fun ProfileEditScreen(
    state: ProfileEditUiState,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    AppScaffold(title = stringResource(R.string.profile_edit_title), onBack = onBack) { padding ->
        if (!state.loaded) return@AppScaffold
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.s4),
            verticalArrangement = Arrangement.spacedBy(Spacing.s4),
        ) {
            AppTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = stringResource(R.string.profile_name),
                supportingText = stringResource(R.string.profile_name_support),
                errorText = state.shownNameError?.let { stringResource(it) },
                maxLength = PROFILE_NAME_MAX,
                imeAction = ImeAction.Next,
                modifier = Modifier.testTag(PROFILE_NAME_TAG),
            )
            AppTextField(
                value = state.phone,
                onValueChange = onPhoneChange,
                label = stringResource(R.string.profile_phone),
                placeholder = stringResource(R.string.profile_phone_hint),
                supportingText = stringResource(R.string.profile_phone_support),
                errorText = state.shownPhoneError?.let { stringResource(it) },
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done,
                onImeAction = onSave,
                modifier = Modifier.testTag(PROFILE_PHONE_TAG),
            )
            AppButton(
                text = stringResource(R.string.profile_save),
                onClick = onSave,
                loading = state.saving,
                fullWidth = true,
            )
        }
    }
}

@ThemePreviews
@Composable
private fun ProfileEditScreenPreview() {
    PreviewTheme {
        ProfileEditScreen(
            state = ProfileEditUiState(loaded = true, name = "Ricky", phone = "0812 3456 7890"),
            onNameChange = {},
            onPhoneChange = {},
            onSave = {},
            onBack = {},
        )
    }
}
