package com.example.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.port.ClockProvider
import com.example.domain.port.ColorPaletteRepository

class SettingsViewModel(
    colorPaletteRepository: ColorPaletteRepository,
    clock: ClockProvider,
) : ViewModel() {

    val controller = SettingsController(
        repository = colorPaletteRepository,
        clock = clock,
        scope = viewModelScope,
    )

    companion object {
        fun factory(
            colorPaletteRepository: ColorPaletteRepository,
            clock: ClockProvider,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SettingsViewModel(colorPaletteRepository, clock) as T
            }
    }
}
