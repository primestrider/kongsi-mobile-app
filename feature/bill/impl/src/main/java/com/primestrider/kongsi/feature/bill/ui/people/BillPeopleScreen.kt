package com.primestrider.kongsi.feature.bill.ui.people

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Phone
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.primestrider.kongsi.core.designsystem.component.AppButton
import com.primestrider.kongsi.core.designsystem.component.AppScaffold
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
import com.primestrider.kongsi.feature.bill.contacts.ContactMatch
import com.primestrider.kongsi.feature.bill.data.FrequentPerson
import com.primestrider.kongsi.feature.bill.impl.R
import com.primestrider.kongsi.feature.bill.model.Person
import com.primestrider.kongsi.feature.bill.ui.flow.FlowStep
import com.primestrider.kongsi.feature.bill.ui.flow.PersonAvatar
import com.primestrider.kongsi.feature.bill.ui.flow.RunningTotal
import com.primestrider.kongsi.feature.bill.ui.flow.RunningTotalBar
import com.primestrider.kongsi.feature.bill.ui.flow.StepIndicator

/** Callbacks of [BillPeopleScreen]. */
data class BillPeopleActions(
    val onBack: () -> Unit,
    val onBackToItems: () -> Unit,
    val onQueryChange: (String) -> Unit,
    val onContactsAllowed: (Boolean) -> Unit,
    val onAdd: (Suggestion) -> Unit,
    val onRemove: (Long) -> Unit,
    val onPickerResult: (Uri?) -> Unit,
    val onNext: () -> Unit,
)

/** Opens the system contact picker on phone numbers: it returns one number with temporary access, no permission. */
private object PickPhoneNumber : ActivityResultContract<Unit, Uri?>() {
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
 * People: who was there. Searching asks for the contacts permission the first time the field is focused, with the
 * reason shown above it; whatever the answer, the contact picker, people from earlier bills and typed names still
 * work. [query] is the search text, read straight from the ViewModel's Compose state.
 */
@Composable
fun BillPeopleScreen(
    state: BillPeopleUiState,
    query: String,
    actions: BillPeopleActions,
) {
    val contactsPermission = rememberPermissionRequest(Manifest.permission.READ_CONTACTS) { granted -> actions.onContactsAllowed(granted) }
    LaunchedEffect(contactsPermission.status) { actions.onContactsAllowed(contactsPermission.status == PermissionStatus.GRANTED) }
    var asked by rememberSaveable { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(PickPhoneNumber) { uri -> actions.onPickerResult(uri) }

    AppScaffold(
        title = stringResource(R.string.bill_people_title),
        onBack = actions.onBack,
        bottomBar = {
            RunningTotalBar(
                RunningTotal.Receipt(state.totalMinor, state.receiptMinor),
                actionLabel = stringResource(R.string.bill_review_next),
                actionEnabled = state.canContinue,
                onAction = actions.onNext,
            )
        },
    ) { padding ->
        if (!state.loaded) return@AppScaffold
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = Spacing.s6),
            verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        ) {
            item(key = "steps") {
                StepIndicator(current = FlowStep.PEOPLE, onStepClick = { if (it == FlowStep.ITEMS) actions.onBackToItems() })
            }
            item(key = "search") {
                Column(Modifier.padding(horizontal = Spacing.s4), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
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
                        onQueryChange = actions.onQueryChange,
                        placeholder = stringResource(R.string.bill_people_search),
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
            val frequent = state.suggestions.filterIsInstance<Suggestion.Frequent>()
            if (query.isBlank() && frequent.isNotEmpty()) {
                item(key = "frequent header") { SectionTitle(stringResource(R.string.bill_people_frequent)) }
            }
            items(state.suggestions, key = { it.key() }) { suggestion ->
                SuggestionRow(suggestion, onClick = { actions.onAdd(suggestion) })
            }
            item(key = "picker") {
                AppButton(
                    text = stringResource(R.string.bill_people_pick_contact),
                    onClick = { picker.launch(Unit) },
                    variant = ButtonVariant.SECONDARY,
                    leadingIcon = Icons.Filled.Person,
                    modifier = Modifier.padding(horizontal = Spacing.s4, vertical = Spacing.s2),
                )
            }
            item(key = "people header") { SectionTitle(stringResource(R.string.bill_people_on_bill)) }
            items(state.people, key = { "person ${it.person.id}" }) { row ->
                PersonCard(row, onRemove = { actions.onRemove(row.person.id) })
            }
            if (state.people.size < 2) {
                item(key = "people empty") {
                    Text(
                        stringResource(R.string.bill_people_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Spacing.s4),
                    )
                }
            }
        }
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

private fun Suggestion.key(): String =
    when (this) {
        is Suggestion.Frequent -> "frequent ${person.id}"
        is Suggestion.Contact -> "contact ${match.contactId} ${match.phone}"
        is Suggestion.Typed -> "typed"
    }

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = Spacing.s4, end = Spacing.s4, top = Spacing.s3).semantics { heading() },
    )
}

/** A number as people read it: the local `08…` form for Indonesian mobiles, otherwise as saved. */
private fun shownNumber(phone: String) = IndonesianPhone.local(phone) ?: phone

@Composable
private fun SuggestionRow(
    suggestion: Suggestion,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.s4, vertical = Spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
    ) {
        when (suggestion) {
            is Suggestion.Typed -> {
                Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    stringResource(R.string.bill_people_add_typed, suggestion.name),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
            }
            is Suggestion.Frequent ->
                NameAndNumber(suggestion.person.name, suggestion.person.phone?.let(::shownNumber), whatsApp = false)
            // As written in the address book, so it is easy to recognise.
            is Suggestion.Contact ->
                NameAndNumber(suggestion.match.name, suggestion.match.phone, whatsApp = suggestion.match.hasWhatsApp)
        }
    }
}

@Composable
private fun NameAndNumber(
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

@Composable
private fun PersonCard(
    row: PersonRow,
    onRemove: () -> Unit,
) {
    val name =
        if (row.isMe) {
            if (row.person.name.isBlank()) {
                stringResource(R.string.bill_people_me)
            } else {
                stringResource(R.string.bill_people_me_named, row.person.name)
            }
        } else {
            row.label
        }
    Surface(
        shape = RoundedCornerShape(Radii.xl),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.padding(horizontal = Spacing.s4).fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = Spacing.s3, top = Spacing.s2, bottom = Spacing.s2, end = Spacing.s1),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        ) {
            PersonAvatar(row.person.name.ifBlank { name }, index = row.index)
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    when {
                        row.isMe -> stringResource(R.string.bill_people_me_pays)
                        row.person.phone != null -> shownNumber(row.person.phone)
                        else -> stringResource(R.string.bill_people_no_number)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!row.isMe) {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.bill_people_remove_cd, row.label))
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun BillPeopleScreenPreview() {
    PreviewTheme {
        BillPeopleScreen(
            state =
                BillPeopleUiState(
                    loaded = true,
                    people =
                        listOf(
                            PersonRow(Person(1, "Ricky", "6281111222233"), 0, "Ricky", isMe = true),
                            PersonRow(Person(2, "Budi Santoso", "6281234567890"), 1, "Budi Santoso", isMe = false),
                            PersonRow(Person(3, "Ani", null), 2, "Ani", isMe = false),
                        ),
                    suggestions =
                        listOf(
                            Suggestion.Contact(
                                ContactMatch(9, "Citra Dewi", "0813-1111-2222", "6281311112222", true, null, hasWhatsApp = true),
                            ),
                            Suggestion.Frequent(FrequentPerson(4, "Dimas", null, 2)),
                            Suggestion.Typed("ci"),
                        ),
                    totalMinor = 152_460,
                    canContinue = true,
                ),
            query = "ci",
            actions = BillPeopleActions({}, {}, {}, {}, {}, {}, {}, {}),
        )
    }
}
