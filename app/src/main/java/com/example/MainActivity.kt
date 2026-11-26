package com.example

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.luminance
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.di.AppContainer
import com.example.domain.model.EventId
import com.example.feature.calendar.CalendarViewModel
import com.example.feature.map.MapViewModel
import com.example.feature.notes.NotesViewModel
import com.example.feature.settings.SettingsViewModel
import com.example.feature.shell.AppShellView
import com.example.feature.shell.ShellViewModel
import com.example.feature.transfer.TransferViewModel
import com.example.reminder.ReminderExtras
import com.example.ui.theme.KalendarzReinmarTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val notificationEventId = MutableStateFlow<EventId?>(null)
    private lateinit var appContainer: AppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        appContainer = AppContainer(applicationContext)
        appContainer.reminderPort.ensureChannel()
        extractNotificationEventId(intent)

        setContent {
            val settingsVm: SettingsViewModel = viewModel(factory = appContainer.settingsViewModelFactory())
            val settingsState by settingsVm.controller.uiState.collectAsStateWithLifecycle()
            val activeThemeSwatches = if (settingsState.applyToAppTheme) {
                settingsState.swatches
            } else {
                null
            }

            KalendarzReinmarTheme(
                customPaletteSwatches = activeThemeSwatches,
                surfaceMode = settingsState.surfaceMode,
            ) {
                val isDarkTopChrome = MaterialTheme.colorScheme.primaryContainer.luminance() < 0.42f
                val isDarkBottomChrome = MaterialTheme.colorScheme.surfaceContainerHigh.luminance() < 0.42f
                SideEffect {
                    enableEdgeToEdge(
                        statusBarStyle = if (isDarkTopChrome) {
                            SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                        } else {
                            SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                        },
                        navigationBarStyle = if (isDarkBottomChrome) {
                            SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                        } else {
                            SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                        },
                    )
                }

                val shellVm: ShellViewModel = viewModel(factory = appContainer.shellViewModelFactory())
                val calendarVm: CalendarViewModel = viewModel(factory = appContainer.calendarViewModelFactory())
                val mapVm: MapViewModel = viewModel(factory = appContainer.mapViewModelFactory())
                val notesVm: NotesViewModel = viewModel(factory = appContainer.notesViewModelFactory())
                val transferVm: TransferViewModel = viewModel(factory = appContainer.transferViewModelFactory())

                val incomingEventId by notificationEventId.collectAsStateWithLifecycle()
                LaunchedEffect(incomingEventId) {
                    val eventId = incomingEventId
                    if (eventId != null && eventId.isValid) {
                        shellVm.controller.requestOpenEventDetails(eventId)
                        notificationEventId.value = null
                    }
                }

                AppShellView(
                    container = appContainer,
                    shellController = shellVm.controller,
                    calendarController = calendarVm.controller,
                    mapController = mapVm.controller,
                    notesController = notesVm.controller,
                    transferController = transferVm.controller,
                    settingsController = settingsVm.controller,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractNotificationEventId(intent)
    }

    private fun extractNotificationEventId(intent: Intent?) {
        val rawId = intent?.getLongExtra(ReminderExtras.EXTRA_EVENT_ID, -1L) ?: -1L
        val eventId = EventId(rawId)
        if (eventId.isValid) {
            notificationEventId.value = eventId
            intent?.removeExtra(ReminderExtras.EXTRA_EVENT_ID)
        }
    }
}
