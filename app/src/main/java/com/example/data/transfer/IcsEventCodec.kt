package com.example.data.transfer

import com.example.domain.model.CalendarEvent
import com.example.domain.model.DateKey
import com.example.domain.model.ImportedEvent
import com.example.domain.port.ClockProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class IcsEventCodec(
    private val clock: ClockProvider,
) {
    fun export(events: List<CalendarEvent>): String {
        val dtStamp = formatUtcTimestamp(clock.nowMillis())
        val sb = StringBuilder()
        sb.append("BEGIN:VCALENDAR\r\n")
        sb.append("VERSION:2.0\r\n")
        sb.append("PRODID:-//KalendarzReinmar//PL\r\n")
        sb.append("CALSCALE:GREGORIAN\r\n")
        for (event in events) {
            val normalizedDate = DateKey.parseOrNull(event.dateKey.value) ?: continue
            val startCompact = normalizedDate.toCompactString()
            val endCompact = normalizedDate.nextDay().toCompactString()
            sb.append("BEGIN:VEVENT\r\n")
            sb.append("UID:reinmar-${event.id.value}-${event.createdAtMillis}@kalendarzreinmar\r\n")
            sb.append("DTSTAMP:$dtStamp\r\n")
            sb.append("DTSTART;VALUE=DATE:$startCompact\r\n")
            sb.append("DTEND;VALUE=DATE:$endCompact\r\n")
            sb.append("SUMMARY:${escapeText(event.title)}\r\n")
            sb.append("END:VEVENT\r\n")
        }
        sb.append("END:VCALENDAR\r\n")
        return sb.toString()
    }

    fun parse(raw: String): List<ImportedEvent> {
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
        var currentDateKey: DateKey? = null

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.equals("BEGIN:VEVENT", ignoreCase = true)) {
                inEvent = true
                currentSummary = null
                currentDateKey = null
                continue
            }
            if (trimmed.equals("END:VEVENT", ignoreCase = true)) {
                val summary = currentSummary?.trim()
                val dateKey = currentDateKey
                if (inEvent && !summary.isNullOrEmpty() && dateKey != null) {
                    result.add(ImportedEvent(title = summary, dateKey = dateKey))
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
                "SUMMARY" -> currentSummary = unescapeText(valuePart)
                "DTSTART" -> currentDateKey = DateKey.parseOrNull(valuePart)
            }
        }

        return result
    }

    private fun escapeText(value: String): String = value
        .replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace("\r\n", "\\n")
        .replace("\n", "\\n")
        .replace("\r", "\\n")

    private fun unescapeText(value: String): String {
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

    private fun formatUtcTimestamp(millis: Long): String {
        val formatter = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return formatter.format(Date(millis))
    }
}
