package com.example.data.transfer

import com.example.data.preferences.ColorWheelSnapshotDto
import com.example.data.preferences.SavedColorPaletteDto
import kotlinx.serialization.Serializable

@Serializable
internal data class FullBackupEnvelopeDto(
    val app: String = APP_MARKER,
    val schemaVersion: Int = 1,
    val exportedAtMillis: Long = 0L,
    val events: List<BackupEventDto> = emptyList(),
    val notes: List<BackupNoteDto> = emptyList(),
    val colorWheelSnapshot: ColorWheelSnapshotDto? = null,
    val savedPalettes: List<SavedColorPaletteDto> = emptyList(),
    val preferences: BackupPreferencesDto? = null,
) {
    companion object {
        const val APP_MARKER = "KalendarzReinmar"
    }
}

@Serializable
internal data class BackupEventDto(
    val backupKey: String,
    val title: String,
    val dateKey: String,
    val endDateKey: String? = null,
    val isAllDay: Boolean = true,
    val startTimeMinutes: Int? = null,
    val endTimeMinutes: Int? = null,
    val description: String = "",
    val recurrenceCode: String = "NONE",
    val colorTagCode: String = "PINE",
    val entryTypeName: String = "EVENT",
    val reminderTimeMinutes: Int? = null,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val locationLabel: String? = null,
    val createdAtMillis: Long = 0L,
    val guests: List<String> = emptyList(),
    val meetingLink: String? = null,
    val taskPriorityName: String? = null,
    val taskCompleted: Boolean = false,
    val checklist: List<String> = emptyList(),
    val celebrantName: String? = null,
    val birthYear: Int? = null,
    val giftIdea: String? = null,
    val specialistName: String? = null,
    val contactPhone: String? = null,
    val preparationNotes: String? = null,
    val transportModeName: String? = null,
    val bookingReference: String? = null,
)

@Serializable
internal data class BackupNoteDto(
    val title: String = "",
    val content: String = "",
    val richSpansEncoded: String = "",
    val attachedEventBackupKeys: List<String> = emptyList(),
    val createdAtMillis: Long = 0L,
)

@Serializable
internal data class BackupPreferencesDto(
    val defaultReminderMinutes: Int = 9 * 60,
    val defaultSnoozeMinutes: Int = 15,
    val weekStartsOnMonday: Boolean = true,
)
