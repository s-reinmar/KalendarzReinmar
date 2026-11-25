package com.example

import com.example.data.CalendarEventEntity
import com.example.data.NoteEntity
import com.example.data.TransferFormats
import org.junit.Assert.assertEquals
import org.junit.Test

class TransferFormatsTest {

    @Test
    fun icsRoundTripPreservesEventsAndEscaping() {
        val events = listOf(
            CalendarEventEntity(id = 1, title = "Spotkanie, zespół; Q4", dateKey = "2026-11-25", createdAt = 1000L),
            CalendarEventEntity(id = 2, title = "Wizyta\\kontrola", dateKey = "2026-12-31", createdAt = 2000L),
        )
        val ics = TransferFormats.exportEventsToIcs(events)
        val parsed = TransferFormats.parseEventsFromIcs(ics)

        assertEquals(2, parsed.size)
        assertEquals("Spotkanie, zespół; Q4", parsed[0].title)
        assertEquals("2026-11-25", parsed[0].dateKey)
        assertEquals("Wizyta\\kontrola", parsed[1].title)
        assertEquals("2026-12-31", parsed[1].dateKey)
    }

    @Test
    fun icsParsesFoldedLinesAndDateTimeFormats() {
        val raw = "\uFEFFBEGIN:VCALENDAR\r\n" +
            "VERSION:2.0\r\n" +
            "BEGIN:VEVENT\r\n" +
            "DTSTART;TZID=Europe/Warsaw:20261130T093000\r\n" +
            "SUMMARY:Długa nazwa \r\n wydarzenia\r\n" +
            "END:VEVENT\r\n" +
            "END:VCALENDAR\r\n"

        val parsed = TransferFormats.parseEventsFromIcs(raw)
        assertEquals(1, parsed.size)
        assertEquals("2026-11-30", parsed[0].dateKey)
        assertEquals("Długa nazwa wydarzenia", parsed[0].title)
    }

    @Test
    fun csvEventsRoundTripAndSemicolonImport() {
        val events = listOf(
            CalendarEventEntity(id = 1, title = "Rozmowa, \"ważna\"", dateKey = "2026-11-25"),
        )
        val csv = TransferFormats.exportEventsToCsv(events)
        val parsed = TransferFormats.parseCsv(csv, preferNotesWhenAmbiguous = false)
        assertEquals(1, parsed.events.size)
        assertEquals("2026-11-25", parsed.events[0].dateKey)
        assertEquals("Rozmowa, \"ważna\"", parsed.events[0].title)

        val polishExcelCsv = "\uFEFFdata;tytuł\r\n25.11.2026;Przegląd\r\n"
        val parsedPl = TransferFormats.parseCsv(polishExcelCsv, preferNotesWhenAmbiguous = false)
        assertEquals(1, parsedPl.events.size)
        assertEquals("2026-11-25", parsedPl.events[0].dateKey)
        assertEquals("Przegląd", parsedPl.events[0].title)
    }

    @Test
    fun csvNotesRoundTripWithMultilineContent() {
        val notes = listOf(
            NoteEntity(id = 1, title = "Lista", content = "Punkt 1\nPunkt 2, \"cytat\""),
        )
        val csv = TransferFormats.exportNotesToCsv(notes)
        val parsed = TransferFormats.parseCsv(csv, preferNotesWhenAmbiguous = true)
        assertEquals(1, parsed.notes.size)
        assertEquals("Lista", parsed.notes[0].title)
        assertEquals("Punkt 1\nPunkt 2, \"cytat\"", parsed.notes[0].content)
    }
}
