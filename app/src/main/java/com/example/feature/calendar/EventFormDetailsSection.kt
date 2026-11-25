package com.example.feature.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.CalendarEntryType
import com.example.domain.model.TaskPriority
import com.example.domain.model.TransportMode

internal data class TypeSpecificFormState(
    val guestsRaw: String = "",
    val meetingLink: String = "",
    val taskPriority: TaskPriority = TaskPriority.MEDIUM,
    val taskCompleted: Boolean = false,
    val taskChecklistRaw: String = "",
    val celebrantName: String = "",
    val birthYearRaw: String = "",
    val giftIdea: String = "",
    val specialistName: String = "",
    val contactPhone: String = "",
    val preparationNotes: String = "",
    val transportMode: TransportMode = TransportMode.CAR,
    val bookingReference: String = "",
    val packingChecklistRaw: String = "",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EventFormDetailsSection(
    entryType: CalendarEntryType,
    description: String,
    onDescriptionChange: (String) -> Unit,
    typeState: TypeSpecificFormState,
    onTypeStateChange: (TypeSpecificFormState) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.entry_form_details_section),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text(stringResource(R.string.entry_description_label)) },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("event_description_input"),
        )

        when (entryType) {
            CalendarEntryType.EVENT -> {
                OutlinedTextField(
                    value = typeState.guestsRaw,
                    onValueChange = { onTypeStateChange(typeState.copy(guestsRaw = it)) },
                    label = { Text(stringResource(R.string.field_guests_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("event_guests_input"),
                )
                OutlinedTextField(
                    value = typeState.meetingLink,
                    onValueChange = { onTypeStateChange(typeState.copy(meetingLink = it)) },
                    label = { Text(stringResource(R.string.field_meeting_link_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("event_meeting_link_input"),
                )
            }

            CalendarEntryType.TASK -> {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.field_task_priority_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        TaskPriority.entries.forEach { priority ->
                            FilterChip(
                                selected = typeState.taskPriority == priority,
                                onClick = { onTypeStateChange(typeState.copy(taskPriority = priority)) },
                                label = { Text(stringResource(CalendarEntryVisuals.priorityLabelRes(priority))) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("task_priority_${priority.code.lowercase()}"),
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.field_task_completed_label),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Switch(
                        checked = typeState.taskCompleted,
                        onCheckedChange = { onTypeStateChange(typeState.copy(taskCompleted = it)) },
                        modifier = Modifier.testTag("task_completed_switch"),
                    )
                }
                OutlinedTextField(
                    value = typeState.taskChecklistRaw,
                    onValueChange = { onTypeStateChange(typeState.copy(taskChecklistRaw = it)) },
                    label = { Text(stringResource(R.string.field_task_checklist_label)) },
                    minLines = 2,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_checklist_input"),
                )
            }

            CalendarEntryType.BIRTHDAY -> {
                OutlinedTextField(
                    value = typeState.celebrantName,
                    onValueChange = { onTypeStateChange(typeState.copy(celebrantName = it)) },
                    label = { Text(stringResource(R.string.field_celebrant_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("birthday_celebrant_input"),
                )
                OutlinedTextField(
                    value = typeState.birthYearRaw,
                    onValueChange = { raw ->
                        val digitsOnly = raw.filter(Char::isDigit).take(4)
                        onTypeStateChange(typeState.copy(birthYearRaw = digitsOnly))
                    },
                    label = { Text(stringResource(R.string.field_birth_year_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("birthday_birth_year_input"),
                )
                OutlinedTextField(
                    value = typeState.giftIdea,
                    onValueChange = { onTypeStateChange(typeState.copy(giftIdea = it)) },
                    label = { Text(stringResource(R.string.field_gift_idea_label)) },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("birthday_gift_input"),
                )
            }

            CalendarEntryType.APPOINTMENT -> {
                OutlinedTextField(
                    value = typeState.specialistName,
                    onValueChange = { onTypeStateChange(typeState.copy(specialistName = it)) },
                    label = { Text(stringResource(R.string.field_specialist_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("appointment_specialist_input"),
                )
                OutlinedTextField(
                    value = typeState.contactPhone,
                    onValueChange = { onTypeStateChange(typeState.copy(contactPhone = it)) },
                    label = { Text(stringResource(R.string.field_contact_phone_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("appointment_phone_input"),
                )
                OutlinedTextField(
                    value = typeState.preparationNotes,
                    onValueChange = { onTypeStateChange(typeState.copy(preparationNotes = it)) },
                    label = { Text(stringResource(R.string.field_preparation_label)) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("appointment_preparation_input"),
                )
            }

            CalendarEntryType.TRIP -> {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.field_transport_mode_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        TransportMode.entries.forEach { mode ->
                            FilterChip(
                                selected = typeState.transportMode == mode,
                                onClick = { onTypeStateChange(typeState.copy(transportMode = mode)) },
                                label = { Text(stringResource(CalendarEntryVisuals.transportLabelRes(mode))) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("trip_transport_${mode.code.lowercase()}"),
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = typeState.bookingReference,
                    onValueChange = { onTypeStateChange(typeState.copy(bookingReference = it)) },
                    label = { Text(stringResource(R.string.field_booking_ref_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("trip_booking_input"),
                )
                OutlinedTextField(
                    value = typeState.packingChecklistRaw,
                    onValueChange = { onTypeStateChange(typeState.copy(packingChecklistRaw = it)) },
                    label = { Text(stringResource(R.string.field_packing_list_label)) },
                    minLines = 2,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("trip_packing_input"),
                )
            }
        }
    }
}
