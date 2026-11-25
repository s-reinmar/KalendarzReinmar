package com.example.feature.calendar

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.domain.model.CalendarDay
import com.example.domain.model.EventId
import com.example.domain.port.DeviceLocationProvider
import com.example.domain.port.GeocodingService
import com.example.feature.map.MapLocationPickerDialog
import com.example.ui.components.ConfirmDeleteDialog

@Composable
fun CalendarDialogHost(
    uiState: CalendarUiState,
    todayDay: CalendarDay,
    controller: CalendarController,
    geocodingService: GeocodingService,
    locationProvider: DeviceLocationProvider,
    onEnsureNotificationPermission: () -> Unit,
    onShowEventOnMainMap: (EventId) -> Unit,
) {
    when (val dialog = uiState.dialog) {
        CalendarDialogState.None -> Unit

        is CalendarDialogState.CreateEvent -> {
            EventFormDialog(
                initialDateKey = dialog.initialDateKey,
                todayDay = todayDay,
                geocodingService = geocodingService,
                locationProvider = locationProvider,
                initialLocation = dialog.initialLocation,
                onDismiss = controller::dismissDialog,
                onSave = { title, dateKey, reminderTime, location ->
                    if (reminderTime != null) {
                        onEnsureNotificationPermission()
                    }
                    controller.createEvent(
                        title = title,
                        dateKey = dateKey,
                        reminderTime = reminderTime,
                        location = location,
                    )
                },
            )
        }

        is CalendarDialogState.EditEvent -> {
            EventFormDialog(
                initialDateKey = dialog.event.dateKey,
                todayDay = todayDay,
                geocodingService = geocodingService,
                locationProvider = locationProvider,
                existingEvent = dialog.event,
                onDismiss = controller::dismissDialog,
                onSave = { title, dateKey, reminderTime, location ->
                    if (reminderTime != null) {
                        onEnsureNotificationPermission()
                    }
                    controller.updateEvent(
                        event = dialog.event,
                        title = title,
                        dateKey = dateKey,
                        reminderTime = reminderTime,
                        location = location,
                    )
                },
            )
        }

        is CalendarDialogState.EventDetails -> {
            val event = uiState.detailEvent
            if (event != null) {
                EventDetailDialog(
                    event = event,
                    onDismiss = controller::dismissDialog,
                    onEditClick = { controller.openEditEvent(event) },
                    onReminderClick = { controller.openReminderEditor(event) },
                    onShowOnMainMap = {
                        controller.dismissDialog()
                        onShowEventOnMainMap(event.id)
                    },
                    onDeleteClick = { controller.requestDeleteEvent(event) },
                )
            }
        }

        is CalendarDialogState.ReminderEditor -> {
            EventReminderDialog(
                event = dialog.event,
                geocodingService = geocodingService,
                locationProvider = locationProvider,
                onDismiss = controller::dismissDialog,
                onSave = { reminderTime, location ->
                    if (reminderTime != null) {
                        onEnsureNotificationPermission()
                    }
                    controller.updateEventReminderAndLocation(
                        event = dialog.event,
                        reminderTime = reminderTime,
                        location = location,
                    )
                },
            )
        }

        is CalendarDialogState.QuickLocationPicker -> {
            MapLocationPickerDialog(
                initialLocation = dialog.event.location,
                geocodingService = geocodingService,
                locationProvider = locationProvider,
                onDismiss = controller::dismissDialog,
                onConfirm = { picked ->
                    controller.updateEventReminderAndLocation(
                        event = dialog.event,
                        reminderTime = dialog.event.reminderTime,
                        location = picked,
                    )
                },
            )
        }

        is CalendarDialogState.ConfirmDelete -> {
            ConfirmDeleteDialog(
                title = stringResource(R.string.calendar_delete_event_title),
                message = stringResource(R.string.calendar_delete_event_message, dialog.event.title),
                confirmTestTag = "confirm_delete_event_button",
                cancelTestTag = "cancel_delete_event_button",
                onConfirm = { controller.confirmDeleteEvent(dialog.event) },
                onDismiss = controller::dismissDialog,
            )
        }
    }
}
