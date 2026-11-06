package com.primestrider.kotlinstarter.feature.example.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.primestrider.kotlinstarter.core.designsystem.component.AppButton
import com.primestrider.kotlinstarter.core.designsystem.component.AppTextField
import com.primestrider.kotlinstarter.core.designsystem.component.ButtonVariant
import com.primestrider.kotlinstarter.core.designsystem.component.PasswordTextField
import com.primestrider.kotlinstarter.core.designsystem.theme.AppThemeTokens
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.feature.catalog.impl.R
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold
import com.primestrider.kotlinstarter.core.designsystem.R as DesignR

internal const val SUBMIT_BUTTON_TAG = "form_submit"

/** Validated form: errors come from [FormUiState.errors]; the screen only renders and reports edits. */
@Composable
fun FormExampleScreen(
    state: FormUiState,
    onChange: (FormField, String) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
) {
    ExamplePageScaffold(
        title = R.string.example_form_title,
        subtitle = R.string.example_form_subtitle,
        onBack = onBack,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            AppTextField(
                value = state.values.name,
                onValueChange = { onChange(FormField.NAME, it) },
                label = stringResource(R.string.form_field_name),
                placeholder = stringResource(R.string.form_field_name_placeholder),
                errorText = errorText(state, FormField.NAME),
                imeAction = ImeAction.Next,
            )
            AppTextField(
                value = state.values.email,
                onValueChange = { onChange(FormField.EMAIL, it) },
                label = stringResource(R.string.form_field_email),
                placeholder = stringResource(R.string.form_field_email_placeholder),
                errorText = errorText(state, FormField.EMAIL),
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            )
            AppTextField(
                value = state.values.phone,
                onValueChange = { onChange(FormField.PHONE, it) },
                label = stringResource(R.string.form_field_phone),
                placeholder = stringResource(R.string.form_field_phone_placeholder),
                errorText = errorText(state, FormField.PHONE),
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next,
            )
            PasswordTextField(
                value = state.values.password,
                onValueChange = { onChange(FormField.PASSWORD, it) },
                label = stringResource(R.string.form_field_password),
                errorText = errorText(state, FormField.PASSWORD),
                imeAction = ImeAction.Next,
            )
            AppTextField(
                value = state.values.bio,
                onValueChange = { onChange(FormField.BIO, it) },
                label = stringResource(R.string.form_field_bio),
                placeholder = stringResource(R.string.form_field_bio_placeholder),
                errorText = errorText(state, FormField.BIO),
                supportingText = stringResource(DesignR.string.component_counter, state.values.bio.length, BIO_MAX),
                singleLine = false,
                minLines = 3,
            )
            AppButton(
                text = stringResource(if (state.submitting) R.string.form_submitting else R.string.form_submit),
                onClick = onSubmit,
                loading = state.submitting,
                fullWidth = true,
                modifier = Modifier.testTag(SUBMIT_BUTTON_TAG),
            )
            AppButton(
                text = stringResource(R.string.form_clear),
                onClick = onClear,
                variant = ButtonVariant.OUTLINED,
                fullWidth = true,
            )
            state.submitted?.let { SubmittedCard(it) }
        }
    }
}

@Composable
private fun errorText(
    state: FormUiState,
    field: FormField,
): String? = state.errors[field]?.let { stringResource(it) }

@Composable
private fun SubmittedCard(values: FormValues) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.s4), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = AppThemeTokens.colors.success)
                Text(stringResource(R.string.form_submitted_title), style = MaterialTheme.typography.titleSmall)
            }
            Text(stringResource(R.string.form_submitted_body), style = MaterialTheme.typography.bodyMedium)
            listOf(
                "name" to values.name,
                "email" to values.email,
                "phone" to values.phone,
                "password" to values.password,
                "bio" to values.bio,
            ).forEach { (key, value) ->
                Text(
                    "$key: $value",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun FormExampleScreenPreview() {
    PreviewTheme {
        FormExampleScreen(
            state =
                FormUiState(
                    values = FormValues(name = "Ada Lovelace", email = "ada@", phone = "5551234567"),
                    errors = mapOf(FormField.EMAIL to R.string.form_error_email_invalid),
                    submitAttempted = true,
                ),
            onChange = { _, _ -> },
            onSubmit = {},
            onClear = {},
            onBack = {},
        )
    }
}
