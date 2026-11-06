package com.primestrider.kotlinstarter.feature.example.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.input.KeyboardType
import com.primestrider.kotlinstarter.core.designsystem.component.AppCheckbox
import com.primestrider.kotlinstarter.core.designsystem.component.AppRadioButton
import com.primestrider.kotlinstarter.core.designsystem.component.AppSwitch
import com.primestrider.kotlinstarter.core.designsystem.component.AppTextField
import com.primestrider.kotlinstarter.core.designsystem.component.LabeledRangeSlider
import com.primestrider.kotlinstarter.core.designsystem.component.LabeledSlider
import com.primestrider.kotlinstarter.core.designsystem.component.MultiSegmentedChoice
import com.primestrider.kotlinstarter.core.designsystem.component.PasswordTextField
import com.primestrider.kotlinstarter.core.designsystem.component.SearchField
import com.primestrider.kotlinstarter.core.designsystem.component.SegmentedChoice
import com.primestrider.kotlinstarter.core.designsystem.component.SelectField
import com.primestrider.kotlinstarter.core.designsystem.component.SelectOption
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.feature.catalog.impl.R
import com.primestrider.kotlinstarter.feature.example.DemoSection
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold
import com.primestrider.kotlinstarter.feature.example.rememberSaveableStateList
import kotlin.math.roundToInt

internal const val PASSWORD_FIELD_TAG = "inputs_password"
internal const val PARENT_CHECKBOX_TAG = "inputs_parent_checkbox"
internal const val SELECT_FIELD_TAG = "inputs_select"
internal const val TAB_ROW_TAG = "inputs_tabs"
internal const val SECONDARY_TAB_ROW_TAG = "inputs_secondary_tabs"

private const val BIO_MAX = 200

/** The four sort options, shared with the bottom sheet on the Overlays page. */
internal val sortOptions =
    listOf(
        R.string.components_sort_newest,
        R.string.components_sort_oldest,
        R.string.components_sort_price_low,
        R.string.components_sort_price_high,
    )

/** Keeps digits and the first decimal point; drops everything else (letters, grouping marks, a second dot). */
internal fun filterDecimal(input: String): String {
    var dotSeen = false
    return buildString {
        input.forEach { char ->
            when {
                char.isDigit() -> append(char)
                char == '.' && !dotSeen -> {
                    dotSeen = true
                    append(char)
                }
            }
        }
    }
}

/** Material 3 text fields, selection controls, select, sliders, segmented buttons and tabs. */
@Composable
fun InputsScreen(onBack: () -> Unit) {
    ExamplePageScaffold(
        title = R.string.example_components_inputs_title,
        subtitle = R.string.example_components_inputs_subtitle,
        onBack = onBack,
    ) {
        TextFieldsSection()
        KeyboardTypesSection()
        SelectionSection()
        SelectSection()
        SlidersSection()
        SegmentedAndTabsSection()
    }
}

@Composable
private fun TextFieldsSection() {
    var name by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("admin") }
    var bio by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    DemoSection(R.string.components_section_text_fields) {
        SearchField(query = query, onQueryChange = { query = it })
        AppTextField(
            value = name,
            onValueChange = { name = it },
            label = stringResource(R.string.components_field_name),
            placeholder = stringResource(R.string.components_field_name_placeholder),
            leadingIcon = Icons.Filled.Person,
            supportingText = stringResource(R.string.components_field_name_hint),
        )
        AppTextField(
            value = username,
            onValueChange = { username = it },
            label = stringResource(R.string.components_field_username),
            errorText = stringResource(R.string.components_field_username_error),
        )
        AppTextField(
            value = "",
            onValueChange = {},
            label = stringResource(R.string.components_field_disabled),
            enabled = false,
        )
        AppTextField(
            value = bio,
            onValueChange = { bio = it },
            label = stringResource(R.string.components_field_bio),
            placeholder = stringResource(R.string.components_field_bio_placeholder),
            singleLine = false,
            minLines = 3,
            maxLength = BIO_MAX,
        )
    }
}

@Composable
private fun KeyboardTypesSection() {
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    DemoSection(R.string.components_section_keyboard) {
        KeyboardField(email, { email = it }, R.string.components_field_email, KeyboardType.Email)
        AppTextField(
            value = phone,
            onValueChange = { phone = it },
            label = stringResource(R.string.components_field_phone),
            keyboardType = KeyboardType.Phone,
            prefix = stringResource(R.string.components_field_phone_prefix),
        )
        KeyboardField(amount, { amount = filterDecimal(it) }, R.string.components_field_amount, KeyboardType.Decimal)
        PasswordTextField(
            value = password,
            onValueChange = { password = it },
            label = stringResource(R.string.components_field_password),
            modifier = Modifier.testTag(PASSWORD_FIELD_TAG),
        )
    }
}

@Composable
private fun KeyboardField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes label: Int,
    keyboardType: KeyboardType,
) {
    AppTextField(value = value, onValueChange = onValueChange, label = stringResource(label), keyboardType = keyboardType)
}

@Composable
private fun SelectionSection() {
    var terms by rememberSaveable { mutableStateOf(false) }
    val channels = rememberSaveableStateList(false, false, false)
    var delivery by rememberSaveable { mutableIntStateOf(0) }
    var wifiOnly by rememberSaveable { mutableStateOf(true) }
    val parentState =
        when {
            channels.all { it } -> ToggleableState.On
            channels.none { it } -> ToggleableState.Off
            else -> ToggleableState.Indeterminate
        }
    DemoSection(R.string.components_section_selection) {
        LabeledRow(
            Modifier.toggleable(value = terms, role = Role.Checkbox, onValueChange = { terms = it }),
        ) {
            AppCheckbox(checked = terms, onCheckedChange = null)
            Text(stringResource(R.string.components_checkbox_terms))
        }
        LabeledRow(
            Modifier
                .testTag(PARENT_CHECKBOX_TAG)
                .triStateToggleable(state = parentState, role = Role.Checkbox, onClick = {
                    val target = parentState != ToggleableState.On
                    channels.indices.forEach { channels[it] = target }
                }),
        ) {
            AppCheckbox(state = parentState, onClick = null)
            Text(stringResource(R.string.components_checkbox_notifications))
        }
        listOf(
            R.string.components_checkbox_email,
            R.string.components_checkbox_sms,
            R.string.components_checkbox_push,
        ).forEachIndexed { index, label ->
            LabeledRow(
                Modifier
                    .padding(start = Spacing.s8)
                    .toggleable(value = channels[index], role = Role.Checkbox, onValueChange = { channels[index] = it }),
            ) {
                AppCheckbox(checked = channels[index], onCheckedChange = null)
                Text(stringResource(label))
            }
        }
        Column(Modifier.selectableGroup()) {
            listOf(
                R.string.components_radio_standard,
                R.string.components_radio_express,
                R.string.components_radio_pickup,
            ).forEachIndexed { index, label ->
                LabeledRow(
                    Modifier.selectable(selected = delivery == index, role = Role.RadioButton, onClick = { delivery = index }),
                ) {
                    AppRadioButton(selected = delivery == index, onClick = null)
                    Text(stringResource(label))
                }
            }
        }
        LabeledRow(Modifier.toggleable(value = wifiOnly, role = Role.Switch, onValueChange = { wifiOnly = it })) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.components_switch_wifi))
                Text(
                    stringResource(R.string.components_switch_wifi_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AppSwitch(checked = wifiOnly, onCheckedChange = null)
        }
    }
}

/** A full-width row whose whole area is the touch target; the control inside passes `null` as its callback. */
@Composable
private fun LabeledRow(
    modifier: Modifier,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    Row(
        modifier.fillMaxWidth().padding(vertical = Spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectSection() {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val options = sortOptions.mapIndexed { index, label -> SelectOption(index, stringResource(label)) }
    DemoSection(R.string.components_section_select) {
        SelectField(
            label = stringResource(R.string.components_select_label),
            options = options,
            selected = selected,
            onSelect = { selected = it },
            modifier = Modifier.testTag(SELECT_FIELD_TAG),
        )
    }
}

@Composable
private fun SlidersSection() {
    var volume by rememberSaveable { mutableFloatStateOf(40f) }
    var range by remember { mutableStateOf(20f..80f) }
    DemoSection(R.string.components_section_sliders) {
        LabeledSlider(
            label = stringResource(R.string.components_slider_label),
            value = volume,
            onValueChange = { volume = it },
            valueRange = 0f..100f,
            valueText = stringResource(R.string.components_slider_value, volume.roundToInt()),
        )
        LabeledRangeSlider(
            label = stringResource(R.string.components_range_label),
            value = range,
            onValueChange = { range = it },
            valueRange = 0f..100f,
            valueText = stringResource(R.string.components_range_value, range.start.roundToInt(), range.endInclusive.roundToInt()),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SegmentedAndTabsSection() {
    val periods = listOf(R.string.components_segment_day, R.string.components_segment_week, R.string.components_segment_month)
    val periodContent =
        listOf(
            R.string.components_period_day_content,
            R.string.components_period_week_content,
            R.string.components_period_month_content,
        )
    val styles = listOf(R.string.components_segment_bold, R.string.components_segment_italic, R.string.components_segment_underline)
    val tabs = listOf(R.string.components_tab_for_you, R.string.components_tab_following, R.string.components_tab_trending)
    val tabContent =
        listOf(
            R.string.components_tab_for_you_content,
            R.string.components_tab_following_content,
            R.string.components_tab_trending_content,
        )
    var period by rememberSaveable { mutableIntStateOf(1) }
    val styleChecked = rememberSaveableStateList(true, false, false)
    var primaryTab by rememberSaveable { mutableIntStateOf(0) }
    var secondaryTab by rememberSaveable { mutableIntStateOf(0) }
    DemoSection(R.string.components_section_segmented_tabs) {
        val periodLabels = periods.map { stringResource(it) }
        SegmentedChoice(periods.indices.toList(), period, onSelect = { period = it }, label = { periodLabels[it] })
        val styleLabels = styles.map { stringResource(it) }
        MultiSegmentedChoice(
            options = styles.indices.toList(),
            selected = styles.indices.filter { styleChecked[it] }.toSet(),
            onToggle = { styleChecked[it] = !styleChecked[it] },
            label = { styleLabels[it] },
        )
        PrimaryTabRow(selectedTabIndex = primaryTab, modifier = Modifier.testTag(TAB_ROW_TAG)) {
            tabs.forEachIndexed { index, label ->
                Tab(selected = primaryTab == index, onClick = { primaryTab = index }, text = { Text(stringResource(label)) })
            }
        }
        Text(stringResource(tabContent[primaryTab]), Modifier.padding(vertical = Spacing.s2))
        SecondaryTabRow(selectedTabIndex = secondaryTab, modifier = Modifier.testTag(SECONDARY_TAB_ROW_TAG)) {
            periods.forEachIndexed { index, label ->
                Tab(selected = secondaryTab == index, onClick = { secondaryTab = index }, text = { Text(stringResource(label)) })
            }
        }
        Text(stringResource(periodContent[secondaryTab]), Modifier.padding(vertical = Spacing.s2))
    }
}

@ThemePreviews
@Composable
private fun InputsScreenPreview() {
    PreviewTheme {
        InputsScreen(onBack = {})
    }
}
