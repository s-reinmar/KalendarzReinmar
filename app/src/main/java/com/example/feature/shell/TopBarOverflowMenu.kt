package com.example.feature.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R

@Composable
internal fun TopBarOverflowMenu(
    onLoadDemoData: () -> Unit,
    onClearDemoData: () -> Unit,
    onImportIcs: () -> Unit,
    onExportIcs: () -> Unit,
    onImportCsv: () -> Unit,
    onExportCsv: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { expanded = true },
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
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            MenuSectionLabel(text = stringResource(R.string.menu_section_demo))
            DropdownMenuItem(
                text = { Text(stringResource(R.string.demo_load_action)) },
                onClick = {
                    expanded = false
                    onLoadDemoData()
                },
                modifier = Modifier.testTag("menu_load_demo"),
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.demo_clear_action)) },
                onClick = {
                    expanded = false
                    onClearDemoData()
                },
                modifier = Modifier.testTag("menu_clear_demo"),
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            MenuSectionLabel(text = stringResource(R.string.menu_section_transfer))
            DropdownMenuItem(
                text = { Text(stringResource(R.string.transfer_import_ics)) },
                onClick = {
                    expanded = false
                    onImportIcs()
                },
                modifier = Modifier.testTag("menu_import_ics"),
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.transfer_export_ics)) },
                onClick = {
                    expanded = false
                    onExportIcs()
                },
                modifier = Modifier.testTag("menu_export_ics"),
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.transfer_import_csv)) },
                onClick = {
                    expanded = false
                    onImportCsv()
                },
                modifier = Modifier.testTag("menu_import_csv"),
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.transfer_export_csv)) },
                onClick = {
                    expanded = false
                    onExportCsv()
                },
                modifier = Modifier.testTag("menu_export_csv"),
            )
        }
    }
}

@Composable
private fun MenuSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
    )
}
