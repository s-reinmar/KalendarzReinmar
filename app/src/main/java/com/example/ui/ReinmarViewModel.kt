package com.example.ui

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CalendarEventEntity
import com.example.data.NoteEntity
import com.example.data.ReinmarRepository
import com.example.data.TransferFormats
import com.example.reminder.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface TransferFeedback {
    data class ImportedEvents(val count: Int) : TransferFeedback
    data class ImportedNotes(val count: Int) : TransferFeedback
    data class ImportedBoth(val eventsCount: Int, val notesCount: Int) : TransferFeedback
    data class ExportedEvents(val count: Int) : TransferFeedback
    data class ExportedNotes(val count: Int) : TransferFeedback
    data object NothingToImport : TransferFeedback
    data object Error : TransferFeedback
}

data class CalendarControlState(
    val displayedYear: Int,
    val displayedMonth: Int,
    val selectedDateKey: String? = null,
)

data class ReinmarUiState(
    val events: List<CalendarEventEntity> = emptyList(),
    val notes: List<NoteEntity> = emptyList(),
    val displayedYear: Int = DateUtils.today().year,
    val displayedMonth: Int = DateUtils.today().month,
    val selectedDateKey: String? = null,
    val todayDateKey: String = DateUtils.today().dateKey,
) {
    val eventDates: Set<String>
        get() = events.mapTo(HashSet()) { it.dateKey }

    val visibleEvents: List<CalendarEventEntity>
        get() = if (selectedDateKey == null) {
            events
        } else {
            events.filter { it.dateKey == selectedDateKey }
        }
}

class ReinmarViewModel(
    private val repository: ReinmarRepository,
) : ViewModel() {

    private val today = DateUtils.today()

    private val calendarControl = MutableStateFlow(
        CalendarControlState(
            displayedYear = today.year,
            displayedMonth = today.month,
            selectedDateKey = null,
        )
    )

    val uiState: StateFlow<ReinmarUiState> = combine(
        repository.events,
        repository.notes,
        calendarControl,
    ) { events, notes, control ->
        ReinmarUiState(
            events = events,
            notes = notes,
            displayedYear = control.displayedYear,
            displayedMonth = control.displayedMonth,
            selectedDateKey = control.selectedDateKey,
            todayDateKey = today.dateKey,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReinmarUiState(
            displayedYear = today.year,
            displayedMonth = today.month,
            todayDateKey = today.dateKey,
        ),
    )

    fun selectDate(dateKey: String?) {
        calendarControl.update { current ->
            val nextSelection = if (dateKey != null && current.selectedDateKey == dateKey) {
                null
            } else {
                dateKey
            }
            current.copy(selectedDateKey = nextSelection)
        }
    }

    fun previousMonth() {
        calendarControl.update { current ->
            if (current.displayedMonth == 1) {
                current.copy(displayedYear = current.displayedYear - 1, displayedMonth = 12)
            } else {
                current.copy(displayedMonth = current.displayedMonth - 1)
            }
        }
    }

    fun nextMonth() {
        calendarControl.update { current ->
            if (current.displayedMonth == 12) {
                current.copy(displayedYear = current.displayedYear + 1, displayedMonth = 1)
            } else {
                current.copy(displayedMonth = current.displayedMonth + 1)
            }
        }
    }

    fun addEvent(
        context: Context,
        title: String,
        dateKey: String,
        reminderTimeMinutes: Int? = null,
    ) {
        val appContext = context.applicationContext
        viewModelScope.launch {
            val insertedId = repository.addEvent(
                title = title,
                dateKey = dateKey,
                reminderTimeMinutes = reminderTimeMinutes,
            )
            if (insertedId != null && reminderTimeMinutes != null) {
                ReminderScheduler.scheduleReminder(
                    context = appContext,
                    eventId = insertedId,
                    title = title.trim(),
                    dateKey = dateKey,
                    reminderTimeMinutes = reminderTimeMinutes,
                )
            }
        }
    }

    fun updateEventReminder(
        context: Context,
        event: CalendarEventEntity,
        reminderTimeMinutes: Int?,
    ) {
        val appContext = context.applicationContext
        viewModelScope.launch {
            repository.updateEventReminder(
                eventId = event.id,
                reminderTimeMinutes = reminderTimeMinutes,
            )
            if (reminderTimeMinutes != null) {
                ReminderScheduler.scheduleReminder(
                    context = appContext,
                    eventId = event.id,
                    title = event.title,
                    dateKey = event.dateKey,
                    reminderTimeMinutes = reminderTimeMinutes,
                )
            } else {
                ReminderScheduler.cancelReminder(
                    context = appContext,
                    eventId = event.id,
                )
            }
        }
    }

    fun updateEvent(
        context: Context,
        event: CalendarEventEntity,
        title: String,
        dateKey: String,
        reminderTimeMinutes: Int?,
    ) {
        val appContext = context.applicationContext
        viewModelScope.launch {
            val updated = repository.updateEvent(
                event = event,
                title = title,
                dateKey = dateKey,
                reminderTimeMinutes = reminderTimeMinutes,
            )
            if (!updated) return@launch
            if (reminderTimeMinutes != null) {
                ReminderScheduler.scheduleReminder(
                    context = appContext,
                    eventId = event.id,
                    title = title.trim(),
                    dateKey = dateKey,
                    reminderTimeMinutes = reminderTimeMinutes,
                )
            } else {
                ReminderScheduler.cancelReminder(
                    context = appContext,
                    eventId = event.id,
                )
            }
        }
    }

    fun deleteEvent(context: Context, eventId: Long) {
        val appContext = context.applicationContext
        viewModelScope.launch {
            ReminderScheduler.cancelReminder(
                context = appContext,
                eventId = eventId,
            )
            repository.deleteEvent(eventId)
        }
    }

    private val _transferFeedback = MutableSharedFlow<TransferFeedback>(extraBufferCapacity = 1)
    val transferFeedback: SharedFlow<TransferFeedback> = _transferFeedback.asSharedFlow()

    fun addNote(title: String, content: String) {
        viewModelScope.launch {
            repository.addNote(title = title, content = content)
        }
    }

    fun updateNote(note: NoteEntity, title: String, content: String) {
        viewModelScope.launch {
            repository.updateNote(note = note, title = title, content = content)
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            repository.deleteNote(noteId)
        }
    }

    fun importIcs(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            try {
                val raw = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                        it.readText()
                    }
                } ?: run {
                    _transferFeedback.emit(TransferFeedback.Error)
                    return@launch
                }

                val parsed = withContext(Dispatchers.Default) {
                    TransferFormats.parseEventsFromIcs(raw)
                }
                if (parsed.isEmpty()) {
                    _transferFeedback.emit(TransferFeedback.NothingToImport)
                    return@launch
                }

                val inserted = repository.addEvents(
                    parsed.map { CalendarEventEntity(title = it.title, dateKey = it.dateKey) }
                )
                if (inserted > 0) {
                    _transferFeedback.emit(TransferFeedback.ImportedEvents(inserted))
                } else {
                    _transferFeedback.emit(TransferFeedback.NothingToImport)
                }
            } catch (_: Exception) {
                _transferFeedback.emit(TransferFeedback.Error)
            }
        }
    }

    fun exportIcs(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            try {
                val events = repository.events.first()
                val content = withContext(Dispatchers.Default) {
                    TransferFormats.exportEventsToIcs(events)
                }
                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use {
                        it.write(content)
                    } ?: error("Cannot open output stream")
                }
                _transferFeedback.emit(TransferFeedback.ExportedEvents(events.size))
            } catch (_: Exception) {
                _transferFeedback.emit(TransferFeedback.Error)
            }
        }
    }

    fun importCsv(
        contentResolver: ContentResolver,
        uri: Uri,
        preferNotesWhenAmbiguous: Boolean,
    ) {
        viewModelScope.launch {
            try {
                val raw = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                        it.readText()
                    }
                } ?: run {
                    _transferFeedback.emit(TransferFeedback.Error)
                    return@launch
                }

                val parsed = withContext(Dispatchers.Default) {
                    TransferFormats.parseCsv(
                        raw = raw,
                        preferNotesWhenAmbiguous = preferNotesWhenAmbiguous,
                    )
                }

                val insertedEvents = if (parsed.events.isNotEmpty()) {
                    repository.addEvents(
                        parsed.events.map { CalendarEventEntity(title = it.title, dateKey = it.dateKey) }
                    )
                } else {
                    0
                }
                val insertedNotes = if (parsed.notes.isNotEmpty()) {
                    repository.addNotes(
                        parsed.notes.map { NoteEntity(title = it.title, content = it.content) }
                    )
                } else {
                    0
                }

                val feedback = when {
                    insertedEvents > 0 && insertedNotes > 0 -> {
                        TransferFeedback.ImportedBoth(insertedEvents, insertedNotes)
                    }
                    insertedEvents > 0 -> TransferFeedback.ImportedEvents(insertedEvents)
                    insertedNotes > 0 -> TransferFeedback.ImportedNotes(insertedNotes)
                    else -> TransferFeedback.NothingToImport
                }
                _transferFeedback.emit(feedback)
            } catch (_: Exception) {
                _transferFeedback.emit(TransferFeedback.Error)
            }
        }
    }

    fun exportCsv(
        contentResolver: ContentResolver,
        uri: Uri,
        exportNotes: Boolean,
    ) {
        viewModelScope.launch {
            try {
                if (exportNotes) {
                    val notes = repository.notes.first()
                    val content = withContext(Dispatchers.Default) {
                        TransferFormats.exportNotesToCsv(notes)
                    }
                    withContext(Dispatchers.IO) {
                        contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use {
                            it.write(content)
                        } ?: error("Cannot open output stream")
                    }
                    _transferFeedback.emit(TransferFeedback.ExportedNotes(notes.size))
                } else {
                    val events = repository.events.first()
                    val content = withContext(Dispatchers.Default) {
                        TransferFormats.exportEventsToCsv(events)
                    }
                    withContext(Dispatchers.IO) {
                        contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use {
                            it.write(content)
                        } ?: error("Cannot open output stream")
                    }
                    _transferFeedback.emit(TransferFeedback.ExportedEvents(events.size))
                }
            } catch (_: Exception) {
                _transferFeedback.emit(TransferFeedback.Error)
            }
        }
    }

    companion object {
        fun factory(repository: ReinmarRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ReinmarViewModel(repository) as T
                }
            }
    }
}
