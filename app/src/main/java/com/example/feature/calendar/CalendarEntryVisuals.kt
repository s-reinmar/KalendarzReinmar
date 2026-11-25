package com.example.feature.calendar

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.R
import com.example.domain.model.CalendarEntryType
import com.example.domain.model.EventColorTag
import com.example.domain.model.EventRecurrence
import com.example.domain.model.TaskPriority
import com.example.domain.model.TransportMode
import com.example.ui.theme.AppIcons
import com.example.ui.theme.DarkInk
import com.example.ui.theme.DarkPineContainer
import com.example.ui.theme.DarkPinePrimary
import com.example.ui.theme.DarkTagAmber
import com.example.ui.theme.DarkTagAmberContainer
import com.example.ui.theme.DarkTagCobalt
import com.example.ui.theme.DarkTagCobaltContainer
import com.example.ui.theme.DarkTagPlum
import com.example.ui.theme.DarkTagPlumContainer
import com.example.ui.theme.DarkTerracotta
import com.example.ui.theme.DarkTerracottaContainer
import com.example.ui.theme.OnPineGreenContainer
import com.example.ui.theme.OnTagAmberContainer
import com.example.ui.theme.OnTagCobaltContainer
import com.example.ui.theme.OnTagPlumContainer
import com.example.ui.theme.OnTerracottaContainer
import com.example.ui.theme.PineGreen
import com.example.ui.theme.PineGreenContainer
import com.example.ui.theme.TagAmber
import com.example.ui.theme.TagAmberContainer
import com.example.ui.theme.TagCobalt
import com.example.ui.theme.TagCobaltContainer
import com.example.ui.theme.TagPlum
import com.example.ui.theme.TagPlumContainer
import com.example.ui.theme.Terracotta
import com.example.ui.theme.TerracottaContainer

data class EntryColorPalette(
    val accent: Color,
    val container: Color,
    val onContainer: Color,
)

object CalendarEntryVisuals {

    @DrawableRes
    fun iconRes(type: CalendarEntryType): Int = when (type) {
        CalendarEntryType.EVENT -> AppIcons.event
        CalendarEntryType.TASK -> AppIcons.task
        CalendarEntryType.BIRTHDAY -> AppIcons.birthday
        CalendarEntryType.APPOINTMENT -> AppIcons.appointment
        CalendarEntryType.TRIP -> AppIcons.trip
    }

    @StringRes
    fun labelRes(type: CalendarEntryType): Int = when (type) {
        CalendarEntryType.EVENT -> R.string.entry_type_event
        CalendarEntryType.TASK -> R.string.entry_type_task
        CalendarEntryType.BIRTHDAY -> R.string.entry_type_birthday
        CalendarEntryType.APPOINTMENT -> R.string.entry_type_appointment
        CalendarEntryType.TRIP -> R.string.entry_type_trip
    }

    @StringRes
    fun subtitleRes(type: CalendarEntryType): Int = when (type) {
        CalendarEntryType.EVENT -> R.string.entry_type_event_subtitle
        CalendarEntryType.TASK -> R.string.entry_type_task_subtitle
        CalendarEntryType.BIRTHDAY -> R.string.entry_type_birthday_subtitle
        CalendarEntryType.APPOINTMENT -> R.string.entry_type_appointment_subtitle
        CalendarEntryType.TRIP -> R.string.entry_type_trip_subtitle
    }

    @StringRes
    fun colorLabelRes(tag: EventColorTag): Int = when (tag) {
        EventColorTag.PINE -> R.string.entry_color_pine
        EventColorTag.COBALT -> R.string.entry_color_cobalt
        EventColorTag.AMBER -> R.string.entry_color_amber
        EventColorTag.TERRACOTTA -> R.string.entry_color_terracotta
        EventColorTag.PLUM -> R.string.entry_color_plum
    }

    @StringRes
    fun recurrenceLabelRes(recurrence: EventRecurrence): Int = when (recurrence) {
        EventRecurrence.NONE -> R.string.recurrence_none
        EventRecurrence.DAILY -> R.string.recurrence_daily
        EventRecurrence.WEEKLY -> R.string.recurrence_weekly
        EventRecurrence.MONTHLY -> R.string.recurrence_monthly
        EventRecurrence.YEARLY -> R.string.recurrence_yearly
    }

    @StringRes
    fun priorityLabelRes(priority: TaskPriority): Int = when (priority) {
        TaskPriority.LOW -> R.string.priority_low
        TaskPriority.MEDIUM -> R.string.priority_medium
        TaskPriority.HIGH -> R.string.priority_high
    }

    @StringRes
    fun transportLabelRes(mode: TransportMode): Int = when (mode) {
        TransportMode.CAR -> R.string.transport_car
        TransportMode.TRAIN -> R.string.transport_train
        TransportMode.PLANE -> R.string.transport_plane
        TransportMode.BUS -> R.string.transport_bus
        TransportMode.OTHER -> R.string.transport_other
    }

    @Composable
    fun palette(tag: EventColorTag, darkTheme: Boolean = isSystemInDarkTheme()): EntryColorPalette =
        if (darkTheme) {
            when (tag) {
                EventColorTag.PINE -> EntryColorPalette(DarkPinePrimary, DarkPineContainer, DarkInk)
                EventColorTag.COBALT -> EntryColorPalette(DarkTagCobalt, DarkTagCobaltContainer, DarkInk)
                EventColorTag.AMBER -> EntryColorPalette(DarkTagAmber, DarkTagAmberContainer, DarkInk)
                EventColorTag.TERRACOTTA -> EntryColorPalette(DarkTerracotta, DarkTerracottaContainer, DarkInk)
                EventColorTag.PLUM -> EntryColorPalette(DarkTagPlum, DarkTagPlumContainer, DarkInk)
            }
        } else {
            when (tag) {
                EventColorTag.PINE -> EntryColorPalette(PineGreen, PineGreenContainer, OnPineGreenContainer)
                EventColorTag.COBALT -> EntryColorPalette(TagCobalt, TagCobaltContainer, OnTagCobaltContainer)
                EventColorTag.AMBER -> EntryColorPalette(TagAmber, TagAmberContainer, OnTagAmberContainer)
                EventColorTag.TERRACOTTA -> EntryColorPalette(Terracotta, TerracottaContainer, OnTerracottaContainer)
                EventColorTag.PLUM -> EntryColorPalette(TagPlum, TagPlumContainer, OnTagPlumContainer)
            }
        }
}
