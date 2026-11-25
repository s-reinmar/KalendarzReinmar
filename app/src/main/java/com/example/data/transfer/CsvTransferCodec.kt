package com.example.data.transfer

import com.example.domain.model.CalendarEvent
import com.example.domain.model.CsvImportResult
import com.example.domain.model.DateKey
import com.example.domain.model.ImportedEvent
import com.example.domain.model.ImportedNote
import com.example.domain.model.Note
import java.util.Locale

class CsvTransferCodec {

    fun exportEvents(events: List<CalendarEvent>): String {
        val sb = StringBuilder()
        sb.append("date,title\r\n")
        for (event in events) {
            sb.append(CsvRowParser.escapeField(event.dateKey.value))
            sb.append(',')
            sb.append(CsvRowParser.escapeField(event.title))
            sb.append("\r\n")
        }
        return sb.toString()
    }

    fun exportNotes(notes: List<Note>): String {
        val sb = StringBuilder()
        sb.append("title,content\r\n")
        for (note in notes) {
            sb.append(CsvRowParser.escapeField(note.title))
            sb.append(',')
            sb.append(CsvRowParser.escapeField(note.content))
            sb.append("\r\n")
        }
        return sb.toString()
    }

    fun parse(raw: String, preferNotesWhenAmbiguous: Boolean): CsvImportResult {
        val cleaned = raw.removePrefix("\uFEFF")
        if (cleaned.isBlank()) return CsvImportResult()

        val delimiter = CsvRowParser.detectDelimiter(cleaned)
        val rows = CsvRowParser.parseRows(cleaned, delimiter)
            .filter { row -> row.any { it.isNotBlank() } }
        if (rows.isEmpty()) return CsvImportResult()

        val firstRowNormalized = rows.first().map { it.trim().lowercase(Locale.US) }
        val columns = HeaderColumns(
            typeIdx = firstRowNormalized.indexOfFirst { it in TYPE_HEADERS },
            dateIdx = firstRowNormalized.indexOfFirst { it in DATE_HEADERS },
            titleIdx = firstRowNormalized.indexOfFirst { it in TITLE_HEADERS },
            contentIdx = firstRowNormalized.indexOfFirst { it in CONTENT_HEADERS },
        )

        return if (columns.hasHeader) {
            parseHeaderedRows(rows.drop(1), columns)
        } else {
            parseHeaderlessRows(rows, preferNotesWhenAmbiguous)
        }
    }

    private fun parseHeaderedRows(rows: List<List<String>>, columns: HeaderColumns): CsvImportResult {
        val events = mutableListOf<ImportedEvent>()
        val notes = mutableListOf<ImportedNote>()

        for (row in rows) {
            val typeValue = row.getOrNull(columns.typeIdx)?.trim()?.lowercase(Locale.US)
            val dateValue = row.getOrNull(columns.dateIdx)?.trim().orEmpty()
            val titleValue = row.getOrNull(columns.titleIdx)?.trim().orEmpty()
            val contentValue = row.getOrNull(columns.contentIdx)?.trim().orEmpty()

            when {
                typeValue in EVENT_TYPES -> {
                    val dateKey = DateKey.parseOrNull(dateValue)
                    if (dateKey != null && titleValue.isNotEmpty()) {
                        events.add(ImportedEvent(title = titleValue, dateKey = dateKey))
                    }
                }
                typeValue in NOTE_TYPES -> {
                    if (contentValue.isNotEmpty()) {
                        notes.add(ImportedNote(title = titleValue, content = contentValue))
                    }
                }
                columns.dateIdx >= 0 && columns.titleIdx >= 0 -> {
                    val dateKey = DateKey.parseOrNull(dateValue)
                    if (dateKey != null && titleValue.isNotEmpty()) {
                        events.add(ImportedEvent(title = titleValue, dateKey = dateKey))
                    }
                }
                columns.contentIdx >= 0 -> {
                    if (contentValue.isNotEmpty()) {
                        notes.add(ImportedNote(title = titleValue, content = contentValue))
                    }
                }
            }
        }
        return CsvImportResult(events = events, notes = notes)
    }

    private fun parseHeaderlessRows(
        rows: List<List<String>>,
        preferNotesWhenAmbiguous: Boolean,
    ): CsvImportResult {
        val events = mutableListOf<ImportedEvent>()
        val notes = mutableListOf<ImportedNote>()

        for (row in rows) {
            if (row.size >= 2) {
                val col0 = row[0].trim()
                val col1 = row[1].trim()
                val col0Date = DateKey.parseOrNull(col0)
                val col1Date = DateKey.parseOrNull(col1)
                when {
                    col0Date != null && col1.isNotEmpty() -> {
                        events.add(ImportedEvent(title = col1, dateKey = col0Date))
                    }
                    col1Date != null && col0.isNotEmpty() -> {
                        events.add(ImportedEvent(title = col0, dateKey = col1Date))
                    }
                    preferNotesWhenAmbiguous && col1.isNotEmpty() -> {
                        notes.add(ImportedNote(title = col0, content = col1))
                    }
                    preferNotesWhenAmbiguous && col0.isNotEmpty() -> {
                        notes.add(ImportedNote(title = "", content = col0))
                    }
                }
            } else if (row.size == 1 && preferNotesWhenAmbiguous) {
                val text = row[0].trim()
                if (text.isNotEmpty()) {
                    notes.add(ImportedNote(title = "", content = text))
                }
            }
        }
        return CsvImportResult(events = events, notes = notes)
    }

    private data class HeaderColumns(
        val typeIdx: Int,
        val dateIdx: Int,
        val titleIdx: Int,
        val contentIdx: Int,
    ) {
        val hasHeader: Boolean
            get() = typeIdx >= 0 || dateIdx >= 0 || titleIdx >= 0 || contentIdx >= 0
    }

    private companion object {
        val TYPE_HEADERS = setOf("type", "typ")
        val DATE_HEADERS = setOf("date", "data", "datekey", "start date", "start_date", "dtstart")
        val TITLE_HEADERS = setOf("title", "tytul", "tytuł", "subject", "summary", "nazwa")
        val CONTENT_HEADERS = setOf("content", "tresc", "treść", "body", "note", "notatka", "text", "tekst")
        val EVENT_TYPES = setOf("event", "wydarzenie")
        val NOTE_TYPES = setOf("note", "notatka")
    }
}
