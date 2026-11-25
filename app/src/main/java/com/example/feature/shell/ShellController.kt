package com.example.feature.shell

import com.example.domain.model.EventId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ShellUiState(
    val activeTab: AppTab = AppTab.CALENDAR,
    val pendingOpenEventId: EventId? = null,
    val pendingMapFocusEventId: EventId? = null,
)

class ShellController {
    private val _uiState = MutableStateFlow(ShellUiState())
    val uiState: StateFlow<ShellUiState> = _uiState.asStateFlow()

    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun navigateBackToCalendar() {
        _uiState.update { it.copy(activeTab = AppTab.CALENDAR) }
    }

    fun requestOpenEventDetails(eventId: EventId) {
        if (!eventId.isValid) return
        _uiState.update { it.copy(pendingOpenEventId = eventId) }
    }

    fun consumePendingOpenEventDetails() {
        _uiState.update { it.copy(pendingOpenEventId = null) }
    }

    fun showEventOnMap(eventId: EventId) {
        if (!eventId.isValid) return
        _uiState.update {
            it.copy(
                activeTab = AppTab.MAP,
                pendingMapFocusEventId = eventId,
            )
        }
    }

    fun consumePendingMapFocusEvent() {
        _uiState.update { it.copy(pendingMapFocusEventId = null) }
    }
}
