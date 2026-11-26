package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.ColorWheelSnapshot
import com.example.domain.model.PaletteId
import com.example.domain.model.PersistedColorWheelState
import com.example.domain.model.SavedColorPalette
import com.example.domain.port.ColorPaletteRepository
import com.example.domain.service.ColorHarmonyEngine
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.colorWheelDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "color_wheel_settings",
)

class DataStoreColorPaletteRepository(
    private val dataStore: DataStore<Preferences>,
    private val json: Json = defaultJson,
) : ColorPaletteRepository {

    constructor(context: Context) : this(
        dataStore = context.applicationContext.colorWheelDataStore,
    )

    override fun observeState(): Flow<PersistedColorWheelState> =
        dataStore.data
            .catch { error ->
                if (error is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }
            .map { prefs ->
                val snapshot = decodeSnapshot(prefs[KEY_ACTIVE_SNAPSHOT])
                val palettes = decodeSavedPalettes(prefs[KEY_SAVED_PALETTES])
                PersistedColorWheelState(
                    activeSnapshot = snapshot,
                    savedPalettes = palettes,
                )
            }

    override suspend fun saveCurrentSnapshot(snapshot: ColorWheelSnapshot) {
        val encoded = json.encodeToString(snapshot.toDto())
        dataStore.edit { prefs ->
            prefs[KEY_ACTIVE_SNAPSHOT] = encoded
        }
    }

    override suspend fun addSavedPalette(palette: SavedColorPalette) {
        dataStore.edit { prefs ->
            val current = decodeSavedPalettes(prefs[KEY_SAVED_PALETTES])
            val updated = listOf(palette) + current.filterNot { it.id == palette.id }
            val envelope = SavedPalettesEnvelopeDto(
                palettes = updated.map(SavedColorPalette::toDto),
            )
            prefs[KEY_SAVED_PALETTES] = json.encodeToString(envelope)
        }
    }

    override suspend fun deleteSavedPalette(id: PaletteId) {
        dataStore.edit { prefs ->
            val current = decodeSavedPalettes(prefs[KEY_SAVED_PALETTES])
            val updated = current.filterNot { it.id == id }
            val envelope = SavedPalettesEnvelopeDto(
                palettes = updated.map(SavedColorPalette::toDto),
            )
            prefs[KEY_SAVED_PALETTES] = json.encodeToString(envelope)
        }
    }

    private fun decodeSnapshot(raw: String?): ColorWheelSnapshot {
        if (raw.isNullOrBlank()) return ColorHarmonyEngine.defaultSnapshot()
        return runCatching {
            json.decodeFromString<ColorWheelSnapshotDto>(raw).toDomainOrDefault()
        }.getOrElse { ColorHarmonyEngine.defaultSnapshot() }
    }

    private fun decodeSavedPalettes(raw: String?): List<SavedColorPalette> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString<SavedPalettesEnvelopeDto>(raw)
                .palettes
                .mapNotNull(SavedColorPaletteDto::toDomainOrNull)
        }.getOrElse { emptyList() }
    }

    companion object {
        private val KEY_ACTIVE_SNAPSHOT = stringPreferencesKey("active_color_wheel_snapshot")
        private val KEY_SAVED_PALETTES = stringPreferencesKey("saved_color_palettes")

        private val defaultJson: Json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}
