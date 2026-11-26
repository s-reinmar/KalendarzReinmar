package com.example.domain.port

import com.example.domain.model.ColorWheelSnapshot
import com.example.domain.model.PaletteId
import com.example.domain.model.PersistedColorWheelState
import com.example.domain.model.SavedColorPalette
import kotlinx.coroutines.flow.Flow

interface ColorPaletteRepository {
    fun observeState(): Flow<PersistedColorWheelState>
    suspend fun saveCurrentSnapshot(snapshot: ColorWheelSnapshot)
    suspend fun addSavedPalette(palette: SavedColorPalette)
    suspend fun deleteSavedPalette(id: PaletteId)
}
