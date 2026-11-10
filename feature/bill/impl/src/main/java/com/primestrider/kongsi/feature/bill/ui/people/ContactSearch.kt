package com.primestrider.kongsi.feature.bill.ui.people

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Phone
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.primestrider.kongsi.core.designsystem.component.AppButton
import com.primestrider.kongsi.core.designsystem.component.AvatarSize
import com.primestrider.kongsi.core.designsystem.component.ButtonVariant
import com.primestrider.kongsi.core.designsystem.component.SearchField
import com.primestrider.kongsi.core.designsystem.theme.AppThemeTokens
import com.primestrider.kongsi.core.designsystem.theme.PreviewTheme
import com.primestrider.kongsi.core.designsystem.theme.Radii
import com.primestrider.kongsi.core.designsystem.theme.Spacing
import com.primestrider.kongsi.core.designsystem.theme.ThemePreviews
import com.primestrider.kongsi.core.permission.PermissionStatus
import com.primestrider.kongsi.core.permission.openAppSettings
import com.primestrider.kongsi.core.permission.rememberPermissionRequest
import com.primestrider.kongsi.core.util.IndonesianPhone
import com.primestrider.kongsi.feature.bill.impl.R
import com.primestrider.kongsi.feature.bill.ui.flow.PersonAvatar

// Pieces of the contact search, shared by the People step and "Add number" on the Summary.

/** Opens the system contact picker on phone numbers: it returns one number with temporary access, no permission. */
internal object PickPhoneNumber : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(
        context: Context,
        input: Unit,
    ) = Intent(Intent.ACTION_PICK, Phone.CONTENT_URI)

    override fun parseResult(
        resultCode: Int,
        intent: Intent?,
    ): Uri? = intent?.data
}

/**
 * The search field over contacts. The contacts permission is asked the first time the field is focused, with the reason
 * shown above it; when it was refused for good, the field says so and offers the settings. [onContactsAllowed] reports
 * whether the address book may be searched.
 */
@Composable
internal fun ContactSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    onContactsAllowed: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val contactsPermission = rememberPermissionRequest(Manifest.permission.READ_CONTACTS) { granted -> onContactsAllowed(granted) }
    LaunchedEffect(contactsPermission.status) { onContactsAllowed(contactsPermission.status == PermissionStatus.GRANTED) }
    var asked by rememberSaveable { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        when {
            contactsPermission.status == PermissionStatus.BLOCKED -> ContactsBlocked()
            contactsPermission.status == PermissionStatus.ASKABLE && !asked ->
                Text(
                    stringResource(R.string.bill_people_contacts_reason),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            else -> Unit
        }
        SearchField(
            query = query,
            onQueryChange = onQueryChange,
            placeholder = placeholder,
            modifier =
                Modifier.onFocusChanged { focus ->
                    // Asked once, when the user starts searching; a blocked permission is not asked again.
                    if (focus.isFocused && !asked && contactsPermission.status == PermissionStatus.ASKABLE) {
                        asked = true
                        contactsPermission.launch()
                    }
                },
        )
    }
}

/** The contacts permission was refused for good: say so and offer the system settings; the picker still works. */
@Composable
private fun ContactsBlocked() {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        Text(
            stringResource(R.string.bill_people_contacts_blocked),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        AppButton(
            text = stringResource(R.string.bill_people_open_settings),
            onClick = { openAppSettings(context) },
            variant = ButtonVariant.TEXT,
        )
    }
}

/** A number as people read it: the local `08…` form for Indonesian mobiles, otherwise as saved. */
internal fun shownNumber(phone: String) = IndonesianPhone.local(phone) ?: phone

/** A tappable result row of the contact search. */
@Composable
internal fun ChoiceRow(
    onClick: () -> Unit,
    horizontalPadding: Dp = Spacing.s4,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = horizontalPadding, vertical = Spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        content = content,
    )
}

/** A found person: avatar, name, then their number (or "No number") and a WhatsApp badge when they have it. */
@Composable
internal fun NameAndNumber(
    name: String,
    phone: String?,
    whatsApp: Boolean,
) {
    PersonAvatar(name, index = 0, size = AvatarSize.SMALL)
    Column(Modifier.fillMaxWidth()) {
        Text(name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2), verticalAlignment = Alignment.CenterVertically) {
            Text(
                phone ?: stringResource(R.string.bill_people_no_number),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (whatsApp) {
                Text(
                    stringResource(R.string.bill_people_whatsapp),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppThemeTokens.colors.success,
                    modifier =
                        Modifier
                            .background(AppThemeTokens.colors.success.copy(alpha = 0.12f), RoundedCornerShape(Radii.full))
                            .padding(horizontal = Spacing.s2, vertical = Spacing.s0_5),
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun ContactSearchPreview() {
    PreviewTheme {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            ContactSearchField(
                query = "ci",
                onQueryChange = {},
                placeholder = "Search contacts or type a name",
                onContactsAllowed = {},
                modifier = Modifier.padding(horizontal = Spacing.s4),
            )
            ChoiceRow(onClick = {}) { NameAndNumber("Citra Dewi", "0813-1111-2222", whatsApp = true) }
            ChoiceRow(onClick = {}) { NameAndNumber("Dimas", null, whatsApp = false) }
        }
    }
}
