package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.data.CalendarEventEntity
import com.example.ui.theme.AppIcons
import kotlinx.coroutines.launch

enum class ReinmarTab {
    CALENDAR,
    NOTES,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReinmarApp(viewModel: ReinmarViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentTab by rememberSaveable { mutableStateOf(ReinmarTab.CALENDAR) }
    var showAddEventDialog by rememberSaveable { mutableStateOf(false) }
    var showAddNoteDialog by rememberSaveable { mutableStateOf(false) }
    var showTransferMenu by remember { mutableStateOf(false) }
    var reminderDialogEvent by remember { mutableStateOf<CalendarEventEntity?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    context.getString(R.string.reminder_permission_denied),
                )
            }
        }
    }

    fun requestNotificationPermissionIfNeeded() {
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
    ) { uri ->
        if (uri != null) {
            viewModel.importIcs(context.contentResolver, uri)
        }
    }

    val exportIcsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/calendar"),
    ) { uri ->
        if (uri != null) {
            viewModel.exportIcs(context.contentResolver, uri)
        }
    }

    val importCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.importCsv(
                contentResolver = context.contentResolver,
                uri = uri,
                preferNotesWhenAmbiguous = currentTab == ReinmarTab.NOTES,
            )
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) {
            viewModel.exportCsv(
                contentResolver = context.contentResolver,
                uri = uri,
                exportNotes = currentTab == ReinmarTab.NOTES,
            )
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.transferFeedback.collect { feedback ->
            val message = when (feedback) {
                is TransferFeedback.ImportedEvents -> {
                    context.getString(R.string.transfer_imported_events, feedback.count)
                }
                is TransferFeedback.ImportedNotes -> {
                    context.getString(R.string.transfer_imported_notes, feedback.count)
                }
                is TransferFeedback.ImportedBoth -> {
                    context.getString(
                        R.string.transfer_imported_both,
                        feedback.eventsCount,
                        feedback.notesCount,
                    )
                }
                is TransferFeedback.ExportedEvents -> {
                    context.getString(R.string.transfer_exported_events, feedback.count)
                }
                is TransferFeedback.ExportedNotes -> {
                    context.getString(R.string.transfer_exported_notes, feedback.count)
                }
                TransferFeedback.NothingToImport -> {
                    context.getString(R.string.transfer_nothing_to_import)
                }
                TransferFeedback.Error -> {
                    context.getString(R.string.transfer_error)
                }
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    BackHandler(enabled = currentTab != ReinmarTab.CALENDAR) {
        currentTab = ReinmarTab.CALENDAR
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useNavigationRail) {
                NavigationRail(
                    modifier = Modifier
                        .fillMaxHeight()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    NavigationRailItem(
                        selected = currentTab == ReinmarTab.CALENDAR,
                        onClick = { currentTab = ReinmarTab.CALENDAR },
                        icon = {
                            Icon(
                                painter = painterResource(AppIcons.calendar),
                                contentDescription = stringResource(R.string.cd_calendar_icon),
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        label = { Text(stringResource(R.string.nav_calendar)) },
                        modifier = Modifier.testTag("nav_tab_calendar"),
                    )
                    NavigationRailItem(
                        selected = currentTab == ReinmarTab.NOTES,
                        onClick = { currentTab = ReinmarTab.NOTES },
                        icon = {
                            Icon(
                                painter = painterResource(AppIcons.notes),
                                contentDescription = stringResource(R.string.cd_notes_icon),
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        label = { Text(stringResource(R.string.nav_notes)) },
                        modifier = Modifier.testTag("nav_tab_notes"),
                    )
                }
            }

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = when (currentTab) {
                                    ReinmarTab.CALENDAR -> stringResource(R.string.nav_calendar)
                                    ReinmarTab.NOTES -> stringResource(R.string.nav_notes)
                                },
                                style = MaterialTheme.typography.headlineMedium,
                            )
                        },
                        actions = {
                            Box {
                                IconButton(
                                    onClick = { showTransferMenu = true },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("transfer_menu_button"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.MoreVert,
                                        contentDescription = stringResource(R.string.transfer_menu_description),
                                    )
                                }
                                DropdownMenu(
                                    expanded = showTransferMenu,
                                    onDismissRequest = { showTransferMenu = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.transfer_import_ics)) },
                                        onClick = {
                                            showTransferMenu = false
                                            importIcsLauncher.launch(arrayOf("*/*"))
                                        },
                                        modifier = Modifier.testTag("menu_import_ics"),
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.transfer_export_ics)) },
                                        onClick = {
                                            showTransferMenu = false
                                            exportIcsLauncher.launch("kalendarz_reinmar.ics")
                                        },
                                        modifier = Modifier.testTag("menu_export_ics"),
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.transfer_import_csv)) },
                                        onClick = {
                                            showTransferMenu = false
                                            importCsvLauncher.launch(arrayOf("*/*"))
                                        },
                                        modifier = Modifier.testTag("menu_import_csv"),
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.transfer_export_csv)) },
                                        onClick = {
                                            showTransferMenu = false
                                            val fileName = if (currentTab == ReinmarTab.NOTES) {
                                                "kalendarz_reinmar_notatki.csv"
                                            } else {
                                                "kalendarz_reinmar_wydarzenia.csv"
                                            }
                                            exportCsvLauncher.launch(fileName)
                                        },
                                        modifier = Modifier.testTag("menu_export_csv"),
                                    )
                                }
                            }
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
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            windowInsets = WindowInsets.navigationBars,
                        ) {
                            NavigationBarItem(
                                selected = currentTab == ReinmarTab.CALENDAR,
                                onClick = { currentTab = ReinmarTab.CALENDAR },
                                icon = {
                                    Icon(
                                        painter = painterResource(AppIcons.calendar),
                                        contentDescription = stringResource(R.string.cd_calendar_icon),
                                        modifier = Modifier.size(22.dp),
                                    )
                                },
                                label = { Text(stringResource(R.string.nav_calendar)) },
                                modifier = Modifier.testTag("nav_tab_calendar"),
                            )
                            NavigationBarItem(
                                selected = currentTab == ReinmarTab.NOTES,
                                onClick = { currentTab = ReinmarTab.NOTES },
                                icon = {
                                    Icon(
                                        painter = painterResource(AppIcons.notes),
                                        contentDescription = stringResource(R.string.cd_notes_icon),
                                        modifier = Modifier.size(22.dp),
                                    )
                                },
                                label = { Text(stringResource(R.string.nav_notes)) },
                                modifier = Modifier.testTag("nav_tab_notes"),
                            )
                        }
                    }
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                            when (currentTab) {
                                ReinmarTab.CALENDAR -> showAddEventDialog = true
                                ReinmarTab.NOTES -> showAddNoteDialog = true
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag(
                            when (currentTab) {
                                ReinmarTab.CALENDAR -> "fab_add_event"
                                ReinmarTab.NOTES -> "fab_add_note"
                            }
                        ),
                    ) {
                        Icon(
                            painter = painterResource(AppIcons.add),
                            contentDescription = when (currentTab) {
                                ReinmarTab.CALENDAR -> stringResource(R.string.calendar_add_event)
                                ReinmarTab.NOTES -> stringResource(R.string.notes_add_note)
                            },
                            modifier = Modifier.size(24.dp),
                        )
                    }
                },
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 640.dp),
                    ) {
                        when (currentTab) {
                            ReinmarTab.CALENDAR -> {
                                CalendarScreen(
                                    uiState = uiState,
                                    onSelectDate = viewModel::selectDate,
                                    onPreviousMonth = viewModel::previousMonth,
                                    onNextMonth = viewModel::nextMonth,
                                    onReminderClick = { event -> reminderDialogEvent = event },
                                )
                            }
                            ReinmarTab.NOTES -> {
                                NotesScreen(notes = uiState.notes)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddEventDialog) {
        AddEventDialog(
            initialDateKey = uiState.selectedDateKey ?: uiState.todayDateKey,
            onDismiss = { showAddEventDialog = false },
            onSave = { title, dateKey, reminderTimeMinutes ->
                if (reminderTimeMinutes != null) {
                    requestNotificationPermissionIfNeeded()
                }
                viewModel.addEvent(
                    context = context,
                    title = title,
                    dateKey = dateKey,
                    reminderTimeMinutes = reminderTimeMinutes,
                )
                showAddEventDialog = false
            },
        )
    }

    reminderDialogEvent?.let { event ->
        EventReminderDialog(
            event = event,
            onDismiss = { reminderDialogEvent = null },
            onSave = { reminderTimeMinutes ->
                if (reminderTimeMinutes != null) {
                    requestNotificationPermissionIfNeeded()
                }
                viewModel.updateEventReminder(
                    context = context,
                    event = event,
                    reminderTimeMinutes = reminderTimeMinutes,
                )
                reminderDialogEvent = null
            },
        )
    }

    if (showAddNoteDialog) {
        AddNoteDialog(
            onDismiss = { showAddNoteDialog = false },
            onSave = { title, content ->
                viewModel.addNote(title = title, content = content)
                showAddNoteDialog = false
            },
        )
    }
}
