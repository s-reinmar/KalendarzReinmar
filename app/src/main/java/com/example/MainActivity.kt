package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.ReinmarRepository
import com.example.ui.ReinmarApp
import com.example.ui.ReinmarViewModel
import com.example.ui.theme.KalendarzReinmarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
                ReinmarApp(viewModel = reinmarViewModel)
            }
        }
    }
}
