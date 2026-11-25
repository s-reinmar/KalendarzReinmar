package com.example.domain

import com.example.data.local.CalendarEventMapper
import com.example.domain.model.CalendarEntryType
import com.example.domain.model.CalendarEvent
import com.example.domain.model.DateKey
import com.example.domain.model.EventColorTag
import com.example.domain.model.EventDraft
import com.example.domain.model.EventId
import com.example.domain.model.EventLocation
import com.example.domain.model.EventRecurrence
import com.example.domain.model.EventTimeRange
import com.example.domain.model.EventTypeDetails
import com.example.domain.model.GeoPoint
import com.example.domain.model.ReminderTime
import com.example.domain.model.TaskPriority
import com.example.domain.model.TimeOfDay
import com.example.domain.model.TransportMode
import com.example.domain.service.CalendarGridBuilder
import com.example.domain.service.DemoDataFactory
import com.example.domain.service.EventScheduleMatcher
import com.example.domain.service.PolishDateFormatter
import com.example.domain.service.YearMonth
import com.example.fakes.FakeClockProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun reminderTimeAndTimeOfDayFormatAndNormalizeRanges() {
        assertEquals("00:00", ReminderTime.fromMinutes(-10).formatHHmm())
        assertEquals("23:59", ReminderTime.fromMinutes(9999).formatHHmm())
        assertEquals("09:05", ReminderTime.fromHourMinute(9, 5).formatHHmm())

        val start = requireNotNull(TimeOfDay.parseOrNull("09:30"))
        val end = requireNotNull(TimeOfDay.parseOrNull("11:00"))
        assertEquals("09:30 – 11:00", EventTimeRange(start, end).normalized().formatDisplay())
        assertEquals("11:00", EventTimeRange(end, start).normalized().formatDisplay())
    }

    @Test
    fun eventDraftSanitizesFieldsAndBirthdayComputesTurningAge() {
        val startDate = requireNotNull(DateKey.parseOrNull("2026-11-28"))
        val birthday = EventTypeDetails.Birthday(
            celebrantName = "  Kasia  ",
            birthYear = 1995,
            giftIdea = "  Album  ",
        )
        assertEquals(31, birthday.turningAgeIn(2026))

        val sanitized = EventDraft(
            title = "  Urodziny Kasi  ",
            dateKey = startDate,
            endDateKey = startDate.plusDays(2),
            isAllDay = false,
            timeRange = EventTimeRange.DEFAULT,
            recurrence = EventRecurrence.YEARLY,
            colorTag = EventColorTag.AMBER,
            details = birthday,
        ).sanitized()

        assertNotNull(sanitized)
        assertEquals("Urodziny Kasi", sanitized?.title)
        assertTrue(sanitized?.isAllDay == true)
        assertNull(sanitized?.endDateKey)
        assertNull(sanitized?.timeRange)
        val details = sanitized?.details as EventTypeDetails.Birthday
        assertEquals("Kasia", details.celebrantName)
        assertEquals("Album", details.giftIdea)
    }

    @Test
    fun eventScheduleMatcherHandlesMultiDayTripsAndYearlyBirthdays() {
        val tripStart = requireNotNull(DateKey.parseOrNull("2026-11-25"))
        val tripEnd = requireNotNull(DateKey.parseOrNull("2026-11-27"))
        val trip = CalendarEvent(
            id = EventId(1),
            title = "Wyjazd",
            dateKey = tripStart,
            endDateKey = tripEnd,
            colorTag = EventColorTag.PLUM,
            details = EventTypeDetails.Trip(transportMode = TransportMode.TRAIN),
        )
        assertTrue(EventScheduleMatcher.occursOn(trip, requireNotNull(DateKey.parseOrNull("2026-11-26"))))
        assertFalse(EventScheduleMatcher.occursOn(trip, requireNotNull(DateKey.parseOrNull("2026-11-28"))))

        val birthday = CalendarEvent(
            id = EventId(2),
            title = "Urodziny",
            dateKey = requireNotNull(DateKey.parseOrNull("2025-11-26")),
            recurrence = EventRecurrence.YEARLY,
            colorTag = EventColorTag.AMBER,
            details = EventTypeDetails.Birthday(),
        )
        assertTrue(EventScheduleMatcher.occursOn(birthday, requireNotNull(DateKey.parseOrNull("2026-11-26"))))

        val dots = EventScheduleMatcher.collectColorDotsForMonth(
            events = listOf(trip, birthday),
            year = 2026,
            month = 11,
        )
        assertEquals(
            listOf(EventColorTag.PLUM, EventColorTag.AMBER),
            dots[requireNotNull(DateKey.parseOrNull("2026-11-26"))],
        )
    }

    @Test
    fun calendarEventMapperRoundTripsAllFiveEntryTypes() {
        val baseDate = requireNotNull(DateKey.parseOrNull("2026-11-25"))
        val samples = listOf(
            CalendarEvent(
                id = EventId(1),
                title = "Event",
                dateKey = baseDate,
                isAllDay = false,
                timeRange = EventTimeRange(TimeOfDay.fromHourMinute(10, 0), TimeOfDay.fromHourMinute(11, 30)),
                description = "Opis",
                colorTag = EventColorTag.PINE,
                details = EventTypeDetails.StandardEvent(listOf("Anna", "Jan"), "https://meet.example"),
            ),
            CalendarEvent(
                id = EventId(2),
                title = "Task",
                dateKey = baseDate,
                colorTag = EventColorTag.COBALT,
                details = EventTypeDetails.Task(TaskPriority.HIGH, true, listOf("Krok 1", "Krok 2")),
            ),
            CalendarEvent(
                id = EventId(3),
                title = "Birthday",
                dateKey = baseDate,
                recurrence = EventRecurrence.YEARLY,
                colorTag = EventColorTag.AMBER,
                details = EventTypeDetails.Birthday("Ola", 1994, "Kwiaty"),
            ),
            CalendarEvent(
                id = EventId(4),
                title = "Appointment",
                dateKey = baseDate,
                colorTag = EventColorTag.TERRACOTTA,
                details = EventTypeDetails.Appointment("dr Kowalski", "+48 123 456", "Wyniki"),
            ),
            CalendarEvent(
                id = EventId(5),
                title = "Trip",
                dateKey = baseDate,
                endDateKey = baseDate.plusDays(2),
                colorTag = EventColorTag.PLUM,
                details = EventTypeDetails.Trip(TransportMode.PLANE, "LO-123", listOf("Paszport")),
            ),
        )

        samples.forEach { original ->
            val mapped = CalendarEventMapper.toDomain(CalendarEventMapper.toEntity(original))
            assertEquals(original, mapped)
        }
    }

    @Test
    fun calendarGridBuilderProducesCompleteMondayToSundayWeeks() {
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
    fun demoDataFactoryCreatesAllFiveEntryTypesWithLocations() {
        val factory = DemoDataFactory(FakeClockProvider())
        val dataset = factory.buildDataset()

        assertEquals(5, dataset.events.size)
        assertEquals(3, dataset.notes.size)
        assertEquals(CalendarEntryType.entries.toSet(), dataset.events.map { it.entryType }.toSet())
        assertTrue(dataset.events.count { it.hasLocation } >= 4)
        assertTrue(dataset.events.all { factory.isDemoEvent(it) })
        assertTrue(dataset.notes.all { factory.isDemoNote(it) })
        assertNotNull(dataset.events.first().reminderTime)
    }
}
