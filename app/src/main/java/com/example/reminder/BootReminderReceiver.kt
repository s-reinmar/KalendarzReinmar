package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AppDatabase
import com.example.data.local.CalendarEventMapper
import com.example.data.time.SystemClockProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        val reminderScheduler = AndroidReminderScheduler(appContext, SystemClockProvider())

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val events = AppDatabase.getInstance(appContext)
                    .calendarEventDao()
                    .getEventsWithReminders()
                    .map(CalendarEventMapper::toDomain)
                events.forEach(reminderScheduler::schedule)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
