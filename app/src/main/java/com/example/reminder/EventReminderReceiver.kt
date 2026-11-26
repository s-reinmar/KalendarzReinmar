package com.example.reminder

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.local.CalendarEventMapper
import com.example.data.preferences.DataStoreAppPreferencesRepository
import com.example.data.time.SystemClockProvider
import com.example.domain.model.AppPreferences
import com.example.domain.model.DateKey
import com.example.domain.service.PolishDateFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class EventReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getLongExtra(ReminderExtras.EXTRA_EVENT_ID, -1L)
        val title = intent.getStringExtra(ReminderExtras.EXTRA_EVENT_TITLE).orEmpty()
        val rawDateKey = intent.getStringExtra(ReminderExtras.EXTRA_EVENT_DATE_KEY).orEmpty()
        val locationSummary = intent.getStringExtra(ReminderExtras.EXTRA_EVENT_LOCATION_SUMMARY)
        val isSnooze = intent.getBooleanExtra(ReminderExtras.EXTRA_IS_SNOOZE, false)
        if (eventId <= 0L || title.isBlank()) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snoozeMinutes = runCatching {
                    DataStoreAppPreferencesRepository(appContext)
                        .observePreferences()
                        .first()
                        .defaultSnoozeMinutes
                }.getOrElse { AppPreferences.DEFAULT_SNOOZE_MINUTES }

                postNotification(
                    context = appContext,
                    eventId = eventId,
                    title = title,
                    rawDateKey = rawDateKey,
                    locationSummary = locationSummary,
                    snoozeMinutes = snoozeMinutes,
                )

                if (!isSnooze) {
                    rescheduleIfRecurring(appContext, eventId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun postNotification(
        context: Context,
        eventId: Long,
        title: String,
        rawDateKey: String,
        locationSummary: String?,
        snoozeMinutes: Int,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        AndroidReminderScheduler(context, SystemClockProvider()).ensureChannel()

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(ReminderExtras.EXTRA_EVENT_ID, eventId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            eventId.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val snoozeIntent = Intent(context, SnoozeReminderReceiver::class.java).apply {
            putExtra(ReminderExtras.EXTRA_EVENT_ID, eventId)
            putExtra(ReminderExtras.EXTRA_EVENT_TITLE, title)
            putExtra(ReminderExtras.EXTRA_EVENT_DATE_KEY, rawDateKey)
            putExtra(ReminderExtras.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
            if (locationSummary != null) {
                putExtra(ReminderExtras.EXTRA_EVENT_LOCATION_SUMMARY, locationSummary)
            }
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            ReminderExtras.SNOOZE_REQUEST_CODE_OFFSET + eventId.toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val dateText = DateKey.parseOrNull(rawDateKey)
            ?.let(PolishDateFormatter::formatDateKeyDisplay)
            ?: context.getString(R.string.app_name)

        val contentText = if (!locationSummary.isNullOrBlank()) {
            "$dateText • ${context.getString(R.string.location_badge, locationSummary)}"
        } else {
            dateText
        }

        val builder = NotificationCompat.Builder(context, ReminderExtras.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_app_event)
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                R.drawable.ic_app_event,
                context.getString(R.string.reminder_snooze_action, snoozeMinutes),
                snoozePendingIntent,
            )

        if (!locationSummary.isNullOrBlank()) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
        }

        NotificationManagerCompat.from(context).notify(eventId.toInt(), builder.build())
    }

    private suspend fun rescheduleIfRecurring(
        appContext: Context,
        eventId: Long,
    ) {
        val entity = AppDatabase.getInstance(appContext)
            .calendarEventDao()
            .getEventById(eventId) ?: return
        val domainEvent = CalendarEventMapper.toDomain(entity)
        if (domainEvent.recurrence.isRepeating && domainEvent.hasReminder) {
            AndroidReminderScheduler(appContext, SystemClockProvider()).schedule(domainEvent)
        }
    }
}
