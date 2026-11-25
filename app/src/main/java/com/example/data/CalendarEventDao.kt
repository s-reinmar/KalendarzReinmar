package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarEventDao {
    @Query("SELECT * FROM calendar_events ORDER BY dateKey ASC, createdAt ASC")
    fun getAllEvents(): Flow<List<CalendarEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: CalendarEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<CalendarEventEntity>): List<Long>

    @Query(
        "UPDATE calendar_events SET reminderTimeMinutes = :reminderTimeMinutes, " +
            "locationLat = :locationLat, locationLng = :locationLng, locationLabel = :locationLabel " +
            "WHERE id = :eventId"
    )
    suspend fun updateEventReminder(
        eventId: Long,
        reminderTimeMinutes: Int?,
        locationLat: Double?,
        locationLng: Double?,
        locationLabel: String?,
    )

    @Query("SELECT * FROM calendar_events WHERE reminderTimeMinutes IS NOT NULL")
    suspend fun getEventsWithReminders(): List<CalendarEventEntity>

    @Update
    suspend fun updateEvent(event: CalendarEventEntity)

    @Query("DELETE FROM calendar_events WHERE id = :eventId")
    suspend fun deleteEventById(eventId: Long)
}
