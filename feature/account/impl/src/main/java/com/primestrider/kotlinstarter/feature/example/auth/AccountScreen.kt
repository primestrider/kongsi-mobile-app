package com.primestrider.kotlinstarter.feature.example.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.primestrider.kotlinstarter.core.auth.AuthTokens
import com.primestrider.kotlinstarter.core.auth.AuthUser
import com.primestrider.kotlinstarter.core.auth.Session
import com.primestrider.kotlinstarter.core.designsystem.component.AppButton
import com.primestrider.kotlinstarter.core.designsystem.component.AppScaffold
import com.primestrider.kotlinstarter.core.designsystem.component.Avatar
import com.primestrider.kotlinstarter.core.designsystem.component.AvatarSize
import com.primestrider.kotlinstarter.core.designsystem.component.Banner
import com.primestrider.kotlinstarter.core.designsystem.component.BannerVariant
import com.primestrider.kotlinstarter.core.designsystem.component.ButtonVariant
import com.primestrider.kotlinstarter.core.designsystem.component.ConfirmDialog
import com.primestrider.kotlinstarter.core.designsystem.component.InfoRow
import com.primestrider.kotlinstarter.core.designsystem.component.SectionHeader
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.core.network.userMessage
import com.primestrider.kotlinstarter.feature.account.impl.R

/** Page only a signed-in session can reach. [profile] is the result of the authenticated `auth/me` call. */
@Composable
fun AccountScreen(
    session: Session,
    profile: ProfileLoad,
    onLoadProfile: () -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit,
) {
    var confirming by rememberSaveable { mutableStateOf(false) }
    AppScaffold(title = stringResource(R.string.auth_account_title), onBack = onBack) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.s4),
            verticalArrangement = Arrangement.spacedBy(Spacing.s4),
        ) {
            Text(
                stringResource(R.string.auth_account_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.s2),
            ) {
                Avatar(session.user.fullName, size = AvatarSize.XLARGE, imageUrl = session.user.image)
                Text(session.user.fullName, style = MaterialTheme.typography.titleLarge)
                Text(
                    session.user.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SectionHeader(stringResource(R.string.auth_server_profile))
            AppButton(
                text = stringResource(R.string.auth_load_profile),
                onClick = onLoadProfile,
                loading = profile is ProfileLoad.Loading,
                variant = ButtonVariant.OUTLINED,
            )
            ProfileResult(profile)
            AppButton(
                text = stringResource(R.string.auth_sign_out),
                onClick = { confirming = true },
                variant = ButtonVariant.DESTRUCTIVE,
                fullWidth = true,
            )
        }
    }
    if (confirming) {
        ConfirmDialog(
            title = stringResource(R.string.auth_sign_out_confirm_title),
            message = stringResource(R.string.auth_sign_out_confirm_body),
            confirmLabel = stringResource(R.string.auth_sign_out),
            onConfirm = {
                confirming = false
                onSignOut()
            },
            onDismiss = { confirming = false },
            destructive = true,
        )
    }
}

@Composable
private fun ProfileResult(profile: ProfileLoad) {
    when (profile) {
        is ProfileLoad.Loaded -> {
            InfoRow(stringResource(R.string.auth_info_id), profile.user.id.toString())
            InfoRow(stringResource(R.string.auth_field_username), profile.user.username)
            InfoRow(stringResource(R.string.auth_info_email), profile.user.email)
        }
        is ProfileLoad.Failed ->
            Banner(
                title = stringResource(R.string.auth_profile_load_error),
                message = profile.error.userMessage(),
                variant = BannerVariant.ERROR,
            )
        ProfileLoad.Idle, ProfileLoad.Loading -> Unit
    }
}

@ThemePreviews
@Composable
private fun AccountScreenPreview() {
    PreviewTheme {
        AccountScreen(
            session =
                Session(
                    user = AuthUser(1, "emilys", "emily.johnson@example.com", "Emily", "Johnson"),
                    tokens = AuthTokens("access", "refresh"),
                ),
            profile = ProfileLoad.Loaded(UserResponse(1, "emilys", "emily.johnson@example.com", "Emily", "Johnson")),
            onLoadProfile = {},
            onSignOut = {},
            onBack = {},
        )
    }
}
