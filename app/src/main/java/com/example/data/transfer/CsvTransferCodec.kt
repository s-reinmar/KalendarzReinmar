package com.example.data.transfer

import com.example.domain.model.CalendarEntryType
import com.example.domain.model.CalendarEvent
import com.example.domain.model.CsvImportResult
import com.example.domain.model.DateKey
import com.example.domain.model.EventRecurrence
import com.example.domain.model.EventTimeRange
import com.example.domain.model.ImportedEvent
import com.example.domain.model.ImportedNote
import com.example.domain.model.Note
import com.example.domain.model.TimeOfDay
import java.util.Locale

class CsvTransferCodec {

    fun exportEvents(events: List<CalendarEvent>): String {
        val sb = StringBuilder()
        sb.append("date,end_date,entry_type,title,all_day,start_time,end_time,recurrence,description\r\n")
        for (event in events) {
            sb.append(CsvRowParser.escapeField(event.dateKey.value))
            sb.append(',')
            sb.append(CsvRowParser.escapeField(event.endDateKey?.value.orEmpty()))
            sb.append(',')
            sb.append(CsvRowParser.escapeField(event.entryType.code))
            sb.append(',')
            sb.append(CsvRowParser.escapeField(event.title))
            sb.append(',')
            sb.append(if (event.isAllDay) "true" else "false")
            sb.append(',')
            sb.append(CsvRowParser.escapeField(event.timeRange?.start?.formatHHmm().orEmpty()))
            sb.append(',')
            sb.append(CsvRowParser.escapeField(event.timeRange?.end?.formatHHmm().orEmpty()))
            sb.append(',')
            sb.append(CsvRowParser.escapeField(event.recurrence.code))
            sb.append(',')
            sb.append(CsvRowParser.escapeField(event.description))
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
            entryTypeIdx = firstRowNormalized.indexOfFirst { it in ENTRY_TYPE_HEADERS },
            dateIdx = firstRowNormalized.indexOfFirst { it in DATE_HEADERS },
            endDateIdx = firstRowNormalized.indexOfFirst { it in END_DATE_HEADERS },
            titleIdx = firstRowNormalized.indexOfFirst { it in TITLE_HEADERS },
            contentIdx = firstRowNormalized.indexOfFirst { it in CONTENT_HEADERS },
            allDayIdx = firstRowNormalized.indexOfFirst { it in ALL_DAY_HEADERS },
            startTimeIdx = firstRowNormalized.indexOfFirst { it in START_TIME_HEADERS },
            endTimeIdx = firstRowNormalized.indexOfFirst { it in END_TIME_HEADERS },
            recurrenceIdx = firstRowNormalized.indexOfFirst { it in RECURRENCE_HEADERS },
            descriptionIdx = firstRowNormalized.indexOfFirst { it in DESCRIPTION_HEADERS },
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
                    buildImportedEvent(row, columns, dateValue, titleValue)?.let(events::add)
                }
                typeValue in NOTE_TYPES -> {
                    if (contentValue.isNotEmpty()) {
                        notes.add(ImportedNote(title = titleValue, content = contentValue))
                    }
                }
                columns.dateIdx >= 0 && columns.titleIdx >= 0 -> {
                    buildImportedEvent(row, columns, dateValue, titleValue)?.let(events::add)
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

    private fun buildImportedEvent(
        row: List<String>,
        columns: HeaderColumns,
        dateValue: String,
        titleValue: String,
    ): ImportedEvent? {
        val dateKey = DateKey.parseOrNull(dateValue) ?: return null
        if (titleValue.isEmpty()) return null

        val rawEntryType = row.getOrNull(columns.entryTypeIdx)
        val entryType = CalendarEntryType.fromCodeOrDefault(rawEntryType)
        val endDateKey = row.getOrNull(columns.endDateIdx)?.let(DateKey::parseOrNull)
        val startTime = row.getOrNull(columns.startTimeIdx)?.let(TimeOfDay::parseOrNull)
        val endTime = row.getOrNull(columns.endTimeIdx)?.let(TimeOfDay::parseOrNull)
        val allDayRaw = row.getOrNull(columns.allDayIdx)?.trim()?.lowercase(Locale.US)
        val isAllDay = when (allDayRaw) {
            "false", "0", "nie", "no" -> false
            "true", "1", "tak", "yes" -> true
            else -> startTime == null
        }
        val timeRange = if (!isAllDay && startTime != null) {
            EventTimeRange(start = startTime, end = endTime).normalized()
        } else {
            null
        }
        val recurrence = EventRecurrence.fromCodeOrDefault(row.getOrNull(columns.recurrenceIdx))
        val description = row.getOrNull(columns.descriptionIdx)?.trim().orEmpty()

        return ImportedEvent(
            title = titleValue,
            dateKey = dateKey,
            endDateKey = endDateKey,
            entryType = entryType,
            isAllDay = isAllDay,
            timeRange = timeRange,
            description = description,
            recurrence = recurrence,
        )
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
        val entryTypeIdx: Int,
        val dateIdx: Int,
        val endDateIdx: Int,
        val titleIdx: Int,
        val contentIdx: Int,
        val allDayIdx: Int,
        val startTimeIdx: Int,
        val endTimeIdx: Int,
        val recurrenceIdx: Int,
        val descriptionIdx: Int,
    ) {
        val hasHeader: Boolean
            get() = typeIdx >= 0 || dateIdx >= 0 || titleIdx >= 0 || contentIdx >= 0
    }

    private companion object {
        val TYPE_HEADERS = setOf("type", "typ")
        val ENTRY_TYPE_HEADERS = setOf("entry_type", "category", "kategoria", "rodzaj")
        val DATE_HEADERS = setOf("date", "data", "datekey", "start date", "start_date", "dtstart")
        val END_DATE_HEADERS = setOf("end_date", "end date", "data_konca", "dtend")
        val TITLE_HEADERS = setOf("title", "tytul", "tytuł", "subject", "summary", "nazwa")
        val CONTENT_HEADERS = setOf("content", "tresc", "treść", "body", "note", "notatka", "text", "tekst")
        val ALL_DAY_HEADERS = setOf("all_day", "all day", "caly_dzien", "cały_dzień")
        val START_TIME_HEADERS = setOf("start_time", "start time", "godzina_od")
        val END_TIME_HEADERS = setOf("end_time", "end time", "godzina_do")
        val RECURRENCE_HEADERS = setOf("recurrence", "repeat", "powtarzanie")
        val DESCRIPTION_HEADERS = setOf("description", "opis")
        val EVENT_TYPES = setOf("event", "wydarzenie")
        val NOTE_TYPES = setOf("note", "notatka")
    }
}
