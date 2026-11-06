package com.primestrider.kotlinstarter.feature.example.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.primestrider.kotlinstarter.core.designsystem.component.AppButton
import com.primestrider.kotlinstarter.core.designsystem.component.ButtonVariant
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.feature.catalog.impl.R
import com.primestrider.kotlinstarter.feature.example.DemoSection
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold
import java.text.DateFormat
import java.util.Date
import java.util.TimeZone

private const val FILE = "PickersScreen.kt"

/** Date and time pickers, inline and in their dialogs. */
@Composable
fun PickersScreen(onBack: () -> Unit) {
    ExamplePageScaffold(R.string.example_components_pickers_title, R.string.example_components_pickers_subtitle, onBack) {
        InlineDatePickerExample()
        DatePickerDialogExample()
        DateRangePickerExample()
        TimePickerDialogExample()
        TimeInputExample()
    }
}

/** Picker millis are midnight UTC of the chosen day, so they are formatted in UTC to show that same day. */
@Composable
private fun formatDay(millis: Long): String {
    val locale = LocalConfiguration.current.locales[0]
    return DateFormat.getDateInstance(DateFormat.MEDIUM, locale).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(millis))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InlineDatePickerExample() {
    val state = rememberDatePickerState()
    DemoSection(
        R.string.components_pickers_inline_date,
        R.string.components_pickers_inline_date_usage,
        "DatePicker(rememberDatePickerState())",
        FILE,
    ) {
        DatePicker(state, Modifier.fillMaxWidth().testTag("inlineDatePicker"))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialogExample() {
    var selected by rememberSaveable { mutableStateOf<Long?>(null) }
    var open by rememberSaveable { mutableStateOf(false) }
    DemoSection(R.string.components_pickers_date_dialog, R.string.components_pickers_date_dialog_usage, "DatePickerDialog", FILE) {
        AppButton(
            text = stringResource(R.string.components_pickers_pick_date),
            onClick = { open = true },
            variant = ButtonVariant.SECONDARY,
        )
        Text(
            stringResource(
                R.string.components_pickers_selected,
                selected?.let { formatDay(it) } ?: stringResource(R.string.components_pickers_none),
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = selected)
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    selected = state.selectedDateMillis
                    open = false
                }) { Text(stringResource(R.string.components_pickers_ok)) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.components_pickers_cancel)) } },
        ) {
            DatePicker(state)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangePickerExample() {
    val state = rememberDateRangePickerState()
    val none = stringResource(R.string.components_pickers_none)
    val start = state.selectedStartDateMillis?.let { formatDay(it) } ?: none
    val end = state.selectedEndDateMillis?.let { formatDay(it) } ?: none
    DemoSection(R.string.components_pickers_range, R.string.components_pickers_range_usage, "DateRangePicker", FILE) {
        Text(stringResource(R.string.components_pickers_range_value, start, end), style = MaterialTheme.typography.bodyMedium)
        // The range picker scrolls through months itself, so it needs a bounded height inside the scrolling page.
        DateRangePicker(state, Modifier.fillMaxWidth().height(480.dp).testTag("rangePicker"))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialogExample() {
    // Minutes after midnight; -1 = nothing chosen yet.
    var minutes by rememberSaveable { mutableIntStateOf(-1) }
    var open by rememberSaveable { mutableStateOf(false) }
    DemoSection(
        R.string.components_pickers_time_dialog,
        R.string.components_pickers_time_dialog_usage,
        "TimePickerDialog + TimePicker",
        FILE,
    ) {
        AppButton(
            text = stringResource(R.string.components_pickers_pick_time),
            onClick = { open = true },
            variant = ButtonVariant.SECONDARY,
        )
        Text(
            if (minutes < 0) {
                stringResource(R.string.components_pickers_time_none)
            } else {
                stringResource(R.string.components_pickers_time_value, minutes / 60, minutes % 60)
            },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    if (open) {
        val state =
            rememberTimePickerState(
                initialHour =
                    if (minutes <
                        0
                    ) {
                        9
                    } else {
                        minutes / 60
                    },
                initialMinute = if (minutes < 0) 0 else minutes % 60,
            )
        TimePickerDialog(
            onDismissRequest = { open = false },
            title = { Text(stringResource(R.string.components_pickers_time_title)) },
            confirmButton = {
                TextButton(onClick = {
                    minutes = state.hour * 60 + state.minute
                    open = false
                }) { Text(stringResource(R.string.components_pickers_ok)) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.components_pickers_cancel)) } },
        ) {
            TimePicker(state)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeInputExample() {
    val state = rememberTimePickerState(initialHour = 14, initialMinute = 30)
    DemoSection(R.string.components_pickers_time_input, R.string.components_pickers_time_input_usage, "TimeInput", FILE) {
        TimeInput(state, Modifier.testTag("timeInput"))
    }
}

@ThemePreviews
@Composable
private fun PickersScreenPreview() {
    PreviewTheme {
        PickersScreen(onBack = {})
    }
}
