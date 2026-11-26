package com.example.feature.transfer

import com.example.data.transfer.CsvTransferCodec
import com.example.data.transfer.FullBackupCodec
import com.example.data.transfer.IcsEventCodec
import com.example.domain.model.BackupRestoreMode
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
import com.example.domain.port.ReminderPort
import com.example.domain.service.BackupRestoreService
import com.example.domain.service.EventDeduplicator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
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
    private val backupRestoreService: BackupRestoreService? = null,
    private val fullBackupCodec: FullBackupCodec = FullBackupCodec(),
    private val reminderPort: ReminderPort? = null,
) {
    private val _uiState = MutableStateFlow(TransferUiState())
    val uiState: StateFlow<TransferUiState> = _uiState.asStateFlow()

    private val _feedback = MutableSharedFlow<TransferFeedback>(extraBufferCapacity = 4)
    val feedback: SharedFlow<TransferFeedback> = _feedback.asSharedFlow()

    fun importIcs(documentUri: String) {
        launchTransfer {
            val raw = documentStorage.readText(documentUri).getOrElse {
                _feedback.emit(TransferFeedback.Error)
                return@launchTransfer
            }
            val parsed = withContext(computationDispatcher) {
                icsCodec.parse(raw)
            }
            if (parsed.isEmpty()) {
                _feedback.emit(TransferFeedback.NothingToImport)
                return@launchTransfer
            }
            val existing = eventRepository.observeEvents().first()
            val candidates = parsed.map(::toCalendarEvent)
            val unique = EventDeduplicator.filterNewEvents(existing = existing, incoming = candidates)
            if (unique.isEmpty()) {
                _feedback.emit(TransferFeedback.NothingToImport)
                return@launchTransfer
            }
            val inserted = eventRepository.addEvents(unique)
            scheduleRemindersFor(inserted)
            if (inserted.isNotEmpty()) {
                _feedback.emit(TransferFeedback.ImportedEvents(inserted.size))
            } else {
                _feedback.emit(TransferFeedback.NothingToImport)
            }
        }
    }

    fun exportIcs(documentUri: String) {
        launchTransfer {
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
        launchTransfer {
            val raw = documentStorage.readText(documentUri).getOrElse {
                _feedback.emit(TransferFeedback.Error)
                return@launchTransfer
            }
            val parsed = withContext(computationDispatcher) {
                csvCodec.parse(raw = raw, preferNotesWhenAmbiguous = preferNotesWhenAmbiguous)
            }
            val insertedEvents = if (parsed.events.isNotEmpty()) {
                val existingEvents = eventRepository.observeEvents().first()
                val uniqueEvents = EventDeduplicator.filterNewEvents(
                    existing = existingEvents,
                    incoming = parsed.events.map(::toCalendarEvent),
                )
                if (uniqueEvents.isNotEmpty()) {
                    val added = eventRepository.addEvents(uniqueEvents)
                    scheduleRemindersFor(added)
                    added.size
                } else {
                    0
                }
            } else {
                0
            }
            val insertedNotes = if (parsed.notes.isNotEmpty()) {
                val existingNotes = noteRepository.observeNotes().first()
                val uniqueNotes = EventDeduplicator.filterNewNotes(
                    existing = existingNotes,
                    incoming = parsed.notes.map { item ->
                        Note(
                            id = NoteId.UNASSIGNED,
                            title = item.title,
                            content = item.content,
                            richSpans = item.richSpans,
                        )
                    },
                )
                if (uniqueNotes.isNotEmpty()) {
                    noteRepository.addNotes(uniqueNotes)
                } else {
                    0
                }
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
        launchTransfer {
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

    fun exportFullBackup(documentUri: String) {
        val service = backupRestoreService ?: run {
            _feedback.tryEmit(TransferFeedback.Error)
            return
        }
        launchTransfer {
            val snapshot = service.createSnapshot()
            val encoded = withContext(computationDispatcher) {
                fullBackupCodec.encode(snapshot)
            }
            val writeResult = documentStorage.writeText(documentUri, encoded)
            if (writeResult.isSuccess) {
                _feedback.emit(
                    TransferFeedback.ExportedBackup(
                        eventsCount = snapshot.events.size,
                        notesCount = snapshot.notes.size,
                        palettesCount = snapshot.savedPalettes.size,
                    ),
                )
            } else {
                _feedback.emit(TransferFeedback.Error)
            }
        }
    }

    fun prepareImportFullBackup(documentUri: String) {
        if (backupRestoreService == null) {
            _feedback.tryEmit(TransferFeedback.Error)
            return
        }
        launchTransfer {
            val raw = documentStorage.readText(documentUri).getOrElse {
                _feedback.emit(TransferFeedback.Error)
                return@launchTransfer
            }
            val snapshot = withContext(computationDispatcher) {
                fullBackupCodec.decode(raw)
            }
            if (snapshot == null) {
                _feedback.emit(TransferFeedback.InvalidBackupFile)
                return@launchTransfer
            }
            _uiState.update {
                it.copy(
                    pendingBackupRestore = PendingBackupRestore(
                        documentUri = documentUri,
                        preview = snapshot.toPreview(),
                        snapshot = snapshot,
                    ),
                )
            }
        }
    }

    fun confirmRestoreBackup(mode: BackupRestoreMode) {
        val service = backupRestoreService
        val pending = _uiState.value.pendingBackupRestore
        if (service == null || pending == null) {
            dismissBackupRestoreDialog()
            return
        }
        _uiState.update { it.copy(pendingBackupRestore = null) }
        launchTransfer {
            val summary = service.restoreSnapshot(snapshot = pending.snapshot, mode = mode)
            _feedback.emit(
                TransferFeedback.RestoredBackup(
                    eventsCount = summary.importedEventsCount,
                    notesCount = summary.importedNotesCount,
                    palettesCount = summary.importedPalettesCount,
                ),
            )
        }
    }

    fun dismissBackupRestoreDialog() {
        _uiState.update { it.copy(pendingBackupRestore = null) }
    }

    private fun launchTransfer(block: suspend () -> Unit) {
        scope.launch {
            _uiState.update { it.copy(isBusy = true) }
            try {
                block()
            } finally {
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }

    private fun scheduleRemindersFor(events: List<CalendarEvent>) {
        val port = reminderPort ?: return
        events.filter { it.hasReminder }.forEach(port::schedule)
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
