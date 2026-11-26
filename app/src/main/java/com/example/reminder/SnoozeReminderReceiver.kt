package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.data.time.SystemClockProvider
import com.example.domain.model.AppPreferences
import com.example.domain.model.DateKey
import com.example.domain.model.EventId

class SnoozeReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val rawEventId = intent.getLongExtra(ReminderExtras.EXTRA_EVENT_ID, -1L)
        val title = intent.getStringExtra(ReminderExtras.EXTRA_EVENT_TITLE).orEmpty()
        val rawDateKey = intent.getStringExtra(ReminderExtras.EXTRA_EVENT_DATE_KEY).orEmpty()
        val locationSummary = intent.getStringExtra(ReminderExtras.EXTRA_EVENT_LOCATION_SUMMARY)
        val snoozeMinutes = AppPreferences.normalizeSnoozeMinutes(
            intent.getIntExtra(ReminderExtras.EXTRA_SNOOZE_MINUTES, AppPreferences.DEFAULT_SNOOZE_MINUTES),
        )
        val eventId = EventId(rawEventId)
        val dateKey = DateKey.parseOrNull(rawDateKey)
        if (!eventId.isValid || title.isBlank() || dateKey == null) return

        NotificationManagerCompat.from(context).cancel(eventId.value.toInt())

        AndroidReminderScheduler(
            appContext = context.applicationContext,
            clock = SystemClockProvider(),
        ).scheduleSnooze(
            eventId = eventId,
            title = title,
            dateKey = dateKey,
            locationSummary = locationSummary,
            snoozeMinutes = snoozeMinutes,
        )
    }
}
