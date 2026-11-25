package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.ReinmarRepository
import com.example.reminder.ReminderScheduler
import com.example.ui.ReinmarApp
import com.example.ui.ReinmarViewModel
import com.example.ui.theme.KalendarzReinmarTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val notificationEventId = MutableStateFlow<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ReminderScheduler.ensureNotificationChannel(applicationContext)
        extractNotificationEventId(intent)

        val database = AppDatabase.getInstance(applicationContext)
        val repository = ReinmarRepository(
            eventDao = database.calendarEventDao(),
            noteDao = database.noteDao(),
        )

        setContent {
            KalendarzReinmarTheme {
                val reinmarViewModel: ReinmarViewModel = viewModel(
                    factory = ReinmarViewModel.factory(repository),
                )
                val incomingEventId by notificationEventId.collectAsStateWithLifecycle()
                LaunchedEffect(incomingEventId) {
                    val eventId = incomingEventId
                    if (eventId != null && eventId > 0L) {
                        reinmarViewModel.requestOpenEventDetails(eventId)
                        notificationEventId.value = null
                    }
                }
                ReinmarApp(viewModel = reinmarViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractNotificationEventId(intent)
    }

    private fun extractNotificationEventId(intent: Intent?) {
        val eventId = intent?.getLongExtra(ReminderScheduler.EXTRA_EVENT_ID, -1L) ?: -1L
        if (eventId > 0L) {
            notificationEventId.value = eventId
            intent?.removeExtra(ReminderScheduler.EXTRA_EVENT_ID)
        }
    }
}
