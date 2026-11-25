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
import com.example.data.time.SystemClockProvider
import com.example.domain.model.DateKey
import com.example.domain.service.PolishDateFormatter

class EventReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getLongExtra(ReminderExtras.EXTRA_EVENT_ID, -1L)
        val title = intent.getStringExtra(ReminderExtras.EXTRA_EVENT_TITLE).orEmpty()
        val rawDateKey = intent.getStringExtra(ReminderExtras.EXTRA_EVENT_DATE_KEY).orEmpty()
        val locationSummary = intent.getStringExtra(ReminderExtras.EXTRA_EVENT_LOCATION_SUMMARY)
        if (eventId <= 0L || title.isBlank()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        AndroidReminderScheduler(context.applicationContext, SystemClockProvider()).ensureChannel()

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

        if (!locationSummary.isNullOrBlank()) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
        }

        NotificationManagerCompat.from(context).notify(eventId.toInt(), builder.build())
    }
}
