package com.example.domain

import com.example.domain.model.DateKey
import com.example.domain.model.EventLocation
import com.example.domain.model.GeoPoint
import com.example.domain.model.ReminderTime
import com.example.domain.service.CalendarGridBuilder
import com.example.domain.service.DemoDataFactory
import com.example.domain.service.PolishDateFormatter
import com.example.domain.service.YearMonth
import com.example.fakes.FakeClockProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainModelsTest {

    @Test
    fun dateKeyParsesIsoCompactAndPolishFormatsAndRejectsInvalidDates() {
        assertEquals("2026-11-25", DateKey.parseOrNull("2026-11-25")?.value)
        assertEquals("2026-11-25", DateKey.parseOrNull("20261125T093000")?.value)
        assertEquals("2026-11-25", DateKey.parseOrNull("25.11.2026")?.value)
        assertEquals("2024-02-29", DateKey.parseOrNull("2024-02-29")?.value)

        assertNull(DateKey.parseOrNull("2026-02-29"))
        assertNull(DateKey.parseOrNull("2026-13-01"))
        assertNull(DateKey.parseOrNull("not-a-date"))
    }

    @Test
    fun dateKeyArithmeticHandlesMonthAndYearRollover() {
        val dec31 = requireNotNull(DateKey.parseOrNull("2026-12-31"))
        assertEquals("2027-01-01", dec31.nextDay().value)
        assertEquals("2027-01-05", dec31.plusDays(5).value)
    }

    @Test
    fun geoPointRejectsInvalidCoordinatesAndFormatsSummary() {
        assertNull(GeoPoint.createOrNull(95.0, 20.0))
        assertNull(GeoPoint.createOrNull(50.0, -181.0))
        assertNull(GeoPoint.createOrNull(Double.NaN, 20.0))
        assertNull(GeoPoint.createOrNull(Double.POSITIVE_INFINITY, 20.0))

        val point = requireNotNull(GeoPoint.createOrNull(52.2297, 21.0122))
        val location = EventLocation(point = point, label = "  Warszawa  ")
        assertEquals("Warszawa (52.22970, 21.01220)", location.formatSummary())
    }

    @Test
    fun reminderTimeClampsAndFormatsHHmm() {
        assertEquals("00:00", ReminderTime.fromMinutes(-10).formatHHmm())
        assertEquals("23:59", ReminderTime.fromMinutes(9999).formatHHmm())
        assertEquals("09:05", ReminderTime.fromHourMinute(9, 5).formatHHmm())
    }

    @Test
    fun calendarGridBuilderProducesCompleteMondayToSundayWeeks() {
        // November 2026 starts on Sunday -> 6 leading empty cells, 30 days, 6 trailing empty = 42 cells
        val grid = CalendarGridBuilder.buildMonthGrid(2026, 11)
        assertEquals(0, grid.size % 7)
        assertEquals(6, grid.takeWhile { it == null }.size)
        assertEquals(30, grid.count { it != null })
        assertEquals("Listopad 2026", PolishDateFormatter.formatMonthYear(2026, 11))
    }

    @Test
    fun yearMonthNavigationWrapsAcrossYears() {
        assertEquals(YearMonth(2025, 12), YearMonth(2026, 1).previous())
        assertEquals(YearMonth(2027, 1), YearMonth(2026, 12).next())
    }

    @Test
    fun demoDataFactoryCreatesConsistentPolishDatasetWithLocations() {
        val factory = DemoDataFactory(FakeClockProvider())
        val dataset = factory.buildDataset()

        assertEquals(5, dataset.events.size)
        assertEquals(3, dataset.notes.size)
        assertTrue(dataset.events.count { it.hasLocation } >= 4)
        assertTrue(dataset.events.all { factory.isDemoEvent(it) })
        assertTrue(dataset.notes.all { factory.isDemoNote(it) })
        assertNotNull(dataset.events.first().reminderTime)
    }
}
