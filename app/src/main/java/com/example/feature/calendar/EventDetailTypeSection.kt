package com.example.feature.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.CalendarEvent
import com.example.domain.model.EventTypeDetails

@Composable
internal fun EventDetailTypeSection(
    event: CalendarEvent,
    modifier: Modifier = Modifier,
) {
    val rows = collectDetailRows(event)
    val checklistInfo = collectChecklist(event)

    if (!event.hasDescription && rows.isEmpty() && checklistInfo == null) {
        return
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (event.hasDescription) {
                DetailLabeledBlock(
                    label = stringResource(R.string.detail_description_label),
                    value = event.description,
                )
            }

            rows.forEach { (label, value) ->
                DetailLabeledBlock(label = label, value = value)
            }

            checklistInfo?.let { (label, items) ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    items.forEach { item ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailLabeledBlock(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun collectDetailRows(event: CalendarEvent): List<Pair<String, String>> =
    when (val details = event.details) {
        is EventTypeDetails.StandardEvent -> buildList {
            if (details.guests.isNotEmpty()) {
                add(stringResource(R.string.detail_guests_label) to details.guests.joinToString(", "))
            }
            details.meetingLink?.let { link ->
                add(stringResource(R.string.detail_meeting_link_label) to link)
            }
        }

        is EventTypeDetails.Task -> buildList {
            add(
                stringResource(R.string.field_task_priority_label) to
                    stringResource(CalendarEntryVisuals.priorityLabelRes(details.priority))
            )
            add(
                stringResource(R.string.entry_form_details_section) to
                    stringResource(
                        if (details.isCompleted) R.string.task_status_completed else R.string.task_status_pending
                    )
            )
        }

        is EventTypeDetails.Birthday -> buildList {
            details.celebrantName?.let { name ->
                add(stringResource(R.string.detail_celebrant_label) to name)
            }
            details.birthYear?.let { year ->
                val targetYear = event.dateKey.toCalendarDay()?.year
                val ageSuffix = targetYear?.let { details.turningAgeIn(it) }?.let { age ->
                    " (${stringResource(R.string.detail_turning_age_badge, age)})"
                }.orEmpty()
                add(stringResource(R.string.detail_birth_year_label) to "$year$ageSuffix")
            }
            details.giftIdea?.let { gift ->
                add(stringResource(R.string.detail_gift_idea_label) to gift)
            }
        }

        is EventTypeDetails.Appointment -> buildList {
            details.specialistName?.let { specialist ->
                add(stringResource(R.string.detail_specialist_label) to specialist)
            }
            details.contactPhone?.let { phone ->
                add(stringResource(R.string.detail_contact_phone_label) to phone)
            }
            details.preparationNotes?.let { notes ->
                add(stringResource(R.string.detail_preparation_label) to notes)
            }
        }

        is EventTypeDetails.Trip -> buildList {
            add(
                stringResource(R.string.field_transport_mode_label) to
                    stringResource(CalendarEntryVisuals.transportLabelRes(details.transportMode))
            )
            details.bookingReference?.let { booking ->
                add(stringResource(R.string.detail_booking_ref_label) to booking)
            }
        }
    }

@Composable
private fun collectChecklist(event: CalendarEvent): Pair<String, List<String>>? =
    when (val details = event.details) {
        is EventTypeDetails.Task -> details.checklist.takeIf { it.isNotEmpty() }?.let {
            stringResource(R.string.detail_task_checklist_label) to it
        }
        is EventTypeDetails.Trip -> details.packingChecklist.takeIf { it.isNotEmpty() }?.let {
            stringResource(R.string.detail_packing_list_label) to it
        }
        else -> null
    }
