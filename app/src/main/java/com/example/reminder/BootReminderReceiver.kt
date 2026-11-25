package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val events = AppDatabase.getInstance(appContext)
                    .calendarEventDao()
                    .getEventsWithReminders()
                for (event in events) {
                    val minutes = event.reminderTimeMinutes ?: continue
                    ReminderScheduler.scheduleReminder(
                        context = appContext,
                        eventId = event.id,
                        title = event.title,
                        dateKey = event.dateKey,
                        reminderTimeMinutes = minutes,
                        locationLat = event.locationLat,
                        locationLng = event.locationLng,
                        locationLabel = event.locationLabel,
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
