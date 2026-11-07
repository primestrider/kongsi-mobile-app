package com.primestrider.kongsi.feature.profile.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.primestrider.kongsi.core.datastore.UserProfile
import com.primestrider.kongsi.core.designsystem.component.AppScaffold
import com.primestrider.kongsi.core.designsystem.component.Avatar
import com.primestrider.kongsi.core.designsystem.component.AvatarSize
import com.primestrider.kongsi.core.designsystem.component.labelRes
import com.primestrider.kongsi.core.designsystem.theme.PreviewTheme
import com.primestrider.kongsi.core.designsystem.theme.Radii
import com.primestrider.kongsi.core.designsystem.theme.Spacing
import com.primestrider.kongsi.core.designsystem.theme.ThemePreviews
import com.primestrider.kongsi.feature.profile.impl.R

/** Callbacks of [ProfileScreen]: each row opens its own page. */
data class ProfileActions(
    val onEditProfile: () -> Unit,
    val onOpenPaymentAccounts: () -> Unit,
    val onOpenPermissions: () -> Unit,
    val onOpenAppearance: () -> Unit,
    val onOpenAbout: () -> Unit,
)

/** The Profile tab: the user's details on top, then the settings pages grouped in one card. */
@Composable
fun ProfileScreen(
    state: ProfileUiState,
    versionName: String,
    actions: ProfileActions,
) {
    AppScaffold(title = stringResource(R.string.profile_title)) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.s4, vertical = Spacing.s2),
            verticalArrangement = Arrangement.spacedBy(Spacing.s4),
        ) {
            ProfileHeader(state.profile, actions.onEditProfile)
            CardGroup {
                MenuRow(
                    icon = Icons.Filled.AccountBox,
                    title = stringResource(R.string.profile_payment_accounts),
                    subtitle = stringResource(R.string.profile_payment_accounts_subtitle),
                    onClick = actions.onOpenPaymentAccounts,
                )
                RowDivider()
                MenuRow(
                    icon = Icons.Filled.Lock,
                    title = stringResource(R.string.profile_section_permissions),
                    subtitle = stringResource(R.string.profile_permissions_subtitle),
                    onClick = actions.onOpenPermissions,
                )
                RowDivider()
                MenuRow(
                    icon = Icons.Filled.Settings,
                    title = stringResource(R.string.profile_section_appearance),
                    subtitle = "${stringResource(state.themeMode.labelRes())} · ${state.language.nativeName}",
                    onClick = actions.onOpenAppearance,
                )
                RowDivider()
                MenuRow(
                    icon = Icons.Filled.Info,
                    title = stringResource(R.string.profile_section_about),
                    subtitle = stringResource(R.string.profile_version_line, versionName),
                    onClick = actions.onOpenAbout,
                )
            }
        }
    }
}

/** White (light) or lifted (dark) card with a hairline border; rows inside are full-bleed. */
@Composable
private fun CardGroup(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(Radii.xl),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column { content() }
    }
}

@Composable
private fun RowDivider() {
    // Inset to the text column so the icon tiles read as one column.
    HorizontalDivider(Modifier.padding(start = Spacing.s16), color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun ProfileHeader(
    profile: UserProfile,
    onEdit: () -> Unit,
) {
    val savedName = profile.name?.takeIf { it.isNotBlank() }
    val name = savedName ?: stringResource(R.string.profile_name_missing)
    CardGroup {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClickLabel = stringResource(R.string.profile_edit), onClick = onEdit)
                .padding(Spacing.s4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s4),
        ) {
            // The name next to it is read out already; the avatar is decoration here.
            if (savedName == null) {
                Box(
                    Modifier
                        .size(AvatarSize.LARGE.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Avatar(name = savedName, size = AvatarSize.LARGE, modifier = Modifier.clearAndSetSemantics {})
            }
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    profile.phone ?: stringResource(R.string.profile_phone_missing),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                stringResource(R.string.profile_edit),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.s4, vertical = Spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s4),
    ) {
        Box(
            Modifier
                .size(Spacing.s8)
                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(Radii.md)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(Spacing.s5),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@ThemePreviews
@Composable
private fun ProfileScreenPreview() {
    PreviewTheme {
        ProfileScreen(
            state = ProfileUiState(profile = UserProfile("Ricky", "081234567890")),
            versionName = "1.0.0",
            actions = ProfileActions({}, {}, {}, {}, {}),
        )
    }
}
