package com.example.feature.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.Note
import com.example.ui.components.EmptySectionCard
import com.example.ui.theme.AppIcons

@Composable
fun NotesScreen(
    notes: List<Note>,
    onEditClick: (Note) -> Unit,
    onDeleteClick: (Note) -> Unit,
    onLoadDemoData: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("notes_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (notes.isEmpty()) {
            item(key = "empty_notes") {
                EmptySectionCard(
                    iconRes = AppIcons.noteItem,
                    iconDescription = stringResource(R.string.cd_note_icon),
                    message = stringResource(R.string.notes_empty),
                    testTag = "notes_empty_state",
                    actionLabel = stringResource(R.string.demo_load_action),
                    actionTestTag = "notes_empty_load_demo_button",
                    onActionClick = onLoadDemoData,
                )
            }
        } else {
            items(
                items = notes,
                key = { it.id.value },
            ) { note ->
                NoteItemCard(
                    note = note,
                    onEditClick = { onEditClick(note) },
                    onDeleteClick = { onDeleteClick(note) },
                )
            }
        }
    }
}
