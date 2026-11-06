package com.primestrider.kotlinstarter.feature.example.sdk.contacts

import android.Manifest
import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.primestrider.kotlinstarter.core.designsystem.component.AppButton
import com.primestrider.kotlinstarter.core.designsystem.component.AppTextField
import com.primestrider.kotlinstarter.core.designsystem.component.Banner
import com.primestrider.kotlinstarter.core.designsystem.component.BannerVariant
import com.primestrider.kotlinstarter.core.designsystem.component.InfoCard
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.core.permission.PermissionGate
import com.primestrider.kotlinstarter.feature.devices.impl.R
import com.primestrider.kotlinstarter.feature.example.DemoSection
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold
import com.primestrider.kotlinstarter.feature.example.sdk.SdkSection
import kotlinx.coroutines.launch

/** The system contact picker (no permission) and a search of the address book (READ_CONTACTS). */
@Composable
fun ContactsScreen(onBack: () -> Unit) {
    ExamplePageScaffold(
        title = R.string.sdk_contacts_title,
        subtitle = R.string.sdk_contacts_subtitle,
        onBack = onBack,
    ) {
        PickerSection()
        PermissionGate(
            permissions = listOf(Manifest.permission.READ_CONTACTS),
            reason = stringResource(R.string.sdk_contacts_reason),
        ) {
            SearchSection()
        }
    }
}

@Composable
private fun PickerSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf<String?>(null) }
    var phone by rememberSaveable { mutableStateOf<String?>(null) }
    var failed by rememberSaveable { mutableStateOf(false) }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
            if (uri != null) {
                scope.launch {
                    val picked = ContactsRepository(context.contentResolver).picked(uri)
                    failed = picked == null
                    if (picked != null) {
                        name = picked.name
                        phone = picked.phone
                    }
                }
            }
        }
    SdkSection(R.string.sdk_contacts_picker_title, R.string.sdk_contacts_picker_description) {
        AppButton(
            stringResource(R.string.sdk_contacts_choose),
            onClick = {
                failed = false
                try {
                    picker.launch(null)
                } catch (e: ActivityNotFoundException) {
                    failed = true
                }
            },
        )
        if (failed) Banner(title = stringResource(R.string.sdk_contacts_picker_failed), variant = BannerVariant.ERROR)
        if (name != null || phone != null) {
            InfoCard(
                listOf(
                    stringResource(R.string.sdk_contacts_name) to name,
                    stringResource(R.string.sdk_contacts_phone) to phone,
                ),
            )
        }
    }
}

@Composable
private fun SearchSection(viewModel: ContactsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ContactsSearchContent(state, viewModel::onQueryChange)
}

/** The search section for a given [state]: count caption, field, then nothing yet, "No contacts found." or the rows. */
@Composable
internal fun ContactsSearchContent(
    state: ContactsUiState,
    onQueryChange: (String) -> Unit,
) {
    DemoSection(R.string.sdk_contacts_search_title) {
        Text(
            state.count?.let { stringResource(R.string.sdk_contacts_search_description_count, it) }
                ?: stringResource(R.string.sdk_contacts_search_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AppTextField(
            value = state.query,
            onValueChange = onQueryChange,
            label = stringResource(R.string.sdk_contacts_search_field),
        )
        when {
            !state.loaded -> Unit
            state.rows.isEmpty() -> Text(stringResource(R.string.sdk_contacts_empty), style = MaterialTheme.typography.bodyMedium)
            else ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column {
                        state.rows.forEachIndexed { index, row ->
                            if (index > 0) HorizontalDivider()
                            ContactItem(row)
                        }
                    }
                }
        }
    }
}

@Composable
private fun ContactItem(row: ContactRow) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.s4, vertical = Spacing.s3),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s4),
    ) {
        Text(
            row.name ?: stringResource(R.string.sdk_contacts_unnamed),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(row.phone ?: "—", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@ThemePreviews
@Composable
private fun ContactsSearchContentPreview() {
    PreviewTheme {
        ExamplePageScaffold(title = R.string.sdk_contacts_title, subtitle = R.string.sdk_contacts_subtitle, onBack = {}) {
            ContactsSearchContent(
                state =
                    ContactsUiState(
                        query = "an",
                        count = 128,
                        rows = listOf(ContactRow("Ana Silva", "+62 812 3456 7890"), ContactRow("Andi Pratama", null)),
                        loaded = true,
                    ),
                onQueryChange = {},
            )
        }
    }
}
