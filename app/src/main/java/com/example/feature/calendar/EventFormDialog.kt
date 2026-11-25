package com.example.feature.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.domain.model.CalendarDay
import com.example.domain.model.CalendarEntryType
import com.example.domain.model.CalendarEvent
import com.example.domain.model.DateKey
import com.example.domain.model.EventDraft
import com.example.domain.model.EventLocation
import com.example.domain.model.EventTimeRange
import com.example.domain.model.EventTypeDetails
import com.example.domain.model.ReminderTime
import com.example.domain.model.TimeOfDay
import com.example.domain.port.DeviceLocationProvider
import com.example.domain.port.GeocodingService
import com.example.domain.service.CalendarGridBuilder
import com.example.feature.map.MapLocationPickerDialog

private enum class ActiveDatePickerTarget {
    START,
    END,
}

private enum class ActiveTimePickerTarget {
    START,
    END,
    REMINDER,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventFormDialog(
    initialDateKey: DateKey,
    todayDay: CalendarDay,
    geocodingService: GeocodingService,
    locationProvider: DeviceLocationProvider,
    initialEntryType: CalendarEntryType = CalendarEntryType.EVENT,
    existingEvent: CalendarEvent? = null,
    initialLocation: EventLocation? = null,
    onDismiss: () -> Unit,
    onSave: (EventDraft) -> Unit,
) {
    val initialDraft = remember(existingEvent?.id, initialDateKey, initialEntryType, initialLocation) {
        existingEvent?.toDraft() ?: EventDraft.forNewEntry(
            entryType = initialEntryType,
            initialDateKey = initialDateKey,
            initialLocation = initialLocation,
        )
    }

    var entryType by remember(initialDraft) { mutableStateOf(initialDraft.entryType) }
    var colorTag by remember(initialDraft) { mutableStateOf(initialDraft.colorTag) }
    var title by remember(initialDraft) { mutableStateOf(initialDraft.title) }
    var startDateKey by remember(initialDraft) { mutableStateOf(initialDraft.dateKey) }
    var endDateKey by remember(initialDraft) { mutableStateOf(initialDraft.endDateKey) }
    var isAllDay by remember(initialDraft) { mutableStateOf(initialDraft.isAllDay) }
    var startTime by remember(initialDraft) {
        mutableStateOf(initialDraft.timeRange?.start ?: TimeOfDay.DEFAULT_START)
    }
    var endTime by remember(initialDraft) {
        mutableStateOf(initialDraft.timeRange?.end ?: startTime.plusMinutes(60))
    }
    var recurrence by remember(initialDraft) { mutableStateOf(initialDraft.recurrence) }
    var description by remember(initialDraft) { mutableStateOf(initialDraft.description) }
    var reminderEnabled by remember(initialDraft) { mutableStateOf(initialDraft.reminderTime != null) }
    var reminderTime by remember(initialDraft) {
        mutableStateOf(initialDraft.reminderTime ?: ReminderTime.DEFAULT)
    }
    var selectedLocation by remember(initialDraft) { mutableStateOf(initialDraft.location) }
    var typeState by remember(initialDraft) {
        mutableStateOf(extractTypeFormState(initialDraft.details))
    }

    var datePickerTarget by remember { mutableStateOf<ActiveDatePickerTarget?>(null) }
    var timePickerTarget by remember { mutableStateOf<ActiveTimePickerTarget?>(null) }
    var showMapPicker by remember { mutableStateOf(false) }

    val entryTypeLabel = stringResource(CalendarEntryVisuals.labelRes(entryType))
    val dialogHeaderTitle = stringResource(
        if (existingEvent == null) R.string.calendar_add_entry_title else R.string.calendar_edit_entry_title,
        entryTypeLabel,
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 580.dp)
                .heightIn(max = 760.dp)
                .testTag("event_form_dialog"),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = dialogHeaderTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    EventFormTypeSection(
                        selectedType = entryType,
                        selectedColorTag = colorTag,
                        onSelectType = { newType ->
                            if (newType != entryType) {
                                val previousDefaultColor = entryType.defaultColorTag
                                val previousDefaultRecurrence = entryType.defaultRecurrence
                                entryType = newType
                                if (colorTag == previousDefaultColor) {
                                    colorTag = newType.defaultColorTag
                                }
                                if (existingEvent == null || recurrence == previousDefaultRecurrence) {
                                    recurrence = newType.defaultRecurrence
                                }
                                if (!newType.supportsTimeRange) {
                                    isAllDay = true
                                } else if (existingEvent == null) {
                                    isAllDay = newType.defaultAllDay
                                }
                                if (!newType.supportsEndDate) {
                                    endDateKey = null
                                }
                            }
                        },
                        onSelectColorTag = { colorTag = it },
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(stringResource(R.string.calendar_event_title_label)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("event_title_input"),
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    EventFormScheduleSection(
                        entryType = entryType,
                        startDateKey = startDateKey,
                        endDateKey = endDateKey,
                        isAllDay = isAllDay,
                        startTime = startTime,
                        endTime = endTime,
                        recurrence = recurrence,
                        reminderEnabled = reminderEnabled,
                        reminderTime = reminderTime,
                        selectedLocation = selectedLocation,
                        onPickStartDate = { datePickerTarget = ActiveDatePickerTarget.START },
                        onPickEndDate = { datePickerTarget = ActiveDatePickerTarget.END },
                        onClearEndDate = { endDateKey = null },
                        onAllDayChange = { isAllDay = it },
                        onPickStartTime = { timePickerTarget = ActiveTimePickerTarget.START },
                        onPickEndTime = { timePickerTarget = ActiveTimePickerTarget.END },
                        onRecurrenceChange = { recurrence = it },
                        onReminderToggle = { reminderEnabled = it },
                        onPickReminderTime = { timePickerTarget = ActiveTimePickerTarget.REMINDER },
                        onPickLocation = { showMapPicker = true },
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    EventFormDetailsSection(
                        entryType = entryType,
                        description = description,
                        onDescriptionChange = { description = it },
                        typeState = typeState,
                        onTypeStateChange = { typeState = it },
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("event_cancel_button"),
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.trim().isNotEmpty()) {
                                val effectiveAllDay = if (!entryType.supportsTimeRange) true else isAllDay
                                val timeRange = if (effectiveAllDay) {
                                    null
                                } else {
                                    EventTimeRange(start = startTime, end = endTime).normalized()
                                }
                                onSave(
                                    EventDraft(
                                        title = title,
                                        dateKey = startDateKey,
                                        endDateKey = if (entryType.supportsEndDate) endDateKey else null,
                                        isAllDay = effectiveAllDay,
                                        timeRange = timeRange,
                                        description = description,
                                        recurrence = recurrence,
                                        colorTag = colorTag,
                                        details = buildTypeDetails(entryType, typeState),
                                        reminderTime = if (reminderEnabled) reminderTime else null,
                                        location = selectedLocation,
                                    )
                                )
                            }
                        },
                        enabled = title.trim().isNotEmpty(),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("event_save_button"),
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                }
            }
        }
    }

    datePickerTarget?.let { target ->
        val seedKey = when (target) {
            ActiveDatePickerTarget.START -> startDateKey
            ActiveDatePickerTarget.END -> endDateKey ?: startDateKey
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = CalendarGridBuilder.dateKeyToUtcMillis(seedKey, todayDay),
        )
        DatePickerDialog(
            onDismissRequest = { datePickerTarget = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val picked = CalendarGridBuilder.utcMillisToDateKey(millis)
                            when (target) {
                                ActiveDatePickerTarget.START -> {
                                    startDateKey = picked
                                    if (endDateKey != null && endDateKey!! <= picked) {
                                        endDateKey = null
                                    }
                                }
                                ActiveDatePickerTarget.END -> {
                                    endDateKey = picked.takeIf { it > startDateKey }
                                }
                            }
                        }
                        datePickerTarget = null
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("date_picker_confirm_button"),
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { datePickerTarget = null },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("date_picker_cancel_button"),
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    timePickerTarget?.let { target ->
        val initialTime = when (target) {
            ActiveTimePickerTarget.START -> startTime.toReminderTime()
            ActiveTimePickerTarget.END -> endTime.toReminderTime()
            ActiveTimePickerTarget.REMINDER -> reminderTime
        }
        ReminderTimePickerDialog(
            initialTime = initialTime,
            onDismiss = { timePickerTarget = null },
            onConfirm = { picked ->
                when (target) {
                    ActiveTimePickerTarget.START -> {
                        val pickedStart = TimeOfDay.fromMinutes(picked.minutesOfDay)
                        startTime = pickedStart
                        if (endTime <= pickedStart) {
                            endTime = pickedStart.plusMinutes(60)
                        }
                    }
                    ActiveTimePickerTarget.END -> {
                        val pickedEnd = TimeOfDay.fromMinutes(picked.minutesOfDay)
                        endTime = if (pickedEnd > startTime) pickedEnd else startTime.plusMinutes(30)
                    }
                    ActiveTimePickerTarget.REMINDER -> {
                        reminderTime = picked
                    }
                }
                timePickerTarget = null
            },
        )
    }

    if (showMapPicker) {
        MapLocationPickerDialog(
            initialLocation = selectedLocation,
            geocodingService = geocodingService,
            locationProvider = locationProvider,
            onDismiss = { showMapPicker = false },
            onConfirm = { picked ->
                selectedLocation = picked
                showMapPicker = false
            },
        )
    }
}

private fun extractTypeFormState(details: EventTypeDetails): TypeSpecificFormState = when (details) {
    is EventTypeDetails.StandardEvent -> TypeSpecificFormState(
        guestsRaw = details.guests.joinToString(", "),
        meetingLink = details.meetingLink.orEmpty(),
    )
    is EventTypeDetails.Task -> TypeSpecificFormState(
        taskPriority = details.priority,
        taskCompleted = details.isCompleted,
        taskChecklistRaw = details.checklist.joinToString("\n"),
    )
    is EventTypeDetails.Birthday -> TypeSpecificFormState(
        celebrantName = details.celebrantName.orEmpty(),
        birthYearRaw = details.birthYear?.toString().orEmpty(),
        giftIdea = details.giftIdea.orEmpty(),
    )
    is EventTypeDetails.Appointment -> TypeSpecificFormState(
        specialistName = details.specialistName.orEmpty(),
        contactPhone = details.contactPhone.orEmpty(),
        preparationNotes = details.preparationNotes.orEmpty(),
    )
    is EventTypeDetails.Trip -> TypeSpecificFormState(
        transportMode = details.transportMode,
        bookingReference = details.bookingReference.orEmpty(),
        packingChecklistRaw = details.packingChecklist.joinToString("\n"),
    )
}

private fun buildTypeDetails(
    entryType: CalendarEntryType,
    state: TypeSpecificFormState,
): EventTypeDetails = when (entryType) {
    CalendarEntryType.EVENT -> EventTypeDetails.StandardEvent(
        guests = state.guestsRaw.split(',', ';').map(String::trim).filter(String::isNotEmpty),
        meetingLink = state.meetingLink,
    )
    CalendarEntryType.TASK -> EventTypeDetails.Task(
        priority = state.taskPriority,
        isCompleted = state.taskCompleted,
        checklist = state.taskChecklistRaw.lines().map(String::trim).filter(String::isNotEmpty),
    )
    CalendarEntryType.BIRTHDAY -> EventTypeDetails.Birthday(
        celebrantName = state.celebrantName,
        birthYear = state.birthYearRaw.toIntOrNull(),
        giftIdea = state.giftIdea,
    )
    CalendarEntryType.APPOINTMENT -> EventTypeDetails.Appointment(
        specialistName = state.specialistName,
        contactPhone = state.contactPhone,
        preparationNotes = state.preparationNotes,
    )
    CalendarEntryType.TRIP -> EventTypeDetails.Trip(
        transportMode = state.transportMode,
        bookingReference = state.bookingReference,
        packingChecklist = state.packingChecklistRaw.lines().map(String::trim).filter(String::isNotEmpty),
    )
}
