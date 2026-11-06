package com.primestrider.kotlinstarter.feature.example.sdk.calendar

import android.Manifest
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.primestrider.kotlinstarter.core.designsystem.component.AppButton
import com.primestrider.kotlinstarter.core.designsystem.component.Banner
import com.primestrider.kotlinstarter.core.designsystem.component.BannerVariant
import com.primestrider.kotlinstarter.core.designsystem.component.InfoCard
import com.primestrider.kotlinstarter.core.designsystem.component.SelectField
import com.primestrider.kotlinstarter.core.designsystem.component.SelectOption
import com.primestrider.kotlinstarter.core.designsystem.component.SwitchRow
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.core.permission.PermissionGate
import com.primestrider.kotlinstarter.core.util.currentLocale
import com.primestrider.kotlinstarter.feature.devices.impl.R
import com.primestrider.kotlinstarter.feature.example.DemoSection
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold
import com.primestrider.kotlinstarter.feature.example.sdk.SdkSection
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

private const val UPCOMING_DAYS = 8
private const val EVENT_TIME_PATTERN = "EEE dd MMM, HH:mm"

private enum class AddResult { NONE, ADDED, NO_CALENDAR, FAILED }

/** Adds a preset booking to the first writable calendar, optionally with a reminder, and lists the next 7 days. */
@Composable
fun CalendarScreen(onBack: () -> Unit) {
    ExamplePageScaffold(
        title = R.string.sdk_calendar_title,
        subtitle = R.string.sdk_calendar_subtitle,
        onBack = onBack,
    ) {
        PermissionGate(
            permissions = listOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR),
            reason = stringResource(R.string.sdk_calendar_reason),
        ) {
            CalendarContent()
        }
    }
}

@Composable
private fun CalendarContent() {
    val context = LocalContext.current
    val locale = currentLocale()
    val repository = remember(context) { CalendarRepository(context.contentResolver) }
    val scope = rememberCoroutineScope()
    var booking by rememberSaveable { mutableStateOf(Booking.FLIGHT) }
    var day by rememberSaveable { mutableStateOf(BookingDay.TOMORROW) }
    var remind by rememberSaveable { mutableStateOf(true) }
    var result by rememberSaveable { mutableStateOf(AddResult.NONE) }
    var calendarName by rememberSaveable { mutableStateOf<String?>(null) }
    var events by remember { mutableStateOf<List<CalendarEvent>?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    var loadFailed by remember { mutableStateOf(false) }
    val bookingTitles = Booking.entries.associateWith { stringResource(it.title) }
    val dayLabels = BookingDay.entries.associateWith { stringResource(it.label) }
    val format = remember(locale) { SimpleDateFormat(EVENT_TIME_PATTERN, locale) }

    // Runs again after each added event, so the list shows it.
    LaunchedEffect(repository, refresh) {
        when (val load = repository.loadUpcoming(System.currentTimeMillis(), UPCOMING_DAYS)) {
            is UpcomingLoad.Loaded -> {
                loadFailed = false
                calendarName = load.calendarName
                events = load.events
            }
            UpcomingLoad.Failed -> {
                loadFailed = true
                events = null
            }
        }
    }

    SdkSection(R.string.sdk_calendar_add_title, R.string.sdk_calendar_add_description) {
        SelectField(
            label = stringResource(R.string.sdk_calendar_booking),
            options = Booking.entries.map { SelectOption(it, bookingTitles.getValue(it)) },
            selected = booking,
            onSelect = { booking = it },
        )
        Text(
            stringResource(R.string.sdk_calendar_booking_detail, booking.location, booking.hours),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SelectField(
            label = stringResource(R.string.sdk_calendar_when),
            options = BookingDay.entries.map { SelectOption(it, dayLabels.getValue(it)) },
            selected = day,
            onSelect = { day = it },
        )
        SwitchRow(label = stringResource(R.string.sdk_calendar_remind), checked = remind, onCheckedChange = { remind = it })
        val title = bookingTitles.getValue(booking)
        val notes = stringResource(R.string.sdk_calendar_notes)
        AppButton(
            stringResource(R.string.sdk_calendar_add),
            onClick = {
                scope.launch {
                    result =
                        when (
                            val outcome =
                                repository.addBooking(booking, title, notes, day, remind, System.currentTimeMillis(), TimeZone.getDefault())
                        ) {
                            is AddOutcome.Added -> {
                                calendarName = outcome.calendarName
                                refresh++
                                AddResult.ADDED
                            }

                            AddOutcome.NoCalendar -> AddResult.NO_CALENDAR

                            AddOutcome.Failed -> AddResult.FAILED
                        }
                }
            },
        )
        when (result) {
            AddResult.NONE -> Unit
            AddResult.ADDED ->
                Banner(
                    title = stringResource(R.string.sdk_calendar_added, calendarName.orEmpty()),
                    variant = BannerVariant.SUCCESS,
                    onDismiss = { result = AddResult.NONE },
                )
            AddResult.NO_CALENDAR ->
                Banner(title = stringResource(R.string.sdk_calendar_no_calendar), variant = BannerVariant.WARNING)
            AddResult.FAILED -> Banner(title = stringResource(R.string.sdk_calendar_failed), variant = BannerVariant.ERROR)
        }
    }
    DemoSection(R.string.sdk_calendar_next_title) {
        val name = calendarName
        if (name != null) {
            Text(
                stringResource(R.string.sdk_calendar_next_description, name),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val upcoming = events.orEmpty()
        if (loadFailed) {
            Banner(title = stringResource(R.string.sdk_calendar_load_failed), variant = BannerVariant.ERROR)
        } else if (upcoming.isEmpty()) {
            Text(stringResource(R.string.sdk_calendar_next_empty), style = MaterialTheme.typography.bodyMedium)
        } else {
            InfoCard(upcoming.map { it.title to format.format(Date(it.start)) })
        }
    }
}

@ThemePreviews
@Composable
private fun CalendarScreenPreview() {
    PreviewTheme {
        CalendarScreen(onBack = {})
    }
}
