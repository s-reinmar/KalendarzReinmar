package com.example.domain.service

import com.example.domain.model.DateKey
import com.example.domain.model.ReminderTime
import java.util.Calendar
import java.util.TimeZone

object ReminderTriggerCalculator {
    fun computeTriggerMillis(
        dateKey: DateKey,
        reminderTime: ReminderTime,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): Long? {
        val day = dateKey.toCalendarDay() ?: return null
        val cal = Calendar.getInstance(timeZone).apply {
            set(Calendar.YEAR, day.year)
            set(Calendar.MONTH, day.month - 1)
            set(Calendar.DAY_OF_MONTH, day.day)
            set(Calendar.HOUR_OF_DAY, reminderTime.hour)
            set(Calendar.MINUTE, reminderTime.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
