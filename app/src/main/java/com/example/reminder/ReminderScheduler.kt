package com.example.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.R
import com.example.ui.DateUtils
import java.util.Calendar
import java.util.Locale

object ReminderScheduler {
    const val CHANNEL_ID = "kalendarz_reinmar_reminders"
    const val EXTRA_EVENT_ID = "extra_event_id"
    const val EXTRA_EVENT_TITLE = "extra_event_title"
    const val EXTRA_EVENT_DATE_KEY = "extra_event_date_key"
    const val DEFAULT_REMINDER_MINUTES = 9 * 60

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.reminder_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    fun formatMinutesOfDay(minutes: Int): String {
        val clamped = minutes.coerceIn(0, 23 * 60 + 59)
        val hour = clamped / 60
        val minute = clamped % 60
        return String.format(Locale.US, "%02d:%02d", hour, minute)
    }

    fun computeTriggerMillis(dateKey: String, reminderTimeMinutes: Int): Long? {
        val day = DateUtils.parseDateKey(dateKey) ?: return null
        val clamped = reminderTimeMinutes.coerceIn(0, 23 * 60 + 59)
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, day.year)
            set(Calendar.MONTH, day.month - 1)
            set(Calendar.DAY_OF_MONTH, day.day)
            set(Calendar.HOUR_OF_DAY, clamped / 60)
            set(Calendar.MINUTE, clamped % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun scheduleReminder(
        context: Context,
        eventId: Long,
        title: String,
        dateKey: String,
        reminderTimeMinutes: Int,
    ) {
        ensureNotificationChannel(context)
        val triggerAtMillis = computeTriggerMillis(dateKey, reminderTimeMinutes) ?: return
        if (triggerAtMillis <= System.currentTimeMillis()) {
            cancelReminder(context, eventId)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = buildPendingIntent(
            context = context,
            eventId = eventId,
            title = title,
            dateKey = dateKey,
            flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return

        val canUseExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()

        if (canUseExact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
        }
    }

    fun cancelReminder(context: Context, eventId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = buildPendingIntent(
            context = context,
            eventId = eventId,
            title = "",
            dateKey = "",
            flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun buildPendingIntent(
        context: Context,
        eventId: Long,
        title: String,
        dateKey: String,
        flags: Int,
    ): PendingIntent? {
        val intent = Intent(context, EventReminderReceiver::class.java).apply {
            putExtra(EXTRA_EVENT_ID, eventId)
            putExtra(EXTRA_EVENT_TITLE, title)
            putExtra(EXTRA_EVENT_DATE_KEY, dateKey)
        }
        return PendingIntent.getBroadcast(
            context,
            eventId.toInt(),
            intent,
            flags,
        )
    }
}
