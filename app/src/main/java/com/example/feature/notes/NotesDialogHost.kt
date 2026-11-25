package com.example.feature.notes

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.ui.components.ConfirmDeleteDialog

@Composable
fun NotesDialogHost(
    dialog: NotesDialogState,
    controller: NotesController,
) {
    when (dialog) {
        NotesDialogState.None -> Unit

        NotesDialogState.Create -> {
            NoteFormDialog(
                onDismiss = controller::dismissDialog,
                onSave = controller::createNote,
            )
        }

        is NotesDialogState.Edit -> {
            NoteFormDialog(
                existingNote = dialog.note,
                onDismiss = controller::dismissDialog,
                onSave = { title, content ->
                    controller.updateNote(note = dialog.note, title = title, content = content)
                },
            )
        }

        is NotesDialogState.ConfirmDelete -> {
            ConfirmDeleteDialog(
                title = stringResource(R.string.notes_delete_note_title),
                message = stringResource(R.string.notes_delete_note_message),
                confirmTestTag = "confirm_delete_note_button",
                cancelTestTag = "cancel_delete_note_button",
                onConfirm = { controller.confirmDeleteNote(dialog.note) },
                onDismiss = controller::dismissDialog,
            )
        }
    }
}
