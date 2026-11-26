package com.example.domain.service

import com.example.domain.model.CalendarEvent
import com.example.domain.model.Note
import java.util.Locale

object EventDeduplicator {

    fun eventFingerprint(event: CalendarEvent): String = buildString {
        append(event.entryType.name)
        append('|')
        append(event.dateKey.value)
        append('|')
        append(event.endDateKey?.value.orEmpty())
        append('|')
        append(event.isAllDay)
        append('|')
        append(event.timeRange?.start?.minutesOfDay ?: -1)
        append('|')
        append(event.timeRange?.end?.minutesOfDay ?: -1)
        append('|')
        append(event.recurrence.code)
        append('|')
        append(event.title.trim().lowercase(Locale.ROOT))
        append('|')
        append(event.description.trim().lowercase(Locale.ROOT))
    }

    fun noteFingerprint(note: Note): String = buildString {
        append(note.title.trim().lowercase(Locale.ROOT))
        append('|')
        append(note.content.trim())
    }

    fun filterNewEvents(
        existing: List<CalendarEvent>,
        incoming: List<CalendarEvent>,
    ): List<CalendarEvent> {
        val seen = existing.mapTo(HashSet(existing.size)) { eventFingerprint(it) }
        val result = ArrayList<CalendarEvent>(incoming.size)
        for (item in incoming) {
            val key = eventFingerprint(item)
            if (seen.add(key)) {
                result.add(item)
            }
        }
        return result
    }

    fun filterNewNotes(
        existing: List<Note>,
        incoming: List<Note>,
    ): List<Note> {
        val seen = existing.mapTo(HashSet(existing.size)) { noteFingerprint(it) }
        val result = ArrayList<Note>(incoming.size)
        for (item in incoming) {
            val key = noteFingerprint(item)
            if (seen.add(key)) {
                result.add(item)
            }
        }
        return result
    }
}
