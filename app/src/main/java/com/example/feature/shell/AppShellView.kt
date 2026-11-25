package com.example.feature.shell

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.di.AppContainer
import com.example.feature.calendar.CalendarController
import com.example.feature.calendar.CalendarDialogHost
import com.example.feature.calendar.CalendarScreen
import com.example.feature.calendar.CreateEntrySpeedDialFab
import com.example.feature.calendar.CreateEntrySpeedDialScrim
import com.example.feature.map.EventsMapScreen
import com.example.feature.map.MapController
import com.example.feature.notes.NotesController
import com.example.feature.notes.NotesDialogHost
import com.example.feature.notes.NotesScreen
import com.example.feature.transfer.TransferController
import com.example.ui.theme.AppIcons
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShellView(
    container: AppContainer,
    shellController: ShellController,
    calendarController: CalendarController,
    mapController: MapController,
    notesController: NotesController,
    transferController: TransferController,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isCreateMenuExpanded by remember { mutableStateOf(false) }

    val shellState by shellController.uiState.collectAsStateWithLifecycle()
    val calendarState by calendarController.uiState.collectAsStateWithLifecycle()
    val mapState by mapController.uiState.collectAsStateWithLifecycle()
    val notesState by notesController.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(shellState.activeTab) {
        isCreateMenuExpanded = false
    }

    LaunchedEffect(shellState.pendingOpenEventId, calendarState.events) {
        val targetId = shellState.pendingOpenEventId
        if (targetId != null && calendarState.events.any { it.id == targetId }) {
            calendarController.openEventDetails(targetId)
            shellController.consumePendingOpenEventDetails()
        }
    }

    LaunchedEffect(transferController) {
        transferController.feedback.collect { feedback ->
            snackbarHostState.showSnackbar(
                TransferFeedbackFormatter.format(context, feedback),
            )
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.reminder_permission_denied))
            }
        }
    }

    fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val importIcsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { transferController.importIcs(it.toString()) } }

    val exportIcsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/calendar"),
    ) { uri -> uri?.let { transferController.exportIcs(it.toString()) } }

    val importCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            transferController.importCsv(
                documentUri = it.toString(),
                preferNotesWhenAmbiguous = shellState.activeTab == AppTab.NOTES,
            )
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        uri?.let {
            transferController.exportCsv(
                documentUri = it.toString(),
                exportNotes = shellState.activeTab == AppTab.NOTES,
            )
        }
    }

    BackHandler(enabled = !isCreateMenuExpanded && shellState.activeTab != AppTab.CALENDAR) {
        shellController.navigateBackToCalendar()
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useNavigationRail) {
                AppNavigationRail(
                    activeTab = shellState.activeTab,
                    onSelectTab = shellController::selectTab,
                )
            }

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = when (shellState.activeTab) {
                                        AppTab.CALENDAR -> stringResource(R.string.nav_calendar)
                                        AppTab.MAP -> stringResource(R.string.nav_map)
                                        AppTab.NOTES -> stringResource(R.string.nav_notes)
                                    },
                                    style = MaterialTheme.typography.headlineMedium,
                                )
                                val subtitle = when (shellState.activeTab) {
                                    AppTab.CALENDAR -> stringResource(
                                        R.string.calendar_summary_count,
                                        calendarState.events.size,
                                    )
                                    AppTab.MAP -> stringResource(
                                        R.string.map_events_count,
                                        mapState.eventsWithLocation.size,
                                    )
                                    AppTab.NOTES -> stringResource(
                                        R.string.notes_summary_count,
                                        notesState.notes.size,
                                    )
                                }
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        actions = {
                            TopBarOverflowMenu(
                                onLoadDemoData = transferController::loadDemoData,
                                onClearDemoData = transferController::clearDemoData,
                                onImportIcs = { importIcsLauncher.launch(arrayOf("*/*")) },
                                onExportIcs = { exportIcsLauncher.launch("kalendarz_reinmar.ics") },
                                onImportCsv = { importCsvLauncher.launch(arrayOf("*/*")) },
                                onExportCsv = {
                                    val fileName = if (shellState.activeTab == AppTab.NOTES) {
                                        "kalendarz_reinmar_notatki.csv"
                                    } else {
                                        "kalendarz_reinmar_wydarzenia.csv"
                                    }
                                    exportCsvLauncher.launch(fileName)
                                },
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onBackground,
                        ),
                        windowInsets = WindowInsets.statusBars,
                    )
                },
                bottomBar = {
                    if (!useNavigationRail) {
                        AppNavigationBar(
                            activeTab = shellState.activeTab,
                            onSelectTab = shellController::selectTab,
                        )
                    }
                },
                floatingActionButton = {
                    when (shellState.activeTab) {
                        AppTab.CALENDAR, AppTab.MAP -> {
                            CreateEntrySpeedDialFab(
                                expanded = isCreateMenuExpanded,
                                onToggle = { isCreateMenuExpanded = !isCreateMenuExpanded },
                                onSelectType = { entryType ->
                                    isCreateMenuExpanded = false
                                    calendarController.openCreateEvent(entryType = entryType)
                                },
                            )
                        }
                        AppTab.NOTES -> {
                            FloatingActionButton(
                                onClick = notesController::openCreateNote,
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.testTag("fab_add_note"),
                            ) {
                                Icon(
                                    painter = painterResource(AppIcons.add),
                                    contentDescription = stringResource(R.string.notes_add_note),
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                    }
                },
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    val maxContentWidth = if (shellState.activeTab == AppTab.MAP) 960.dp else 640.dp
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth()
                            .widthIn(max = maxContentWidth),
                    ) {
                        when (shellState.activeTab) {
                            AppTab.CALENDAR -> {
                                CalendarScreen(
                                    uiState = calendarState,
                                    onSelectDate = calendarController::selectDate,
                                    onSelectTypeFilter = calendarController::selectTypeFilter,
                                    onPreviousMonth = calendarController::previousMonth,
                                    onNextMonth = calendarController::nextMonth,
                                    onEventClick = { calendarController.openEventDetails(it.id) },
                                    onToggleTaskCompleted = calendarController::toggleTaskCompletion,
                                    onLocationPinClick = calendarController::openLocationPinAction,
                                    onReminderClick = calendarController::openReminderEditor,
                                    onEditClick = calendarController::openEditEvent,
                                    onDeleteClick = calendarController::requestDeleteEvent,
                                    onLoadDemoData = transferController::loadDemoData,
                                )
                            }
                            AppTab.MAP -> {
                                EventsMapScreen(
                                    controller = mapController,
                                    focusedEventId = shellState.pendingMapFocusEventId,
                                    onFocusedEventConsumed = shellController::consumePendingMapFocusEvent,
                                    onOpenEventDetails = { calendarController.openEventDetails(it.id) },
                                    onAddEventAtLocation = { location ->
                                        calendarController.openCreateEvent(initialLocation = location)
                                    },
                                    onShowMessage = { message ->
                                        coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                                    },
                                    isPaused = calendarState.hasOpenDialog || isCreateMenuExpanded,
                                )
                            }
                            AppTab.NOTES -> {
                                NotesScreen(
                                    notes = notesState.notes,
                                    onEditClick = notesController::openEditNote,
                                    onDeleteClick = notesController::requestDeleteNote,
                                    onLoadDemoData = transferController::loadDemoData,
                                )
                            }
                        }
                    }

                    CreateEntrySpeedDialScrim(
                        expanded = isCreateMenuExpanded && shellState.activeTab != AppTab.NOTES,
                        onDismiss = { isCreateMenuExpanded = false },
                    )
                }
            }
        }
    }

    CalendarDialogHost(
        uiState = calendarState,
        todayDay = container.clockProvider.today(),
        controller = calendarController,
        geocodingService = container.geocodingService,
        locationProvider = container.deviceLocationProvider,
        onEnsureNotificationPermission = ::ensureNotificationPermission,
        onShowEventOnMainMap = shellController::showEventOnMap,
    )

    NotesDialogHost(
        dialog = notesState.dialog,
        controller = notesController,
    )
}
