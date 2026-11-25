package com.example.feature.notes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.CalendarEvent
import com.example.domain.model.EventId
import com.example.domain.service.PolishDateFormatter
import com.example.feature.calendar.CalendarEntryVisuals

@Composable
internal fun NoteEditorAttachmentsSection(
    attachedEvents: List<CalendarEvent>,
    onOpenPicker: () -> Unit,
    onRemoveEvent: (EventId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .testTag("note_editor_attachments_section"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (attachedEvents.isEmpty()) {
                        stringResource(R.string.note_attachments_section_title)
                    } else {
                        stringResource(R.string.note_attachments_section_count, attachedEvents.size)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                FilledTonalButton(
                    onClick = onOpenPicker,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("note_attach_event_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.note_attachments_add_action),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            if (attachedEvents.isEmpty()) {
                Text(
                    text = stringResource(R.string.note_attachments_empty_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (event in attachedEvents) {
                        EditorAttachedEventCard(
                            event = event,
                            onRemove = { onRemoveEvent(event.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorAttachedEventCard(
    event: CalendarEvent,
    onRemove: () -> Unit,
) {
    val palette = CalendarEntryVisuals.palette(event.colorTag)
    val dateLabel = PolishDateFormatter.formatDateKeyDisplay(event.dateKey)
    val timeLabel = event.timeRange?.formatDisplay()
    val scheduleText = if (timeLabel != null) "$dateLabel · $timeLabel" else dateLabel

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = palette.container.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("note_attached_event_row_${event.id.value}"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
                    color = palette.onContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = scheduleText,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.onContainer.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("note_detach_event_${event.id.value}"),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.note_attachment_remove_cd),
                    tint = palette.onContainer,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NoteCardAttachedEventsRow(
    attachedEvents: List<CalendarEvent>,
    onEventClick: (EventId) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (attachedEvents.isEmpty()) return
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (event in attachedEvents) {
            val palette = CalendarEntryVisuals.palette(event.colorTag)
            val openDesc = stringResource(R.string.note_attachment_open_cd, event.title)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = palette.container,
                border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.3f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button) { onEventClick(event.id) }
                    .semantics { contentDescription = openDesc }
                    .testTag("note_card_attached_event_${event.id.value}"),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        painter = painterResource(CalendarEntryVisuals.iconRes(event.entryType)),
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = "${event.title} · ${PolishDateFormatter.formatDateKeyDisplay(event.dateKey)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.onContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
