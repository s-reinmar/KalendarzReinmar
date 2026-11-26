package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AppPreferences
import com.example.domain.model.ReminderTime
import com.example.domain.port.AppPreferencesRepository
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.appPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_preferences",
)

class DataStoreAppPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) : AppPreferencesRepository {

    constructor(context: Context) : this(
        dataStore = context.applicationContext.appPreferencesDataStore,
    )

    override fun observePreferences(): Flow<AppPreferences> =
        dataStore.data
            .catch { error ->
                if (error is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }
            .map { prefs ->
                val reminderMinutes = prefs[KEY_DEFAULT_REMINDER_MINUTES]
                    ?: ReminderTime.DEFAULT.minutesOfDay
                val snoozeMinutes = prefs[KEY_DEFAULT_SNOOZE_MINUTES]
                    ?: AppPreferences.DEFAULT_SNOOZE_MINUTES
                val weekStartsOnMonday = prefs[KEY_WEEK_STARTS_ON_MONDAY] ?: true
                AppPreferences(
                    defaultReminderTime = ReminderTime.fromMinutes(reminderMinutes),
                    defaultSnoozeMinutes = AppPreferences.normalizeSnoozeMinutes(snoozeMinutes),
                    weekStartsOnMonday = weekStartsOnMonday,
                )
            }

    override suspend fun updatePreferences(preferences: AppPreferences) {
        val normalized = preferences.normalized()
        dataStore.edit { prefs ->
            prefs[KEY_DEFAULT_REMINDER_MINUTES] = normalized.defaultReminderTime.minutesOfDay
            prefs[KEY_DEFAULT_SNOOZE_MINUTES] = normalized.defaultSnoozeMinutes
            prefs[KEY_WEEK_STARTS_ON_MONDAY] = normalized.weekStartsOnMonday
        }
    }

    companion object {
        private val KEY_DEFAULT_REMINDER_MINUTES = intPreferencesKey("default_reminder_minutes")
        private val KEY_DEFAULT_SNOOZE_MINUTES = intPreferencesKey("default_snooze_minutes")
        private val KEY_WEEK_STARTS_ON_MONDAY = booleanPreferencesKey("week_starts_on_monday")
    }
}
