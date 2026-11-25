package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class ImportedEvent(
    val title: String,
    val dateKey: String,
)

data class ImportedNote(
    val title: String,
    val content: String,
)

data class CsvImportResult(
    val events: List<ImportedEvent> = emptyList(),
    val notes: List<ImportedNote> = emptyList(),
)

object TransferFormats {

    fun exportEventsToIcs(events: List<CalendarEventEntity>): String {
        val dtStamp = formatUtcTimestamp(System.currentTimeMillis())
        val sb = StringBuilder()
        sb.append("BEGIN:VCALENDAR\r\n")
        sb.append("VERSION:2.0\r\n")
        sb.append("PRODID:-//KalendarzReinmar//PL\r\n")
        sb.append("CALSCALE:GREGORIAN\r\n")
        for (event in events) {
            val startCompact = dateKeyToCompact(event.dateKey) ?: continue
            val endCompact = nextDayCompact(event.dateKey) ?: startCompact
            sb.append("BEGIN:VEVENT\r\n")
            sb.append("UID:reinmar-${event.id}-${event.createdAt}@kalendarzreinmar\r\n")
            sb.append("DTSTAMP:$dtStamp\r\n")
            sb.append("DTSTART;VALUE=DATE:$startCompact\r\n")
            sb.append("DTEND;VALUE=DATE:$endCompact\r\n")
            sb.append("SUMMARY:${escapeIcsText(event.title)}\r\n")
            sb.append("END:VEVENT\r\n")
        }
        sb.append("END:VCALENDAR\r\n")
        return sb.toString()
    }

    fun parseEventsFromIcs(raw: String): List<ImportedEvent> {
        val cleaned = raw.removePrefix("\uFEFF")
        val unfolded = cleaned
            .replace("\r\n ", "")
            .replace("\r\n\t", "")
            .replace("\n ", "")
            .replace("\n\t", "")

        val lines = unfolded.split("\r\n", "\n", "\r")
        val result = mutableListOf<ImportedEvent>()

        var inEvent = false
        var currentSummary: String? = null
        var currentDateKey: String? = null

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.equals("BEGIN:VEVENT", ignoreCase = true)) {
                inEvent = true
                currentSummary = null
                currentDateKey = null
                continue
            }
            if (trimmed.equals("END:VEVENT", ignoreCase = true)) {
                if (inEvent && !currentSummary.isNullOrBlank() && !currentDateKey.isNullOrBlank()) {
                    result.add(
                        ImportedEvent(
                            title = currentSummary.trim(),
                            dateKey = currentDateKey,
                        )
                    )
                }
                inEvent = false
                currentSummary = null
                currentDateKey = null
                continue
            }
            if (!inEvent) continue

            val colonIndex = line.indexOf(':')
            if (colonIndex <= 0) continue

            val keyPart = line.substring(0, colonIndex).trim()
            val propertyName = keyPart.substringBefore(';').uppercase(Locale.US)
            val valuePart = line.substring(colonIndex + 1)

            when (propertyName) {
                "SUMMARY" -> currentSummary = unescapeIcsText(valuePart)
                "DTSTART" -> currentDateKey = parseIcsDateToDateKey(valuePart.trim())
            }
        }

        return result
    }

    fun exportEventsToCsv(events: List<CalendarEventEntity>): String {
        val sb = StringBuilder()
        sb.append("date,title\r\n")
        for (event in events) {
            sb.append(escapeCsvField(event.dateKey))
            sb.append(',')
            sb.append(escapeCsvField(event.title))
            sb.append("\r\n")
        }
        return sb.toString()
    }

    fun exportNotesToCsv(notes: List<NoteEntity>): String {
        val sb = StringBuilder()
        sb.append("title,content\r\n")
        for (note in notes) {
            sb.append(escapeCsvField(note.title))
            sb.append(',')
            sb.append(escapeCsvField(note.content))
            sb.append("\r\n")
        }
        return sb.toString()
    }

    fun parseCsv(raw: String, preferNotesWhenAmbiguous: Boolean): CsvImportResult {
        val cleaned = raw.removePrefix("\uFEFF")
        if (cleaned.isBlank()) return CsvImportResult()

        val delimiter = detectDelimiter(cleaned)
        val rows = parseCsvRows(cleaned, delimiter)
            .filter { row -> row.any { it.isNotBlank() } }
        if (rows.isEmpty()) return CsvImportResult()

        val firstRowNormalized = rows.first().map { it.trim().lowercase(Locale.US) }

        val typeIdx = firstRowNormalized.indexOfFirst { it in setOf("type", "typ") }
        val dateIdx = firstRowNormalized.indexOfFirst {
            it in setOf("date", "data", "datekey", "start date", "start_date", "dtstart")
        }
        val titleIdx = firstRowNormalized.indexOfFirst {
            it in setOf("title", "tytul", "tytuł", "subject", "summary", "nazwa")
        }
        val contentIdx = firstRowNormalized.indexOfFirst {
            it in setOf("content", "tresc", "treść", "body", "note", "notatka", "text", "tekst")
        }

        val hasHeader = typeIdx >= 0 || dateIdx >= 0 || titleIdx >= 0 || contentIdx >= 0
        val dataRows = if (hasHeader) rows.drop(1) else rows

        val events = mutableListOf<ImportedEvent>()
        val notes = mutableListOf<ImportedNote>()

        if (hasHeader) {
            for (row in dataRows) {
                val typeValue = row.getOrNull(typeIdx)?.trim()?.lowercase(Locale.US)
                val dateValue = row.getOrNull(dateIdx)?.trim().orEmpty()
                val titleValue = row.getOrNull(titleIdx)?.trim().orEmpty()
                val contentValue = row.getOrNull(contentIdx)?.trim().orEmpty()

                when {
                    typeValue in setOf("event", "wydarzenie") -> {
                        val normalizedDate = normalizeDateString(dateValue)
                        if (normalizedDate != null && titleValue.isNotEmpty()) {
                            events.add(ImportedEvent(title = titleValue, dateKey = normalizedDate))
                        }
                    }
                    typeValue in setOf("note", "notatka") -> {
                        if (contentValue.isNotEmpty()) {
                            notes.add(ImportedNote(title = titleValue, content = contentValue))
                        }
                    }
                    dateIdx >= 0 && titleIdx >= 0 -> {
                        val normalizedDate = normalizeDateString(dateValue)
                        if (normalizedDate != null && titleValue.isNotEmpty()) {
                            events.add(ImportedEvent(title = titleValue, dateKey = normalizedDate))
                        }
                    }
                    contentIdx >= 0 -> {
                        if (contentValue.isNotEmpty()) {
                            notes.add(ImportedNote(title = titleValue, content = contentValue))
                        }
                    }
                }
            }
        } else {
            for (row in dataRows) {
                if (row.size >= 2) {
                    val col0 = row[0].trim()
                    val col1 = row[1].trim()
                    val col0Date = normalizeDateString(col0)
                    val col1Date = normalizeDateString(col1)
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
        }

        return CsvImportResult(events = events, notes = notes)
    }

    private fun detectDelimiter(text: String): Char {
        var inQuotes = false
        var commas = 0
        var semicolons = 0
        for (ch in text) {
            when (ch) {
                '"' -> inQuotes = !inQuotes
                ',' -> if (!inQuotes) commas++
                ';' -> if (!inQuotes) semicolons++
                '\n', '\r' -> if (!inQuotes) break
            }
        }
        return if (semicolons > commas) ';' else ','
    }

    private fun parseCsvRows(text: String, delimiter: Char): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < text.length) {
            val ch = text[i]
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') {
                        currentField.append('"')
                        i += 2
                        continue
                    } else {
                        inQuotes = false
                    }
                } else {
                    currentField.append(ch)
                }
            } else {
                when (ch) {
                    '"' -> inQuotes = true
                    delimiter -> {
                        currentRow.add(currentField.toString())
                        currentField.setLength(0)
                    }
                    '\r' -> {
                        if (i + 1 < text.length && text[i + 1] == '\n') {
                            i++
                        }
                        currentRow.add(currentField.toString())
                        currentField.setLength(0)
                        rows.add(currentRow.toList())
                        currentRow.clear()
                    }
                    '\n' -> {
                        currentRow.add(currentField.toString())
                        currentField.setLength(0)
                        rows.add(currentRow.toList())
                        currentRow.clear()
                    }
                    else -> currentField.append(ch)
                }
            }
            i++
        }

        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString())
            rows.add(currentRow.toList())
        }

        return rows
    }

    private fun escapeCsvField(value: String): String {
        val needsQuotes = value.any { it == ',' || it == ';' || it == '"' || it == '\n' || it == '\r' }
        return if (needsQuotes) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun escapeIcsText(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\r\n", "\\n")
            .replace("\n", "\\n")
            .replace("\r", "\\n")
    }

    private fun unescapeIcsText(value: String): String {
        val sb = StringBuilder(value.length)
        var i = 0
        while (i < value.length) {
            val ch = value[i]
            if (ch == '\\' && i + 1 < value.length) {
                when (val next = value[i + 1]) {
                    'n', 'N' -> sb.append('\n')
                    '\\', ',', ';' -> sb.append(next)
                    else -> sb.append(next)
                }
                i += 2
            } else {
                sb.append(ch)
                i++
            }
        }
        return sb.toString()
    }

    private fun parseIcsDateToDateKey(value: String): String? {
        val datePart = value.substringBefore('T').substringBefore('t').trim()
        return normalizeDateString(datePart)
    }

    internal fun normalizeDateString(raw: String): String? {
        val value = raw.trim().substringBefore('T').substringBefore(' ')
        if (value.isEmpty()) return null

        val compactRegex = Regex("^(\\d{4})(\\d{2})(\\d{2})$")
        compactRegex.matchEntire(value)?.let { m ->
            return validateDate(
                m.groupValues[1].toInt(),
                m.groupValues[2].toInt(),
                m.groupValues[3].toInt(),
            )
        }

        val isoRegex = Regex("^(\\d{4})[-/.](\\d{1,2})[-/.](\\d{1,2})$")
        isoRegex.matchEntire(value)?.let { m ->
            return validateDate(
                m.groupValues[1].toInt(),
                m.groupValues[2].toInt(),
                m.groupValues[3].toInt(),
            )
        }

        val dmyRegex = Regex("^(\\d{1,2})[-/.](\\d{1,2})[-/.](\\d{4})$")
        dmyRegex.matchEntire(value)?.let { m ->
            return validateDate(
                m.groupValues[3].toInt(),
                m.groupValues[2].toInt(),
                m.groupValues[1].toInt(),
            )
        }

        return null
    }

    private fun validateDate(year: Int, month: Int, day: Int): String? {
        if (year !in 1900..2999 || month !in 1..12 || day !in 1..31) return null
        return try {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                isLenient = false
                clear()
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, day)
                timeInMillis
            }
            if (cal.get(Calendar.YEAR) == year &&
                cal.get(Calendar.MONTH) == month - 1 &&
                cal.get(Calendar.DAY_OF_MONTH) == day
            ) {
                String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
            } else {
                null
            }
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun dateKeyToCompact(dateKey: String): String? {
        val normalized = normalizeDateString(dateKey) ?: return null
        return normalized.replace("-", "")
    }

    private fun nextDayCompact(dateKey: String): String? {
        val normalized = normalizeDateString(dateKey) ?: return null
        val parts = normalized.split("-")
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(Calendar.YEAR, parts[0].toInt())
            set(Calendar.MONTH, parts[1].toInt() - 1)
            set(Calendar.DAY_OF_MONTH, parts[2].toInt())
            add(Calendar.DAY_OF_MONTH, 1)
        }
        return String.format(
            Locale.US,
            "%04d%02d%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
        )
    }

    private fun formatUtcTimestamp(millis: Long): String {
        val formatter = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return formatter.format(Date(millis))
    }
}
