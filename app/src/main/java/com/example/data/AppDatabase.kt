package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CalendarEventEntity::class, NoteEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun noteDao(): NoteDao

    companion object {
        internal val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE calendar_events ADD COLUMN reminderTimeMinutes INTEGER DEFAULT NULL"
                )
            }
        }

        internal val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE calendar_events ADD COLUMN locationLat REAL DEFAULT NULL"
                )
                db.execSQL(
                    "ALTER TABLE calendar_events ADD COLUMN locationLng REAL DEFAULT NULL"
                )
                db.execSQL(
                    "ALTER TABLE calendar_events ADD COLUMN locationLabel TEXT DEFAULT NULL"
                )
            }
        }

        internal val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE calendar_events ADD COLUMN entryType TEXT NOT NULL DEFAULT 'EVENT'")
                db.execSQL("ALTER TABLE calendar_events ADD COLUMN endDateKey TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE calendar_events ADD COLUMN isAllDay INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE calendar_events ADD COLUMN startTimeMinutes INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE calendar_events ADD COLUMN endTimeMinutes INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE calendar_events ADD COLUMN description TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE calendar_events ADD COLUMN recurrence TEXT NOT NULL DEFAULT 'NONE'")
                db.execSQL("ALTER TABLE calendar_events ADD COLUMN colorTag TEXT NOT NULL DEFAULT 'PINE'")
                db.execSQL("ALTER TABLE calendar_events ADD COLUMN guests TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE calendar_events ADD COLUMN meetingLink TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE calendar_events ADD COLUMN taskPriority TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE calendar_events ADD COLUMN taskCompleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE calendar_events ADD COLUMN checklistItems TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE calendar_events ADD COLUMN celebrantName TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE calendar_events ADD COLUMN birthYear INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE calendar_events ADD COLUMN giftIdea TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE calendar_events ADD COLUMN specialistName TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE calendar_events ADD COLUMN contactPhone TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE calendar_events ADD COLUMN preparationNotes TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE calendar_events ADD COLUMN transportMode TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE calendar_events ADD COLUMN bookingReference TEXT DEFAULT NULL")
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kalendarz_reinmar.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
