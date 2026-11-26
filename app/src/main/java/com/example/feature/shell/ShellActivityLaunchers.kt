package com.example.feature.shell

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.R
import com.example.feature.transfer.TransferController

internal class ShellActivityLaunchers(
    val ensureNotificationPermission: () -> Unit,
    val launchExportJsonBackup: () -> Unit,
    val launchImportJsonBackup: () -> Unit,
    val launchImportIcs: () -> Unit,
    val launchExportIcs: () -> Unit,
    val launchImportCsv: () -> Unit,
    val launchExportCsv: (exportNotes: Boolean) -> Unit,
)

@Composable
internal fun rememberShellActivityLaunchers(
    activeTab: AppTab,
    transferController: TransferController,
    onShowMessage: (String) -> Unit,
): ShellActivityLaunchers {
    val context = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) {
            onShowMessage(context.getString(R.string.reminder_permission_denied))
        }
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let { transferController.exportFullBackup(it.toString()) } }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { transferController.prepareImportFullBackup(it.toString()) } }

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
                preferNotesWhenAmbiguous = activeTab == AppTab.NOTES,
            )
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        uri?.let {
            transferController.exportCsv(
                documentUri = it.toString(),
                exportNotes = activeTab == AppTab.NOTES,
            )
        }
    }

    return remember(
        context,
        notificationPermissionLauncher,
        exportJsonLauncher,
        importJsonLauncher,
        importIcsLauncher,
        exportIcsLauncher,
        importCsvLauncher,
        exportCsvLauncher,
    ) {
        ShellActivityLaunchers(
            ensureNotificationPermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!granted) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            },
            launchExportJsonBackup = {
                exportJsonLauncher.launch("kalendarz_reinmar_backup.json")
            },
            launchImportJsonBackup = {
                importJsonLauncher.launch(arrayOf("application/json", "*/*"))
            },
            launchImportIcs = {
                importIcsLauncher.launch(arrayOf("*/*"))
            },
            launchExportIcs = {
                exportIcsLauncher.launch("kalendarz_reinmar.ics")
            },
            launchImportCsv = {
                importCsvLauncher.launch(arrayOf("*/*"))
            },
            launchExportCsv = { exportNotes ->
                val fileName = if (exportNotes) {
                    "kalendarz_reinmar_notatki.csv"
                } else {
                    "kalendarz_reinmar_wydarzenia.csv"
                }
                exportCsvLauncher.launch(fileName)
            },
        )
    }
}
