package com.example.feature.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.CalendarEvent
import com.example.domain.model.DateKey
import com.example.domain.service.PolishDateFormatter
import com.example.ui.components.EmptySectionCard
import com.example.ui.theme.AppIcons

@Composable
fun CalendarScreen(
    uiState: CalendarUiState,
    onSelectDate: (DateKey?) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    onLocationPinClick: (CalendarEvent) -> Unit,
    onReminderClick: (CalendarEvent) -> Unit,
    onEditClick: (CalendarEvent) -> Unit,
    onDeleteClick: (CalendarEvent) -> Unit,
    onLoadDemoData: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("calendar_events_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "month_card") {
            MonthCalendarCard(
                year = uiState.displayedYear,
                month = uiState.displayedMonth,
                cells = uiState.monthCells,
                eventDates = uiState.eventDates,
                selectedDateKey = uiState.selectedDateKey,
                todayDateKey = uiState.todayDateKey,
                onSelectDate = onSelectDate,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
            )
        }

        item(key = "events_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (uiState.selectedDateKey != null) {
                        stringResource(
                            R.string.calendar_events_for_day,
                            PolishDateFormatter.formatDateKeyDisplay(uiState.selectedDateKey),
                        )
                    } else {
                        stringResource(R.string.calendar_all_events)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                if (uiState.selectedDateKey != null) {
                    TextButton(
                        onClick = { onSelectDate(null) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("calendar_show_all_button"),
                    ) {
                        Text(text = stringResource(R.string.calendar_show_all))
                    }
                }
            }
        }

        if (uiState.visibleEvents.isEmpty()) {
            item(key = "empty_events") {
                EmptySectionCard(
                    iconRes = AppIcons.event,
                    iconDescription = stringResource(R.string.cd_event_icon),
                    message = if (uiState.selectedDateKey != null) {
                        stringResource(R.string.calendar_empty_events_for_day)
                    } else {
                        stringResource(R.string.calendar_empty_events)
                    },
                    testTag = "calendar_empty_state",
                    actionLabel = if (uiState.events.isEmpty()) {
                        stringResource(R.string.demo_load_action)
                    } else {
                        null
                    },
                    actionTestTag = "calendar_empty_load_demo_button",
                    onActionClick = if (uiState.events.isEmpty()) onLoadDemoData else null,
                )
            }
        } else {
            items(
                items = uiState.visibleEvents,
                key = { it.id.value },
            ) { event ->
                EventItemCard(
                    event = event,
                    onEventClick = { onEventClick(event) },
                    onLocationPinClick = { onLocationPinClick(event) },
                    onReminderClick = { onReminderClick(event) },
                    onEditClick = { onEditClick(event) },
                    onDeleteClick = { onDeleteClick(event) },
                )
            }
        }
    }
}
