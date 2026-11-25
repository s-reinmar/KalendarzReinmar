package com.example.domain.service

import com.example.domain.model.CalendarEvent
import com.example.domain.model.EventId
import com.example.domain.model.EventLocation
import com.example.domain.model.GeoPoint
import com.example.domain.model.Note
import com.example.domain.model.NoteId
import com.example.domain.model.ReminderTime
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
                reminderTime = ReminderTime.fromHourMinute(10, 0),
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
                reminderTime = ReminderTime.fromHourMinute(14, 30),
                location = EventLocation(
                    point = requireNotNull(GeoPoint.createOrNull(50.06194, 19.93685)),
                    label = "Kraków, Rynek Główny",
                ),
                createdAtMillis = now + 1L,
            ),
            CalendarEvent(
                id = EventId.UNASSIGNED,
                title = "Warsztaty czystego kodu SOLID",
                dateKey = baseDate.plusDays(3),
                reminderTime = ReminderTime.fromHourMinute(9, 15),
                location = EventLocation(
                    point = requireNotNull(GeoPoint.createOrNull(51.10929, 17.03208)),
                    label = "Wrocław, Rynek",
                ),
                createdAtMillis = now + 2L,
            ),
            CalendarEvent(
                id = EventId.UNASSIGNED,
                title = "Spotkanie z klientem — wdrożenie",
                dateKey = baseDate.plusDays(6),
                reminderTime = ReminderTime.fromHourMinute(11, 0),
                location = EventLocation(
                    point = requireNotNull(GeoPoint.createOrNull(54.35202, 18.64663)),
                    label = "Gdańsk, Długi Targ",
                ),
                createdAtMillis = now + 3L,
            ),
            CalendarEvent(
                id = EventId.UNASSIGNED,
                title = "Eksport kalendarza i archiwizacja",
                dateKey = baseDate.plusDays(9),
                reminderTime = null,
                location = null,
                createdAtMillis = now + 4L,
            ),
        )

        val notes = listOf(
            Note(
                id = NoteId.UNASSIGNED,
                title = "Scenariusz prezentacji dema",
                content = "1. Pokazać widok miesiąca i filtrowanie dnia.\n" +
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
            "Warsztaty czystego kodu SOLID",
            "Spotkanie z klientem — wdrożenie",
            "Eksport kalendarza i archiwizacja",
        )

        private val DEMO_NOTE_TITLES = setOf(
            "Scenariusz prezentacji dema",
            "Założenia architektury SOLID",
            "Lista kontrolna przed wydaniem",
        )
    }
}
