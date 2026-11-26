package com.example.domain.model

data class ImportedEvent(
    val title: String,
    val dateKey: DateKey,
    val endDateKey: DateKey? = null,
    val entryType: CalendarEntryType = CalendarEntryType.EVENT,
    val isAllDay: Boolean = true,
    val timeRange: EventTimeRange? = null,
    val description: String = "",
    val recurrence: EventRecurrence = EventRecurrence.NONE,
    val colorTag: EventColorTag? = null,
)

data class ImportedNote(
    val title: String,
    val content: String,
    val richSpans: List<NoteRichSpan> = emptyList(),
)

data class CsvImportResult(
    val events: List<ImportedEvent> = emptyList(),
    val notes: List<ImportedNote> = emptyList(),
)

sealed interface TransferFeedback {
    data class ImportedEvents(val count: Int) : TransferFeedback
    data class ImportedNotes(val count: Int) : TransferFeedback
    data class ImportedBoth(val eventsCount: Int, val notesCount: Int) : TransferFeedback
    data class ExportedEvents(val count: Int) : TransferFeedback
    data class ExportedNotes(val count: Int) : TransferFeedback
    data object NothingToImport : TransferFeedback
    data object Error : TransferFeedback
}
