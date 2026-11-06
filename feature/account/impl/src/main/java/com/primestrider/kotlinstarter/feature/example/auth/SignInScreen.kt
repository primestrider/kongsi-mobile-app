package com.primestrider.kotlinstarter.feature.example.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import com.primestrider.kotlinstarter.core.designsystem.component.AppButton
import com.primestrider.kotlinstarter.core.designsystem.component.AppCheckbox
import com.primestrider.kotlinstarter.core.designsystem.component.AppTextField
import com.primestrider.kotlinstarter.core.designsystem.component.Banner
import com.primestrider.kotlinstarter.core.designsystem.component.BannerVariant
import com.primestrider.kotlinstarter.core.designsystem.component.ButtonVariant
import com.primestrider.kotlinstarter.core.designsystem.component.PasswordTextField
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.core.network.userMessage
import com.primestrider.kotlinstarter.feature.account.impl.R
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold

internal const val SIGN_IN_BUTTON_TAG = "sign_in_submit"
internal const val PASSWORD_FIELD_TAG = "sign_in_password"

@Composable
fun SignInScreen(
    state: SignInUiState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onShortLivedChange: (Boolean) -> Unit,
    onFillDemo: () -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
) {
    ExamplePageScaffold(
        title = R.string.auth_sign_in_title,
        subtitle = R.string.auth_sign_in_subtitle,
        onBack = onBack,
    ) {
        DemoCard(onFillDemo)
        state.error?.let { error ->
            Banner(
                title = stringResource(R.string.auth_error_title),
                message = error.userMessage(),
                variant = BannerVariant.ERROR,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            AppTextField(
                value = state.username,
                onValueChange = onUsernameChange,
                label = stringResource(R.string.auth_field_username),
                errorText = state.errors[SignInField.USERNAME]?.let { stringResource(it) },
                imeAction = ImeAction.Next,
            )
            PasswordTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = stringResource(R.string.auth_field_password),
                modifier = Modifier.testTag(PASSWORD_FIELD_TAG),
                errorText = state.errors[SignInField.PASSWORD]?.let { stringResource(it) },
                imeAction = ImeAction.Done,
                onImeAction = onSubmit,
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .toggleable(value = state.shortLived, role = Role.Checkbox, onValueChange = onShortLivedChange),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
            ) {
                AppCheckbox(checked = state.shortLived, onCheckedChange = null)
                Text(stringResource(R.string.auth_short_lived), style = MaterialTheme.typography.bodyMedium)
            }
            AppButton(
                text = stringResource(R.string.auth_sign_in_title),
                onClick = onSubmit,
                loading = state.loading,
                fullWidth = true,
                modifier = Modifier.testTag(SIGN_IN_BUTTON_TAG),
            )
        }
    }
}

@Composable
private fun DemoCard(onFillDemo: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.s4), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            Text(stringResource(R.string.auth_demo_title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.auth_demo_description), style = MaterialTheme.typography.bodyMedium)
            Text(
                "$DEMO_USERNAME / $DEMO_PASSWORD",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            )
            AppButton(
                text = stringResource(R.string.auth_demo_fill),
                onClick = onFillDemo,
                variant = ButtonVariant.OUTLINED,
            )
        }
    }
}

@ThemePreviews
@Composable
private fun SignInScreenPreview() {
    PreviewTheme {
        SignInScreen(
            state = SignInUiState(username = "em", errors = mapOf(SignInField.USERNAME to R.string.auth_validation_username_min)),
            onUsernameChange = {},
            onPasswordChange = {},
            onShortLivedChange = {},
            onFillDemo = {},
            onSubmit = {},
            onBack = {},
        )
    }
}
