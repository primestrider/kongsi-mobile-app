package com.primestrider.kotlinstarter.feature.example.sdk.calendar

import android.database.sqlite.SQLiteException
import androidx.annotation.StringRes
import com.primestrider.kotlinstarter.feature.devices.impl.R
import kotlinx.coroutines.CancellationException
import java.util.Calendar
import java.util.TimeZone

private const val DAY_MILLIS = 86_400_000L
private const val MILLIS_PER_HOUR = 3_600_000L
private const val START_HOUR = 9

/** A preset booking: its title, where it takes place and how long it lasts. */
enum class Booking(
    @StringRes val title: Int,
    val location: String,
    val hours: Int,
) {
    FLIGHT(R.string.sdk_calendar_booking_flight, "Soekarno-Hatta T3", 2),
    DOCTOR(R.string.sdk_calendar_booking_doctor, "RS Pondok Indah", 1),
    MOVIE(R.string.sdk_calendar_booking_movie, "CGV Grand Indonesia", 3),
}

/** When the booking happens, as a number of days from today (always at 09:00 local). */
enum class BookingDay(
    @StringRes val label: Int,
    val days: Int,
) {
    TOMORROW(R.string.sdk_calendar_day_tomorrow, 1),
    IN_3_DAYS(R.string.sdk_calendar_day_3_days, 3),
    NEXT_WEEK(R.string.sdk_calendar_day_next_week, 7),
}

/** 09:00 in [zone] on the day [days] after [now]. `java.util.Calendar`, because java.time needs API 26. */
fun bookingStart(
    now: Long,
    days: Int,
    zone: TimeZone,
): Long =
    Calendar
        .getInstance(zone)
        .apply {
            timeInMillis = now
            add(Calendar.DAY_OF_MONTH, days)
            set(Calendar.HOUR_OF_DAY, START_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

/** The end of [booking] when it starts at [start]. */
fun bookingEnd(
    start: Long,
    booking: Booking,
): Long = start + booking.hours * MILLIS_PER_HOUR

const val REMINDER_MINUTES = 60

/** What adding a booking came to. */
sealed interface AddOutcome {
    data class Added(
        val calendarName: String,
    ) : AddOutcome

    data object NoCalendar : AddOutcome

    data object Failed : AddOutcome
}

/** Adds [booking] on [day] (09:00 in [zone]) to the first writable calendar, with a reminder an hour before when [remind]. */
suspend fun CalendarSource.addBooking(
    booking: Booking,
    title: String,
    notes: String,
    day: BookingDay,
    remind: Boolean,
    now: Long,
    zone: TimeZone,
): AddOutcome =
    try {
        val calendar = writableCalendar()
        if (calendar == null) {
            AddOutcome.NoCalendar
        } else {
            val start = bookingStart(now, day.days, zone)
            val eventId = addEvent(calendar.id, title, booking.location, notes, start, bookingEnd(start, booking), zone.id)
            if (eventId == null) {
                AddOutcome.Failed
            } else {
                if (remind) addReminder(eventId, REMINDER_MINUTES)
                AddOutcome.Added(calendar.name)
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: SecurityException) {
        AddOutcome.Failed
    } catch (e: IllegalArgumentException) {
        AddOutcome.Failed
    }

/** The "Next 7 days" list: the target calendar and its events, or [Failed] when the calendar could not be read. */
sealed interface UpcomingLoad {
    /** [calendarName] is null when there is no writable calendar (then there are no [events] either). */
    data class Loaded(
        val calendarName: String?,
        val events: List<CalendarEvent>,
    ) : UpcomingLoad

    data object Failed : UpcomingLoad
}

/**
 * Reads the first writable calendar and its events in [now, now + [days] days). A revoked permission, a provider
 * that rejects the query or a broken calendar database becomes [UpcomingLoad.Failed] instead of a crash.
 */
suspend fun CalendarSource.loadUpcoming(
    now: Long,
    days: Int,
): UpcomingLoad =
    try {
        val calendar = writableCalendar()
        UpcomingLoad.Loaded(calendar?.name, calendar?.let { upcoming(it.id, now, now + days * DAY_MILLIS) }.orEmpty())
    } catch (e: CancellationException) {
        throw e
    } catch (e: SecurityException) {
        UpcomingLoad.Failed
    } catch (e: IllegalArgumentException) {
        UpcomingLoad.Failed
    } catch (e: IllegalStateException) {
        UpcomingLoad.Failed
    } catch (e: SQLiteException) {
        UpcomingLoad.Failed
    }
