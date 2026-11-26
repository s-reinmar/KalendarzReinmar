package com.example.domain.port

import com.example.domain.model.AppPreferences
import kotlinx.coroutines.flow.Flow

interface AppPreferencesRepository {
    fun observePreferences(): Flow<AppPreferences>
    suspend fun updatePreferences(preferences: AppPreferences)
}
