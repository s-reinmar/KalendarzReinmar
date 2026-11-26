package com.example.feature.calendar

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.domain.model.CalendarEntryType
import com.example.domain.model.CustomRecurrenceUnit
import com.example.domain.model.EventColorTag
import com.example.domain.model.EventRecurrence
import com.example.domain.model.PresetColorTag
import com.example.domain.model.TaskPriority
import com.example.domain.model.TransportMode
import com.example.ui.theme.AppIcons
import com.example.ui.theme.DarkInk
import com.example.ui.theme.DarkPineContainer
import com.example.ui.theme.DarkPinePrimary
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkTagAmber
import com.example.ui.theme.DarkTagAmberContainer
import com.example.ui.theme.DarkTagCobalt
import com.example.ui.theme.DarkTagCobaltContainer
import com.example.ui.theme.DarkTagPlum
import com.example.ui.theme.DarkTagPlumContainer
import com.example.ui.theme.DarkTerracotta
import com.example.ui.theme.DarkTerracottaContainer
import com.example.ui.theme.InkDark
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
import com.example.ui.theme.WarmPaper

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
    fun presetColorLabelRes(preset: PresetColorTag): Int = when (preset) {
        PresetColorTag.PINE -> R.string.entry_color_pine
        PresetColorTag.COBALT -> R.string.entry_color_cobalt
        PresetColorTag.AMBER -> R.string.entry_color_amber
        PresetColorTag.TERRACOTTA -> R.string.entry_color_terracotta
        PresetColorTag.PLUM -> R.string.entry_color_plum
    }

    @Composable
    fun colorLabel(tag: EventColorTag): String = when (tag) {
        is EventColorTag.Preset -> stringResource(presetColorLabelRes(tag.preset))
        is EventColorTag.CustomRgb -> tag.hexString
    }

    @Composable
    fun recurrenceLabel(recurrence: EventRecurrence): String = when (recurrence) {
        EventRecurrence.None -> stringResource(R.string.recurrence_none)
        EventRecurrence.Daily -> stringResource(R.string.recurrence_daily)
        EventRecurrence.Weekly -> stringResource(R.string.recurrence_weekly)
        EventRecurrence.Monthly -> stringResource(R.string.recurrence_monthly)
        EventRecurrence.Yearly -> stringResource(R.string.recurrence_yearly)
        is EventRecurrence.Custom -> formatCustomRecurrenceLabel(recurrence)
    }

    @Composable
    private fun formatCustomRecurrenceLabel(recurrence: EventRecurrence.Custom): String {
        val n = recurrence.interval
        return when (recurrence.unit) {
            CustomRecurrenceUnit.HOURS -> when {
                n == 1 -> stringResource(R.string.recurrence_every_one_hour)
                n % 10 in 2..4 && n % 100 !in 12..14 -> stringResource(R.string.recurrence_every_few_hours, n)
                else -> stringResource(R.string.recurrence_every_many_hours, n)
            }
            CustomRecurrenceUnit.DAYS -> when (n) {
                1 -> stringResource(R.string.recurrence_every_one_day)
                else -> stringResource(R.string.recurrence_every_many_days, n)
            }
        }
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

    fun contrastingOnColor(red: Int, green: Int, blue: Int): Color {
        val perceivedLuminance = (0.299 * red) + (0.587 * green) + (0.114 * blue)
        return if (perceivedLuminance > 155.0) InkDark else WarmPaper
    }

    @Composable
    fun palette(
        tag: EventColorTag,
        darkTheme: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.35f,
    ): EntryColorPalette =
        when (tag) {
            is EventColorTag.Preset -> presetPalette(tag.preset, darkTheme)
            is EventColorTag.CustomRgb -> customRgbPalette(tag, darkTheme)
        }

    private fun presetPalette(preset: PresetColorTag, darkTheme: Boolean): EntryColorPalette =
        if (darkTheme) {
            when (preset) {
                PresetColorTag.PINE -> EntryColorPalette(DarkPinePrimary, DarkPineContainer, DarkInk)
                PresetColorTag.COBALT -> EntryColorPalette(DarkTagCobalt, DarkTagCobaltContainer, DarkInk)
                PresetColorTag.AMBER -> EntryColorPalette(DarkTagAmber, DarkTagAmberContainer, DarkInk)
                PresetColorTag.TERRACOTTA -> EntryColorPalette(DarkTerracotta, DarkTerracottaContainer, DarkInk)
                PresetColorTag.PLUM -> EntryColorPalette(DarkTagPlum, DarkTagPlumContainer, DarkInk)
            }
        } else {
            when (preset) {
                PresetColorTag.PINE -> EntryColorPalette(PineGreen, PineGreenContainer, OnPineGreenContainer)
                PresetColorTag.COBALT -> EntryColorPalette(TagCobalt, TagCobaltContainer, OnTagCobaltContainer)
                PresetColorTag.AMBER -> EntryColorPalette(TagAmber, TagAmberContainer, OnTagAmberContainer)
                PresetColorTag.TERRACOTTA -> EntryColorPalette(Terracotta, TerracottaContainer, OnTerracottaContainer)
                PresetColorTag.PLUM -> EntryColorPalette(TagPlum, TagPlumContainer, OnTagPlumContainer)
            }
        }

    private fun customRgbPalette(tag: EventColorTag.CustomRgb, darkTheme: Boolean): EntryColorPalette {
        val base = Color(red = tag.red, green = tag.green, blue = tag.blue)
        return if (darkTheme) {
            val perceivedLuminance = (0.299 * tag.red) + (0.587 * tag.green) + (0.114 * tag.blue)
            val accent = if (perceivedLuminance < 70.0) lerp(base, DarkInk, 0.35f) else base
            EntryColorPalette(
                accent = accent,
                container = lerp(DarkSurface, base, 0.32f),
                onContainer = lerp(base, DarkInk, 0.78f),
            )
        } else {
            EntryColorPalette(
                accent = base,
                container = lerp(WarmPaper, base, 0.18f),
                onContainer = lerp(base, InkDark, 0.62f),
            )
        }
    }
}
