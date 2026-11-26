package com.example.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.R
import com.example.domain.model.CalendarEvent
import com.example.domain.model.DateKey
import com.example.domain.model.EventId
import com.example.domain.port.ClockProvider
import com.example.domain.port.ReminderPort
import com.example.domain.service.ReminderTriggerCalculator

class AndroidReminderScheduler(
    private val appContext: Context,
    private val clock: ClockProvider,
) : ReminderPort {

    override fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        val channel = NotificationChannel(
            ReminderExtras.CHANNEL_ID,
            appContext.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = appContext.getString(R.string.reminder_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    override fun schedule(event: CalendarEvent) {
        if (!event.hasReminder) {
            cancel(event.id)
            return
        }
        ensureChannel()
        val triggerAtMillis = ReminderTriggerCalculator.computeNextTriggerMillis(
            event = event,
            nowMillis = clock.nowMillis(),
        )
        if (triggerAtMillis == null || triggerAtMillis <= clock.nowMillis()) {
            cancel(event.id)
            return
        }

        val pendingIntent = buildBroadcastPendingIntent(
            requestCode = mainRequestCode(event.id),
            eventId = event.id,
            title = event.title,
            dateKey = event.dateKey.value,
            locationSummary = event.location?.formatSummary(),
            isSnooze = false,
            flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return

        scheduleAlarm(triggerAtMillis, pendingIntent)
    }

    override fun scheduleSnooze(
        eventId: EventId,
        title: String,
        dateKey: DateKey,
        locationSummary: String?,
        snoozeMinutes: Int,
    ) {
        if (!eventId.isValid || title.isBlank()) return
        ensureChannel()
        val triggerAtMillis = ReminderTriggerCalculator.computeSnoozeTriggerMillis(
            nowMillis = clock.nowMillis(),
            snoozeMinutes = snoozeMinutes,
        )
        val pendingIntent = buildBroadcastPendingIntent(
            requestCode = snoozeRequestCode(eventId),
            eventId = eventId,
            title = title,
            dateKey = dateKey.value,
            locationSummary = locationSummary,
            isSnooze = true,
            flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return

        scheduleAlarm(triggerAtMillis, pendingIntent)
    }

    override fun cancel(eventId: EventId) {
        cancelRequestCode(mainRequestCode(eventId), eventId, isSnooze = false)
        cancelRequestCode(snoozeRequestCode(eventId), eventId, isSnooze = true)
    }

    private fun scheduleAlarm(
        triggerAtMillis: Long,
        pendingIntent: PendingIntent,
    ) {
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
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

    private fun cancelRequestCode(
        requestCode: Int,
        eventId: EventId,
        isSnooze: Boolean,
    ) {
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = buildBroadcastPendingIntent(
            requestCode = requestCode,
            eventId = eventId,
            title = "",
            dateKey = "",
            locationSummary = null,
            isSnooze = isSnooze,
            flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun buildBroadcastPendingIntent(
        requestCode: Int,
        eventId: EventId,
        title: String,
        dateKey: String,
        locationSummary: String?,
        isSnooze: Boolean,
        flags: Int,
    ): PendingIntent? {
        val intent = Intent(appContext, EventReminderReceiver::class.java).apply {
            putExtra(ReminderExtras.EXTRA_EVENT_ID, eventId.value)
            putExtra(ReminderExtras.EXTRA_EVENT_TITLE, title)
            putExtra(ReminderExtras.EXTRA_EVENT_DATE_KEY, dateKey)
            putExtra(ReminderExtras.EXTRA_IS_SNOOZE, isSnooze)
            if (locationSummary != null) {
                putExtra(ReminderExtras.EXTRA_EVENT_LOCATION_SUMMARY, locationSummary)
            }
        }
        return PendingIntent.getBroadcast(
            appContext,
            requestCode,
            intent,
            flags,
        )
    }

    private fun mainRequestCode(eventId: EventId): Int = eventId.value.toInt()

    private fun snoozeRequestCode(eventId: EventId): Int =
        ReminderExtras.SNOOZE_REQUEST_CODE_OFFSET + eventId.value.toInt()
}
