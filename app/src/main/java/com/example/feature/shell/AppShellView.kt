package com.example.feature.shell

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.example.feature.settings.SettingsController
import com.example.feature.settings.SettingsFeedback
import com.example.feature.settings.SettingsScreen
import com.example.feature.transfer.BackupRestoreDialog
import com.example.feature.transfer.TransferController
import com.example.ui.theme.AppIcons
import kotlinx.coroutines.launch

@Composable
fun AppShellView(
    container: AppContainer,
    shellController: ShellController,
    calendarController: CalendarController,
    mapController: MapController,
    notesController: NotesController,
    transferController: TransferController,
    settingsController: SettingsController,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isCreateMenuExpanded by remember { mutableStateOf(false) }

    val shellState by shellController.uiState.collectAsStateWithLifecycle()
    val calendarState by calendarController.uiState.collectAsStateWithLifecycle()
    val mapState by mapController.uiState.collectAsStateWithLifecycle()
    val notesState by notesController.uiState.collectAsStateWithLifecycle()
    val settingsState by settingsController.uiState.collectAsStateWithLifecycle()
    val transferState by transferController.uiState.collectAsStateWithLifecycle()

    DisposableEffect(lifecycleOwner, calendarController) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                calendarController.refreshToday()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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

    LaunchedEffect(settingsController) {
        settingsController.feedback.collect { feedback ->
            val message = when (feedback) {
                is SettingsFeedback.PaletteSaved -> context.getString(
                    R.string.settings_palette_saved_feedback,
                    feedback.paletteName,
                )
                is SettingsFeedback.PaletteRestored -> context.getString(
                    R.string.settings_palette_restored_feedback,
                    feedback.paletteName,
                )
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    val launchers = rememberShellActivityLaunchers(
        activeTab = shellState.activeTab,
        transferController = transferController,
        onShowMessage = { message ->
            coroutineScope.launch { snackbarHostState.showSnackbar(message) }
        },
    )

    BackHandler(enabled = !isCreateMenuExpanded && shellState.activeTab != AppTab.CALENDAR) {
        shellController.navigateBackToCalendar()
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                        ShellTopBar(
                            activeTab = shellState.activeTab,
                            eventsCount = calendarState.events.size,
                            mapEventsCount = mapState.eventsWithLocation.size,
                            notesCount = notesState.notes.size,
                            launchers = launchers,
                        )
                    },
                    bottomBar = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (!useNavigationRail) {
                                AppNavigationBar(
                                    activeTab = shellState.activeTab,
                                    onSelectTab = shellController::selectTab,
                                    windowInsets = WindowInsets(0, 0, 0, 0),
                                )
                            }
                            AppCopyrightFooter(
                                onOpenAuthorProfile = { url ->
                                    runCatching { uriHandler.openUri(url) }
                                        .onFailure {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(
                                                    context.getString(R.string.footer_open_link_error),
                                                )
                                            }
                                        }
                                },
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
                            AppTab.SETTINGS -> Unit
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
                                        onJumpToToday = calendarController::jumpToToday,
                                        onEventClick = { calendarController.openEventDetails(it.id) },
                                        onToggleTaskCompleted = calendarController::toggleTaskCompletion,
                                        onLocationPinClick = calendarController::openLocationPinAction,
                                        onReminderClick = calendarController::openReminderEditor,
                                        onEditClick = calendarController::openEditEvent,
                                        onDeleteClick = calendarController::requestDeleteEvent,
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
                                        isPaused = calendarState.hasOpenDialog,
                                    )
                                }
                                AppTab.NOTES -> {
                                    NotesScreen(
                                        notes = notesState.notes,
                                        eventsById = notesState.eventsById,
                                        onEditClick = notesController::openEditNote,
                                        onDeleteClick = notesController::requestDeleteNote,
                                        onOpenAttachedEvent = calendarController::openEventDetails,
                                    )
                                }
                                AppTab.SETTINGS -> {
                                    SettingsScreen(
                                        uiState = settingsState,
                                        onToggleColorWheelExpanded = settingsController::toggleColorWheelExpanded,
                                        onToggleApplyToAppTheme = settingsController::setApplyToAppTheme,
                                        onSelectSurfaceMode = settingsController::selectSurfaceMode,
                                        onSelectColorCount = settingsController::selectColorCount,
                                        onResetToDefaultPalette = settingsController::resetToDefaultPalette,
                                        onSelectHarmonyRule = settingsController::selectHarmonyRule,
                                        onSwatchDragged = settingsController::updateSwatchOnWheel,
                                        onSelectSwatch = settingsController::selectSwatch,
                                        onActiveSwatchValueChange = settingsController::updateActiveSwatchValue,
                                        onCommitContinuousAdjustment = settingsController::commitContinuousAdjustment,
                                        onOpenSavePaletteDialog = settingsController::openSavePaletteDialog,
                                        onDismissSavePaletteDialog = settingsController::dismissSavePaletteDialog,
                                        onConfirmSavePalette = settingsController::saveNamedPalette,
                                        onRestoreSavedPalette = settingsController::restoreSavedPalette,
                                        onDeleteSavedPalette = settingsController::deleteSavedPalette,
                                        onOpenDefaultReminderPicker = settingsController::openDefaultReminderPicker,
                                        onDismissDefaultReminderPicker = settingsController::dismissDefaultReminderPicker,
                                        onConfirmDefaultReminderTime = settingsController::updateDefaultReminderTime,
                                        onSelectSnoozeMinutes = settingsController::updateDefaultSnoozeMinutes,
                                        onSelectWeekStartsOnMonday = settingsController::setWeekStartsOnMonday,
                                        isTransferBusy = transferState.isBusy,
                                        onExportJsonBackup = launchers.launchExportJsonBackup,
                                        onImportJsonBackup = launchers.launchImportJsonBackup,
                                        onExportIcs = launchers.launchExportIcs,
                                        onImportIcs = launchers.launchImportIcs,
                                        onExportCsv = { launchers.launchExportCsv(false) },
                                        onImportCsv = launchers.launchImportCsv,
                                    )
                                }
                            }
                        }

                        CreateEntrySpeedDialScrim(
                            expanded = isCreateMenuExpanded &&
                                (shellState.activeTab == AppTab.CALENDAR || shellState.activeTab == AppTab.MAP),
                            onDismiss = { isCreateMenuExpanded = false },
                        )
                    }
                }
            }
        }

        CalendarDialogHost(
            uiState = calendarState,
            todayDay = calendarState.todayDateKey.toCalendarDay() ?: container.clockProvider.today(),
            controller = calendarController,
            geocodingService = container.geocodingService,
            locationProvider = container.deviceLocationProvider,
            onEnsureNotificationPermission = launchers.ensureNotificationPermission,
            onShowEventOnMainMap = shellController::showEventOnMap,
        )

        NotesDialogHost(
            dialog = notesState.dialog,
            availableEvents = notesState.availableEvents,
            controller = notesController,
        )

        transferState.pendingBackupRestore?.let { pending ->
            BackupRestoreDialog(
                preview = pending.preview,
                onSelectMode = transferController::confirmRestoreBackup,
                onDismiss = transferController::dismissBackupRestoreDialog,
            )
        }
    }
}
