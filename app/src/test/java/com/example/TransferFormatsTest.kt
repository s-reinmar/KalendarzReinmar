package com.example

import com.example.data.transfer.CsvTransferCodec
import com.example.data.transfer.IcsEventCodec
import com.example.domain.model.CalendarEvent
import com.example.domain.model.DateKey
import com.example.domain.model.EventId
import com.example.domain.model.Note
import com.example.domain.model.NoteId
import com.example.fakes.FakeClockProvider
import org.junit.Assert.assertEquals
import org.junit.Test

class TransferFormatsTest {

    private val icsCodec = IcsEventCodec(FakeClockProvider())
    private val csvCodec = CsvTransferCodec()

    @Test
    fun icsRoundTripPreservesEventsAndEscaping() {
        val events = listOf(
            CalendarEvent(
                id = EventId(1),
                title = "Spotkanie, zespół; Q4",
                dateKey = requireNotNull(DateKey.parseOrNull("2026-11-25")),
                createdAtMillis = 1000L,
            ),
            CalendarEvent(
                id = EventId(2),
                title = "Wizyta\\kontrola",
                dateKey = requireNotNull(DateKey.parseOrNull("2026-12-31")),
                createdAtMillis = 2000L,
            ),
        )
        val ics = icsCodec.export(events)
        val parsed = icsCodec.parse(ics)

        assertEquals(2, parsed.size)
        assertEquals("Spotkanie, zespół; Q4", parsed[0].title)
        assertEquals("2026-11-25", parsed[0].dateKey.value)
        assertEquals("Wizyta\\kontrola", parsed[1].title)
        assertEquals("2026-12-31", parsed[1].dateKey.value)
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

        val parsed = icsCodec.parse(raw)
        assertEquals(1, parsed.size)
        assertEquals("2026-11-30", parsed[0].dateKey.value)
        assertEquals("Długa nazwa wydarzenia", parsed[0].title)
    }

    @Test
    fun csvEventsRoundTripAndSemicolonImport() {
        val events = listOf(
            CalendarEvent(
                id = EventId(1),
                title = "Rozmowa, \"ważna\"",
                dateKey = requireNotNull(DateKey.parseOrNull("2026-11-25")),
            ),
        )
        val csv = csvCodec.exportEvents(events)
        val parsed = csvCodec.parse(csv, preferNotesWhenAmbiguous = false)
        assertEquals(1, parsed.events.size)
        assertEquals("2026-11-25", parsed.events[0].dateKey.value)
        assertEquals("Rozmowa, \"ważna\"", parsed.events[0].title)

        val polishExcelCsv = "\uFEFFdata;tytuł\r\n25.11.2026;Przegląd\r\n"
        val parsedPl = csvCodec.parse(polishExcelCsv, preferNotesWhenAmbiguous = false)
        assertEquals(1, parsedPl.events.size)
        assertEquals("2026-11-25", parsedPl.events[0].dateKey.value)
        assertEquals("Przegląd", parsedPl.events[0].title)
    }

    @Test
    fun csvNotesRoundTripWithMultilineContent() {
        val notes = listOf(
            Note(
                id = NoteId(1),
                title = "Lista",
                content = "Punkt 1\nPunkt 2, \"cytat\"",
            ),
        )
        val csv = csvCodec.exportNotes(notes)
        val parsed = csvCodec.parse(csv, preferNotesWhenAmbiguous = true)
        assertEquals(1, parsed.notes.size)
        assertEquals("Lista", parsed.notes[0].title)
        assertEquals("Punkt 1\nPunkt 2, \"cytat\"", parsed.notes[0].content)
    }
}
