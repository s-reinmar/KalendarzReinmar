package com.example.fakes

import com.example.domain.model.CalendarDay
import com.example.domain.model.CalendarEvent
import com.example.domain.model.DateKey
import com.example.domain.model.DeviceLocationResult
import com.example.domain.model.EventId
import com.example.domain.model.EventLocation
import com.example.domain.model.GeoPoint
import com.example.domain.model.GeocodingResult
import com.example.domain.model.Note
import com.example.domain.model.NoteId
import com.example.domain.model.ReminderTime
import com.example.domain.port.ClockProvider
import com.example.domain.port.DeviceLocationProvider
import com.example.domain.port.DocumentStoragePort
import com.example.domain.port.EventRepository
import com.example.domain.port.GeocodingService
import com.example.domain.port.NoteRepository
import com.example.domain.port.ReminderPort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakeClockProvider(
    private val fixedDay: CalendarDay = CalendarDay(2026, 11, 25),
    private val fixedMillis: Long = 1_764_000_000_000L,
) : ClockProvider {
    override fun today(): CalendarDay = fixedDay
    override fun nowMillis(): Long = fixedMillis
}

class FakeEventRepository : EventRepository {
    private val eventsFlow = MutableStateFlow<List<CalendarEvent>>(emptyList())
    private var nextId = 1L

    override fun observeEvents(): Flow<List<CalendarEvent>> = eventsFlow.asStateFlow()

    override suspend fun getEventsWithReminders(): List<CalendarEvent> =
        eventsFlow.value.filter { it.hasReminder }

    override suspend fun addEvent(
        title: String,
        dateKey: DateKey,
        reminderTime: ReminderTime?,
        location: EventLocation?,
    ): CalendarEvent? {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return null
        val created = CalendarEvent(
            id = EventId(nextId++),
            title = trimmed,
            dateKey = dateKey,
            reminderTime = reminderTime,
            location = location,
            createdAtMillis = nextId * 1000L,
        )
        eventsFlow.update { (it + created).sortedBy { e -> e.dateKey.value } }
        return created
    }

    override suspend fun updateEvent(
        event: CalendarEvent,
        title: String,
        dateKey: DateKey,
        reminderTime: ReminderTime?,
        location: EventLocation?,
    ): CalendarEvent? {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return null
        val updated = event.copy(
            title = trimmed,
            dateKey = dateKey,
            reminderTime = reminderTime,
            location = location,
        )
        eventsFlow.update { list -> list.map { if (it.id == event.id) updated else it } }
        return updated
    }

    override suspend fun updateEventReminder(
        eventId: EventId,
        reminderTime: ReminderTime?,
        location: EventLocation?,
    ) {
        eventsFlow.update { list ->
            list.map { event ->
                if (event.id == eventId) {
                    event.copy(reminderTime = reminderTime, location = location)
                } else {
                    event
                }
            }
        }
    }

    override suspend fun deleteEvent(eventId: EventId) {
        eventsFlow.update { list -> list.filterNot { it.id == eventId } }
    }

    override suspend fun addEvents(events: List<CalendarEvent>): List<CalendarEvent> {
        val inserted = events.mapNotNull { event ->
            val trimmed = event.title.trim()
            if (trimmed.isEmpty()) {
                null
            } else {
                event.copy(id = EventId(nextId++), title = trimmed)
            }
        }
        if (inserted.isNotEmpty()) {
            eventsFlow.update { (it + inserted).sortedBy { e -> e.dateKey.value } }
        }
        return inserted
    }
}

class FakeNoteRepository : NoteRepository {
    private val notesFlow = MutableStateFlow<List<Note>>(emptyList())
    private var nextId = 1L

    override fun observeNotes(): Flow<List<Note>> = notesFlow.asStateFlow()

    override suspend fun addNote(title: String, content: String): Boolean {
        val trimmedContent = content.trim()
        if (trimmedContent.isEmpty()) return false
        val note = Note(
            id = NoteId(nextId++),
            title = title.trim(),
            content = trimmedContent,
            createdAtMillis = nextId * 1000L,
        )
        notesFlow.update { listOf(note) + it }
        return true
    }

    override suspend fun updateNote(note: Note, title: String, content: String): Boolean {
        val trimmedContent = content.trim()
        if (trimmedContent.isEmpty()) return false
        val updated = note.copy(title = title.trim(), content = trimmedContent)
        notesFlow.update { list -> list.map { if (it.id == note.id) updated else it } }
        return true
    }

    override suspend fun deleteNote(noteId: NoteId) {
        notesFlow.update { list -> list.filterNot { it.id == noteId } }
    }

    override suspend fun addNotes(notes: List<Note>): Int {
        val valid = notes.mapNotNull { note ->
            val trimmed = note.content.trim()
            if (trimmed.isEmpty()) null else note.copy(id = NoteId(nextId++), title = note.title.trim(), content = trimmed)
        }
        if (valid.isNotEmpty()) {
            notesFlow.update { valid + it }
        }
        return valid.size
    }
}

class FakeReminderPort : ReminderPort {
    val scheduledEvents = mutableListOf<CalendarEvent>()
    val cancelledEventIds = mutableListOf<EventId>()
    var channelEnsured = false

    override fun ensureChannel() {
        channelEnsured = true
    }

    override fun schedule(event: CalendarEvent) {
        scheduledEvents.add(event)
    }

    override fun cancel(eventId: EventId) {
        cancelledEventIds.add(eventId)
    }
}

class FakeGeocodingService(
    var searchResult: Result<List<GeocodingResult>> = Result.success(emptyList()),
    var reverseResult: Result<String?> = Result.success(null),
) : GeocodingService {
    override suspend fun search(query: String): Result<List<GeocodingResult>> = searchResult
    override suspend fun reverseGeocode(point: GeoPoint): Result<String?> = reverseResult
}

class FakeDeviceLocationProvider(
    var permissionGranted: Boolean = true,
    var locationResult: DeviceLocationResult = DeviceLocationResult.Unavailable,
) : DeviceLocationProvider {
    override fun hasLocationPermission(): Boolean = permissionGranted
    override suspend fun fetchCurrentLocation(): DeviceLocationResult = locationResult
}

class FakeDocumentStorage : DocumentStoragePort {
    val files = mutableMapOf<String, String>()
    var failReads = false
    var failWrites = false

    override suspend fun readText(documentUri: String): Result<String> {
        if (failReads) return Result.failure(IllegalStateException("Read failed"))
        val content = files[documentUri]
            ?: return Result.failure(IllegalArgumentException("Missing $documentUri"))
        return Result.success(content)
    }

    override suspend fun writeText(documentUri: String, content: String): Result<Unit> {
        if (failWrites) return Result.failure(IllegalStateException("Write failed"))
        files[documentUri] = content
        return Result.success(Unit)
    }
}
