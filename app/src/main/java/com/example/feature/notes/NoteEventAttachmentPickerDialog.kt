package com.example.feature.notes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.CalendarEvent
import com.example.domain.model.EventId
import com.example.domain.service.PolishDateFormatter
import com.example.feature.calendar.CalendarEntryVisuals

@Composable
internal fun NoteEventAttachmentPickerDialog(
    availableEvents: List<CalendarEvent>,
    initialSelectedIds: List<EventId>,
    onConfirm: (List<EventId>) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var selectedIds by remember(initialSelectedIds) {
        mutableStateOf(initialSelectedIds.toSet())
    }

    val filteredEvents = remember(availableEvents, query) {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) {
            availableEvents
        } else {
            availableEvents.filter { event ->
                event.title.lowercase().contains(needle) ||
                    event.dateKey.value.contains(needle) ||
                    PolishDateFormatter.formatDateKeyDisplay(event.dateKey).lowercase().contains(needle)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.note_attachments_picker_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (availableEvents.isEmpty()) {
                    Text(
                        text = stringResource(R.string.note_attachments_picker_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text(stringResource(R.string.note_attachments_picker_search)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("note_attachment_picker_search"),
                    )

                    if (filteredEvents.isEmpty()) {
                        Text(
                            text = stringResource(R.string.note_attachments_picker_no_matches),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(
                                items = filteredEvents,
                                key = { it.id.value },
                            ) { event ->
                                val isChecked = event.id in selectedIds
                                val toggle = {
                                    selectedIds = if (isChecked) {
                                        selectedIds - event.id
                                    } else {
                                        selectedIds + event.id
                                    }
                                }
                                PickerEventRow(
                                    event = event,
                                    checked = isChecked,
                                    onToggle = toggle,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val ordered = availableEvents
                        .map { it.id }
                        .filter { it in selectedIds }
                    onConfirm(ordered)
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("note_attachment_picker_confirm"),
            ) {
                Text(stringResource(R.string.note_attachments_picker_done, selectedIds.size))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("note_attachment_picker_cancel"),
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun PickerEventRow(
    event: CalendarEvent,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    val palette = CalendarEntryVisuals.palette(event.colorTag)
    val dateText = PolishDateFormatter.formatDateKeyDisplay(event.dateKey)
    val timeText = event.timeRange?.formatDisplay()
    val subtitle = if (timeText != null) "$dateText · $timeText" else dateText

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (checked) {
            palette.container.copy(alpha = 0.75f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (checked) palette.accent else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .testTag("note_attachment_picker_item_${event.id.value}"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = { onToggle() },
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = palette.accent.copy(alpha = 0.16f),
                modifier = Modifier.size(32.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(CalendarEntryVisuals.iconRes(event.entryType)),
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
