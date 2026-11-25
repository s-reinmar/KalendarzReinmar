package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val dateKey: String,
    val reminderTimeMinutes: Int? = null,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val locationLabel: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val entryType: String = "EVENT",
    val endDateKey: String? = null,
    val isAllDay: Boolean = true,
    val startTimeMinutes: Int? = null,
    val endTimeMinutes: Int? = null,
    val description: String = "",
    val recurrence: String = "NONE",
    val colorTag: String = "PINE",
    val guests: String? = null,
    val meetingLink: String? = null,
    val taskPriority: String? = null,
    val taskCompleted: Boolean = false,
    val checklistItems: String? = null,
    val celebrantName: String? = null,
    val birthYear: Int? = null,
    val giftIdea: String? = null,
    val specialistName: String? = null,
    val contactPhone: String? = null,
    val preparationNotes: String? = null,
    val transportMode: String? = null,
    val bookingReference: String? = null,
)
