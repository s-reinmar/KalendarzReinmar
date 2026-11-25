package com.example.domain.model

data class ImportedEvent(
    val title: String,
    val dateKey: DateKey,
)

data class ImportedNote(
    val title: String,
    val content: String,
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
    data class DemoLoaded(val eventsCount: Int, val notesCount: Int) : TransferFeedback
    data object DemoAlreadyLoaded : TransferFeedback
    data class DemoCleared(val eventsCount: Int, val notesCount: Int) : TransferFeedback
    data object DemoNothingToClear : TransferFeedback
}
