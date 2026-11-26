package com.example.domain.model

enum class BackupRestoreMode {
    MERGE,
    REPLACE,
}

data class BackupEventRecord(
    val backupKey: String,
    val event: CalendarEvent,
)

data class BackupNoteRecord(
    val note: Note,
    val attachedEventBackupKeys: List<String> = emptyList(),
)

data class FullBackupSnapshot(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exportedAtMillis: Long,
    val events: List<BackupEventRecord> = emptyList(),
    val notes: List<BackupNoteRecord> = emptyList(),
    val colorWheelSnapshot: ColorWheelSnapshot? = null,
    val savedPalettes: List<SavedColorPalette> = emptyList(),
    val preferences: AppPreferences? = null,
) {
    fun toPreview(): BackupPreview = BackupPreview(
        schemaVersion = schemaVersion,
        exportedAtMillis = exportedAtMillis,
        eventsCount = events.size,
        notesCount = notes.size,
        palettesCount = savedPalettes.size,
    )

    companion object {
        const val CURRENT_SCHEMA_VERSION: Int = 1
    }
}

data class BackupPreview(
    val schemaVersion: Int,
    val exportedAtMillis: Long,
    val eventsCount: Int,
    val notesCount: Int,
    val palettesCount: Int,
)

data class BackupRestoreSummary(
    val importedEventsCount: Int,
    val importedNotesCount: Int,
    val importedPalettesCount: Int,
    val mode: BackupRestoreMode,
)

data class LocalDatabaseStats(
    val totalEvents: Int = 0,
    val pendingTasks: Int = 0,
    val completedTasks: Int = 0,
    val eventsWithReminder: Int = 0,
    val eventsWithLocation: Int = 0,
    val totalNotes: Int = 0,
    val savedPalettes: Int = 0,
) {
    companion object {
        fun compute(
            events: List<CalendarEvent>,
            notes: List<Note>,
            palettesCount: Int,
        ): LocalDatabaseStats {
            var pendingTasks = 0
            var completedTasks = 0
            var withReminder = 0
            var withLocation = 0
            for (event in events) {
                if (event.hasReminder) withReminder++
                if (event.hasLocation) withLocation++
                val task = event.details as? EventTypeDetails.Task
                if (task != null) {
                    if (task.isCompleted) completedTasks++ else pendingTasks++
                }
            }
            return LocalDatabaseStats(
                totalEvents = events.size,
                pendingTasks = pendingTasks,
                completedTasks = completedTasks,
                eventsWithReminder = withReminder,
                eventsWithLocation = withLocation,
                totalNotes = notes.size,
                savedPalettes = palettesCount,
            )
        }
    }
}
