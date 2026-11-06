package com.primestrider.kotlinstarter.feature.example.sdk.calendar

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.TimeZone

class AddBookingTest {
    private class FakeCalendar(
        private val calendar: CalendarInfo? = CalendarInfo(7, "Work"),
        private val eventId: Long? = 99,
        private val addFails: Boolean = false,
    ) : CalendarSource {
        var event: List<Any?>? = null
        val reminders = mutableListOf<Pair<Long, Int>>()

        override suspend fun writableCalendar() = calendar

        override suspend fun addEvent(
            calendarId: Long,
            title: String,
            location: String,
            notes: String,
            start: Long,
            end: Long,
            timeZoneId: String,
        ): Long? {
            if (addFails) throw SecurityException("no WRITE_CALENDAR")
            event = listOf(calendarId, title, location, notes, end - start, timeZoneId)
            return eventId
        }

        override suspend fun addReminder(
            eventId: Long,
            minutes: Int,
        ) {
            reminders += eventId to minutes
        }

        override suspend fun upcoming(
            calendarId: Long,
            from: Long,
            to: Long,
        ) = emptyList<CalendarEvent>()
    }

    private val jakarta = TimeZone.getTimeZone("Asia/Jakarta")

    private fun add(
        source: CalendarSource,
        remind: Boolean,
    ) = runBlocking {
        source.addBooking(Booking.FLIGHT, "Flight", "Notes", BookingDay.TOMORROW, remind, 0L, jakarta)
    }

    @Test
    fun added_with_a_reminder_an_hour_before() {
        val source = FakeCalendar()
        assertEquals(AddOutcome.Added("Work"), add(source, remind = true))
        assertEquals(listOf<Any?>(7L, "Flight", "Soekarno-Hatta T3", "Notes", 2 * 3_600_000L, "Asia/Jakarta"), source.event)
        assertEquals(listOf(99L to 60), source.reminders)
    }

    @Test
    fun added_without_a_reminder_when_the_switch_is_off() {
        val source = FakeCalendar()
        assertEquals(AddOutcome.Added("Work"), add(source, remind = false))
        assertEquals(emptyList<Pair<Long, Int>>(), source.reminders)
    }

    @Test
    fun no_writable_calendar() {
        val source = FakeCalendar(calendar = null)
        assertEquals(AddOutcome.NoCalendar, add(source, remind = true))
        assertEquals(null, source.event)
    }

    @Test
    fun a_refused_insert_fails_without_a_reminder() {
        val source = FakeCalendar(eventId = null)
        assertEquals(AddOutcome.Failed, add(source, remind = true))
        assertEquals(emptyList<Pair<Long, Int>>(), source.reminders)
    }

    @Test
    fun a_provider_error_fails() {
        assertEquals(AddOutcome.Failed, add(FakeCalendar(addFails = true), remind = true))
    }
}
