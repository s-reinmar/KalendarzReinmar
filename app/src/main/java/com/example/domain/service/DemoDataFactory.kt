package com.example.domain.service

import com.example.domain.model.CalendarEvent
import com.example.domain.model.EventColorTag
import com.example.domain.model.EventId
import com.example.domain.model.EventLocation
import com.example.domain.model.EventRecurrence
import com.example.domain.model.EventTimeRange
import com.example.domain.model.EventTypeDetails
import com.example.domain.model.GeoPoint
import com.example.domain.model.Note
import com.example.domain.model.NoteId
import com.example.domain.model.ReminderTime
import com.example.domain.model.TaskPriority
import com.example.domain.model.TimeOfDay
import com.example.domain.model.TransportMode
import com.example.domain.port.ClockProvider

data class DemoDataset(
    val events: List<CalendarEvent>,
    val notes: List<Note>,
)

class DemoDataFactory(
    private val clock: ClockProvider,
) {
    fun buildDataset(): DemoDataset {
        val baseDate = clock.today().toDateKey()
        val now = clock.nowMillis()

        val events = listOf(
            CalendarEvent(
                id = EventId.UNASSIGNED,
                title = "Prezentacja dema architektury MVC",
                dateKey = baseDate,
                isAllDay = false,
                timeRange = EventTimeRange(
                    start = TimeOfDay.fromHourMinute(10, 0),
                    end = TimeOfDay.fromHourMinute(11, 30),
                ),
                description = "Omówienie rozdzielenia Model–View–Controller i zasad SOLID z zespołem.",
                recurrence = EventRecurrence.NONE,
                colorTag = EventColorTag.PINE,
                details = EventTypeDetails.StandardEvent(
                    guests = listOf("Anna Kowalska", "Marek Nowak"),
                    meetingLink = "https://meet.google.com/abc-defg-hij",
                ),
                reminderTime = ReminderTime.fromHourMinute(9, 45),
                location = EventLocation(
                    point = requireNotNull(GeoPoint.createOrNull(52.23195, 21.00672)),
                    label = "Warszawa, Pałac Kultury i Nauki",
                ),
                createdAtMillis = now,
            ),
            CalendarEvent(
                id = EventId.UNASSIGNED,
                title = "Przegląd sprintu i testy odbiorcze",
                dateKey = baseDate.plusDays(1),
                isAllDay = false,
                timeRange = EventTimeRange(
                    start = TimeOfDay.fromHourMinute(14, 0),
                    end = TimeOfDay.fromHourMinute(15, 0),
                ),
                description = "Zamknięcie zaległych zgłoszeń i weryfikacja migracji bazy Room.",
                recurrence = EventRecurrence.NONE,
                colorTag = EventColorTag.COBALT,
                details = EventTypeDetails.Task(
                    priority = TaskPriority.HIGH,
                    isCompleted = false,
                    checklist = listOf(
                        "Uruchomić testy jednostkowe",
                        "Zweryfikować eksport ICS i CSV",
                        "Sprawdzić ciemny motyw",
                    ),
                ),
                reminderTime = ReminderTime.fromHourMinute(13, 30),
                location = EventLocation(
                    point = requireNotNull(GeoPoint.createOrNull(50.06194, 19.93685)),
                    label = "Kraków, Rynek Główny",
                ),
                createdAtMillis = now + 1L,
            ),
            CalendarEvent(
                id = EventId.UNASSIGNED,
                title = "Urodziny Kasi",
                dateKey = baseDate.plusDays(3),
                isAllDay = true,
                description = "Kameralna kolacja urodzinowa w centrum.",
                recurrence = EventRecurrence.YEARLY,
                colorTag = EventColorTag.AMBER,
                details = EventTypeDetails.Birthday(
                    celebrantName = "Kasia Wiśniewska",
                    birthYear = 1995,
                    giftIdea = "Album fotograficzny i kawa ziarnista",
                ),
                reminderTime = ReminderTime.fromHourMinute(9, 0),
                location = EventLocation(
                    point = requireNotNull(GeoPoint.createOrNull(51.10929, 17.03208)),
                    label = "Wrocław, Rynek",
                ),
                createdAtMillis = now + 2L,
            ),
            CalendarEvent(
                id = EventId.UNASSIGNED,
                title = "Wizyta kontrolna — Stomatolog",
                dateKey = baseDate.plusDays(5),
                isAllDay = false,
                timeRange = EventTimeRange(
                    start = TimeOfDay.fromHourMinute(11, 0),
                    end = TimeOfDay.fromHourMinute(11, 45),
                ),
                description = "Coroczny przegląd i higienizacja.",
                recurrence = EventRecurrence.NONE,
                colorTag = EventColorTag.TERRACOTTA,
                details = EventTypeDetails.Appointment(
                    specialistName = "lek. dent. Piotr Zieliński",
                    contactPhone = "+48 58 301 44 20",
                    preparationNotes = "Zabrać dowód osobisty i zdjęcie pantomograficzne.",
                ),
                reminderTime = ReminderTime.fromHourMinute(10, 0),
                location = EventLocation(
                    point = requireNotNull(GeoPoint.createOrNull(54.35202, 18.64663)),
                    label = "Gdańsk, Długi Targ",
                ),
                createdAtMillis = now + 3L,
            ),
            CalendarEvent(
                id = EventId.UNASSIGNED,
                title = "Wyjazd służbowy — Konferencja Kotlin",
                dateKey = baseDate.plusDays(8),
                endDateKey = baseDate.plusDays(10),
                isAllDay = true,
                description = "Trzydniowa konferencja z warsztatami Jetpack Compose.",
                recurrence = EventRecurrence.NONE,
                colorTag = EventColorTag.PLUM,
                details = EventTypeDetails.Trip(
                    transportMode = TransportMode.TRAIN,
                    bookingReference = "PKP-IC-84219",
                    packingChecklist = listOf(
                        "Laptop i ładowarka USB-C",
                        "Bilet w telefonie",
                        "Identyfikator uczestnika",
                    ),
                ),
                reminderTime = ReminderTime.fromHourMinute(7, 30),
                location = null,
                createdAtMillis = now + 4L,
            ),
        )

        val notes = listOf(
            Note(
                id = NoteId.UNASSIGNED,
                title = "Scenariusz prezentacji dema",
                content = "1. Pokazać widok miesiąca, kolorowe kropki i filtrowanie typów.\n" +
                    "2. Przejść na zakładkę Mapa i kliknąć pinezkę wydarzenia.\n" +
                    "3. Wyeksportować wydarzenia do pliku .ics lub .csv.",
                createdAtMillis = now + 30L,
            ),
            Note(
                id = NoteId.UNASSIGNED,
                title = "Założenia architektury SOLID",
                content = "Każda funkcja (Kalendarz, Mapa, Notatki, Transfer) ma własny kontroler MVC, " +
                    "interfejsy portów oraz testy jednostkowe bez zależności od Android Context.",
                createdAtMillis = now + 20L,
            ),
            Note(
                id = NoteId.UNASSIGNED,
                title = "Lista kontrolna przed wydaniem",
                content = "- Zweryfikować uprawnienia powiadomień i GPS\n" +
                    "- Sprawdzić działanie trybu ciemnego\n" +
                    "- Przetestować import polskiego pliku CSV ze średnikami",
                createdAtMillis = now + 10L,
            ),
        )

        return DemoDataset(events = events, notes = notes)
    }

    fun isDemoEvent(event: CalendarEvent): Boolean = event.title in DEMO_EVENT_TITLES

    fun isDemoNote(note: Note): Boolean = note.title in DEMO_NOTE_TITLES

    companion object {
        private val DEMO_EVENT_TITLES = setOf(
            "Prezentacja dema architektury MVC",
            "Przegląd sprintu i testy odbiorcze",
            "Urodziny Kasi",
            "Wizyta kontrolna — Stomatolog",
            "Wyjazd służbowy — Konferencja Kotlin",
        )

        private val DEMO_NOTE_TITLES = setOf(
            "Scenariusz prezentacji dema",
            "Założenia architektury SOLID",
            "Lista kontrolna przed wydaniem",
        )
    }
}
