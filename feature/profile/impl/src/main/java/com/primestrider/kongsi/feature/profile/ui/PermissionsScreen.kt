package com.primestrider.kongsi.feature.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.primestrider.kongsi.core.designsystem.component.AppButton
import com.primestrider.kongsi.core.designsystem.component.AppScaffold
import com.primestrider.kongsi.core.designsystem.component.ButtonVariant
import com.primestrider.kongsi.core.designsystem.theme.PreviewTheme
import com.primestrider.kongsi.core.designsystem.theme.Spacing
import com.primestrider.kongsi.core.designsystem.theme.ThemePreviews
import com.primestrider.kongsi.core.permission.PermissionStatus
import com.primestrider.kongsi.feature.profile.impl.R

/**
 * What the app may access and why, with each permission's status. Android lets an app ask, not revoke: an allowed
 * or blocked permission is changed in the system settings, which [onAction] opens.
 */
@Composable
fun PermissionsScreen(
    permissions: List<PermissionRow>,
    onAction: (AppPermission, PermissionStatus) -> Unit,
    onBack: () -> Unit,
) {
    AppScaffold(title = stringResource(R.string.profile_section_permissions), onBack = onBack) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = Spacing.s2),
        ) {
            Text(
                stringResource(R.string.profile_permissions_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.s4, vertical = Spacing.s2),
            )
            permissions.forEachIndexed { index, row ->
                if (index > 0) HorizontalDivider(Modifier.padding(horizontal = Spacing.s4))
                PermissionItem(row, onAction)
            }
        }
    }
}

@Composable
private fun PermissionItem(
    row: PermissionRow,
    onAction: (AppPermission, PermissionStatus) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.s4, vertical = Spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
            Text(stringResource(row.permission.title), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(row.permission.purpose),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(
                    when (row.status) {
                        PermissionStatus.GRANTED -> R.string.profile_permission_granted
                        PermissionStatus.ASKABLE -> R.string.profile_permission_not_granted
                        PermissionStatus.BLOCKED -> R.string.profile_permission_blocked
                    },
                ),
                style = MaterialTheme.typography.labelLarge,
                color =
                    if (row.status == PermissionStatus.GRANTED) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        }
        val status = row.status
        AppButton(
            text =
                stringResource(
                    when (status) {
                        PermissionStatus.ASKABLE -> R.string.profile_permission_allow
                        PermissionStatus.BLOCKED -> R.string.profile_permission_open_settings
                        PermissionStatus.GRANTED -> R.string.profile_permission_manage
                    },
                ),
            onClick = { onAction(row.permission, status) },
            variant = ButtonVariant.TEXT,
        )
    }
}

@ThemePreviews
@Composable
private fun PermissionsScreenPreview() {
    PreviewTheme {
        PermissionsScreen(
            permissions =
                listOf(
                    PermissionRow(AppPermission.CONTACTS, PermissionStatus.ASKABLE),
                    PermissionRow(AppPermission.CAMERA, PermissionStatus.GRANTED),
                ),
            onAction = { _, _ -> },
            onBack = {},
        )
    }
}
