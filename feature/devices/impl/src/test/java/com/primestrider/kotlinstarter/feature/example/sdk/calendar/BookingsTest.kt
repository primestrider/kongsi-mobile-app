package com.primestrider.kotlinstarter.feature.example.sdk.calendar

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class BookingsTest {
    private val jakarta = TimeZone.getTimeZone("Asia/Jakarta")

    private fun at(
        zone: TimeZone,
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int = 0,
    ): Long =
        Calendar
            .getInstance(zone)
            .apply {
                clear()
                set(year, month, day, hour, minute, 0)
            }.timeInMillis

    @Test
    fun start_for_plus_one_day_is_tomorrow_nine_local() {
        val now = at(jakarta, 2026, Calendar.OCTOBER, 31, 15, 42)
        assertEquals(at(jakarta, 2026, Calendar.NOVEMBER, 1, 9), bookingStart(now, 1, jakarta))
    }

    @Test
    fun start_uses_the_given_zone_not_the_default() {
        // 2026-10-31 20:00 UTC is already November 1st in Jakarta (UTC+7).
        val now = at(TimeZone.getTimeZone("UTC"), 2026, Calendar.OCTOBER, 31, 20)
        assertEquals(at(jakarta, 2026, Calendar.NOVEMBER, 2, 9), bookingStart(now, 1, jakarta))
    }

    @Test
    fun start_for_next_week_is_seven_days_on() {
        val now = at(jakarta, 2026, Calendar.DECEMBER, 28, 8)
        assertEquals(at(jakarta, 2027, Calendar.JANUARY, 4, 9), bookingStart(now, BookingDay.NEXT_WEEK.days, jakarta))
    }

    @Test
    fun end_adds_the_preset_hours() {
        val start = at(jakarta, 2026, Calendar.NOVEMBER, 1, 9)
        assertEquals(start + 2 * 3_600_000L, bookingEnd(start, Booking.FLIGHT))
        assertEquals(start + 3_600_000L, bookingEnd(start, Booking.DOCTOR))
        assertEquals(start + 3 * 3_600_000L, bookingEnd(start, Booking.MOVIE))
    }
}
