package com.primestrider.kotlinstarter.feature.example.sdk.calendar

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.provider.CalendarContract
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A calendar the demo may write to. */
data class CalendarInfo(
    val id: Long,
    val name: String,
)

/** An event that starts at [start] (epoch millis). */
data class CalendarEvent(
    val title: String,
    val start: Long,
)

/** What the Calendar demo needs from the device calendar; [addBooking] is written against this, not a ContentResolver. */
interface CalendarSource {
    suspend fun writableCalendar(): CalendarInfo?

    suspend fun addEvent(
        calendarId: Long,
        title: String,
        location: String,
        notes: String,
        start: Long,
        end: Long,
        timeZoneId: String,
    ): Long?

    suspend fun addReminder(
        eventId: Long,
        minutes: Int,
    )

    suspend fun upcoming(
        calendarId: Long,
        from: Long,
        to: Long,
    ): List<CalendarEvent>
}

/** Reads and writes the device calendar through `CalendarContract`. Every call runs on [io]. */
class CalendarRepository(
    private val contentResolver: ContentResolver,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : CalendarSource {
    /** The first visible calendar the user can add events to, or null when there is none. */
    override suspend fun writableCalendar(): CalendarInfo? =
        withContext(io) {
            contentResolver
                .query(
                    CalendarContract.Calendars.CONTENT_URI,
                    arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME),
                    "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ? AND ${CalendarContract.Calendars.VISIBLE} = 1",
                    arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()),
                    "${CalendarContract.Calendars._ID} ASC",
                )?.use { if (it.moveToFirst()) CalendarInfo(it.getLong(0), it.getString(1).orEmpty()) else null }
        }

    /** Inserts the event and returns its id, or null when the provider refused it. */
    override suspend fun addEvent(
        calendarId: Long,
        title: String,
        location: String,
        notes: String,
        start: Long,
        end: Long,
        timeZoneId: String,
    ): Long? =
        withContext(io) {
            val values =
                ContentValues().apply {
                    put(CalendarContract.Events.CALENDAR_ID, calendarId)
                    put(CalendarContract.Events.TITLE, title)
                    put(CalendarContract.Events.EVENT_LOCATION, location)
                    put(CalendarContract.Events.DESCRIPTION, notes)
                    put(CalendarContract.Events.DTSTART, start)
                    put(CalendarContract.Events.DTEND, end)
                    put(CalendarContract.Events.EVENT_TIMEZONE, timeZoneId)
                }
            contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)?.let(ContentUris::parseId)
        }

    /** An alert [minutes] before the event starts. */
    override suspend fun addReminder(
        eventId: Long,
        minutes: Int,
    ) {
        withContext(io) {
            val values =
                ContentValues().apply {
                    put(CalendarContract.Reminders.EVENT_ID, eventId)
                    put(CalendarContract.Reminders.MINUTES, minutes)
                    put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                }
            contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, values)
        }
    }

    /** Occurrences (recurring events included) of [calendarId] that start between [from] and [to], earliest first. */
    override suspend fun upcoming(
        calendarId: Long,
        from: Long,
        to: Long,
    ): List<CalendarEvent> =
        withContext(io) {
            val uri =
                CalendarContract.Instances.CONTENT_URI
                    .buildUpon()
                    .also {
                        ContentUris.appendId(it, from)
                        ContentUris.appendId(it, to)
                    }.build()
            val events = mutableListOf<CalendarEvent>()
            contentResolver
                .query(
                    uri,
                    arrayOf(CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN),
                    "${CalendarContract.Instances.CALENDAR_ID} = ?",
                    arrayOf(calendarId.toString()),
                    "${CalendarContract.Instances.BEGIN} ASC",
                )?.use { cursor ->
                    // Instances also lists events that began earlier and are still running; this list starts at [from].
                    while (cursor.moveToNext()) {
                        if (cursor.getLong(1) >= from) events += CalendarEvent(cursor.getString(0).orEmpty(), cursor.getLong(1))
                    }
                }
            events
        }
}
