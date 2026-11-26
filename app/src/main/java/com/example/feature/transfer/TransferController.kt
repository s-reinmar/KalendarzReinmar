package com.example.feature.transfer

import com.example.data.transfer.CsvTransferCodec
import com.example.data.transfer.IcsEventCodec
import com.example.domain.model.CalendarEvent
import com.example.domain.model.EventId
import com.example.domain.model.EventTypeDetails
import com.example.domain.model.ImportedEvent
import com.example.domain.model.Note
import com.example.domain.model.NoteId
import com.example.domain.model.TransferFeedback
import com.example.domain.port.DocumentStoragePort
import com.example.domain.port.EventRepository
import com.example.domain.port.NoteRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TransferController(
    private val eventRepository: EventRepository,
    private val noteRepository: NoteRepository,
    private val documentStorage: DocumentStoragePort,
    private val icsCodec: IcsEventCodec,
    private val csvCodec: CsvTransferCodec,
    private val scope: CoroutineScope,
    private val computationDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val _feedback = MutableSharedFlow<TransferFeedback>(extraBufferCapacity = 4)
    val feedback: SharedFlow<TransferFeedback> = _feedback.asSharedFlow()

    fun importIcs(documentUri: String) {
        scope.launch {
            val raw = documentStorage.readText(documentUri).getOrElse {
                _feedback.emit(TransferFeedback.Error)
                return@launch
            }
            val parsed = withContext(computationDispatcher) {
                icsCodec.parse(raw)
            }
            if (parsed.isEmpty()) {
                _feedback.emit(TransferFeedback.NothingToImport)
                return@launch
            }
            val inserted = eventRepository.addEvents(
                parsed.map(::toCalendarEvent)
            )
            if (inserted.isNotEmpty()) {
                _feedback.emit(TransferFeedback.ImportedEvents(inserted.size))
            } else {
                _feedback.emit(TransferFeedback.NothingToImport)
            }
        }
    }

    fun exportIcs(documentUri: String) {
        scope.launch {
            val events = eventRepository.observeEvents().first()
            val content = withContext(computationDispatcher) {
                icsCodec.export(events)
            }
            val writeResult = documentStorage.writeText(documentUri, content)
            if (writeResult.isSuccess) {
                _feedback.emit(TransferFeedback.ExportedEvents(events.size))
            } else {
                _feedback.emit(TransferFeedback.Error)
            }
        }
    }

    fun importCsv(documentUri: String, preferNotesWhenAmbiguous: Boolean) {
        scope.launch {
            val raw = documentStorage.readText(documentUri).getOrElse {
                _feedback.emit(TransferFeedback.Error)
                return@launch
            }
            val parsed = withContext(computationDispatcher) {
                csvCodec.parse(raw = raw, preferNotesWhenAmbiguous = preferNotesWhenAmbiguous)
            }
            val insertedEvents = if (parsed.events.isNotEmpty()) {
                eventRepository.addEvents(
                    parsed.events.map(::toCalendarEvent)
                ).size
            } else {
                0
            }
            val insertedNotes = if (parsed.notes.isNotEmpty()) {
                noteRepository.addNotes(
                    parsed.notes.map { item ->
                        Note(
                            id = NoteId.UNASSIGNED,
                            title = item.title,
                            content = item.content,
                            richSpans = item.richSpans,
                        )
                    }
                )
            } else {
                0
            }

            val message = when {
                insertedEvents > 0 && insertedNotes > 0 -> {
                    TransferFeedback.ImportedBoth(insertedEvents, insertedNotes)
                }
                insertedEvents > 0 -> TransferFeedback.ImportedEvents(insertedEvents)
                insertedNotes > 0 -> TransferFeedback.ImportedNotes(insertedNotes)
                else -> TransferFeedback.NothingToImport
            }
            _feedback.emit(message)
        }
    }

    fun exportCsv(documentUri: String, exportNotes: Boolean) {
        scope.launch {
            if (exportNotes) {
                val notes = noteRepository.observeNotes().first()
                val content = withContext(computationDispatcher) {
                    csvCodec.exportNotes(notes)
                }
                val result = documentStorage.writeText(documentUri, content)
                if (result.isSuccess) {
                    _feedback.emit(TransferFeedback.ExportedNotes(notes.size))
                } else {
                    _feedback.emit(TransferFeedback.Error)
                }
            } else {
                val events = eventRepository.observeEvents().first()
                val content = withContext(computationDispatcher) {
                    csvCodec.exportEvents(events)
                }
                val result = documentStorage.writeText(documentUri, content)
                if (result.isSuccess) {
                    _feedback.emit(TransferFeedback.ExportedEvents(events.size))
                } else {
                    _feedback.emit(TransferFeedback.Error)
                }
            }
        }
    }

    private fun toCalendarEvent(item: ImportedEvent): CalendarEvent = CalendarEvent(
        id = EventId.UNASSIGNED,
        title = item.title,
        dateKey = item.dateKey,
        endDateKey = item.endDateKey,
        isAllDay = item.isAllDay,
        timeRange = item.timeRange,
        description = item.description,
        recurrence = item.recurrence,
        colorTag = item.colorTag ?: item.entryType.defaultColorTag,
        details = EventTypeDetails.defaultFor(item.entryType),
    )
}
