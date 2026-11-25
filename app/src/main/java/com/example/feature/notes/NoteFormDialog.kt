package com.example.feature.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.domain.model.Note

@Composable
fun NoteFormDialog(
    existingNote: Note? = null,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String) -> Unit,
) {
    var title by remember(existingNote?.id) { mutableStateOf(existingNote?.title.orEmpty()) }
    var content by remember(existingNote?.id) { mutableStateOf(existingNote?.content.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    if (existingNote == null) R.string.notes_add_note else R.string.notes_edit_note,
                ),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.notes_title_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_title_input"),
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(stringResource(R.string.notes_content_label)) },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_content_input"),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (content.trim().isNotEmpty()) {
                        onSave(title, content)
                    }
                },
                enabled = content.trim().isNotEmpty(),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("note_save_button"),
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("note_cancel_button"),
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
