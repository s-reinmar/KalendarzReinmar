package com.example.feature.shell

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShellTopBar(
    activeTab: AppTab,
    eventsCount: Int,
    mapEventsCount: Int,
    notesCount: Int,
    launchers: ShellActivityLaunchers,
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = when (activeTab) {
                        AppTab.CALENDAR -> stringResource(R.string.nav_calendar)
                        AppTab.MAP -> stringResource(R.string.nav_map)
                        AppTab.NOTES -> stringResource(R.string.nav_notes)
                        AppTab.SETTINGS -> stringResource(R.string.nav_settings)
                    },
                    style = MaterialTheme.typography.headlineMedium,
                )
                val subtitle = when (activeTab) {
                    AppTab.CALENDAR -> stringResource(R.string.calendar_summary_count, eventsCount)
                    AppTab.MAP -> stringResource(R.string.map_events_count, mapEventsCount)
                    AppTab.NOTES -> stringResource(R.string.notes_summary_count, notesCount)
                    AppTab.SETTINGS -> null
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                    )
                }
            }
        },
        actions = {
            TopBarOverflowMenu(
                onExportJson = launchers.launchExportJsonBackup,
                onImportJson = launchers.launchImportJsonBackup,
                onImportIcs = launchers.launchImportIcs,
                onExportIcs = launchers.launchExportIcs,
                onImportCsv = launchers.launchImportCsv,
                onExportCsv = { launchers.launchExportCsv(activeTab == AppTab.NOTES) },
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        windowInsets = WindowInsets.statusBars,
    )
}
