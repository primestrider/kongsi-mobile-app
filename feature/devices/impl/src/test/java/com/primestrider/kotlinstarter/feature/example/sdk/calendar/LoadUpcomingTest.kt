package com.primestrider.kotlinstarter.feature.example.sdk.calendar

import android.database.sqlite.SQLiteException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class LoadUpcomingTest {
    private class FakeCalendar(
        private val calendar: () -> CalendarInfo? = { CalendarInfo(7, "Work") },
        private val events: () -> List<CalendarEvent> = { listOf(CalendarEvent("Flight", 5_000L)) },
    ) : CalendarSource {
        var range: Pair<Long, Long>? = null

        override suspend fun writableCalendar() = calendar()

        override suspend fun addEvent(
            calendarId: Long,
            title: String,
            location: String,
            notes: String,
            start: Long,
            end: Long,
            timeZoneId: String,
        ): Long? = error("not used")

        override suspend fun addReminder(
            eventId: Long,
            minutes: Int,
        ) = error("not used")

        override suspend fun upcoming(
            calendarId: Long,
            from: Long,
            to: Long,
        ): List<CalendarEvent> {
            range = from to to
            return events()
        }
    }

    private fun load(source: CalendarSource) = runBlocking { source.loadUpcoming(now = 1_000L, days = 8) }

    @Test
    fun loads_the_calendar_and_eight_days_of_events() {
        val source = FakeCalendar()
        assertEquals(UpcomingLoad.Loaded("Work", listOf(CalendarEvent("Flight", 5_000L))), load(source))
        assertEquals(1_000L to 1_000L + 8 * 86_400_000L, source.range)
    }

    @Test
    fun no_writable_calendar_loads_nothing() {
        assertEquals(UpcomingLoad.Loaded(null, emptyList()), load(FakeCalendar(calendar = { null })))
    }

    @Test
    fun provider_errors_on_the_first_read_become_the_error_state() {
        listOf(
            SecurityException("permission revoked"),
            IllegalArgumentException("unknown column"),
            IllegalStateException("cursor closed"),
            SQLiteException("database corrupt"),
        ).forEach { error ->
            assertEquals(error.toString(), UpcomingLoad.Failed, load(FakeCalendar(calendar = { throw error })))
            assertEquals(error.toString(), UpcomingLoad.Failed, load(FakeCalendar(events = { throw error })))
        }
    }
}
