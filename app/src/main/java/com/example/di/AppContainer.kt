package com.example.di

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.geocoding.NominatimGeocodingService
import com.example.data.local.RoomEventRepository
import com.example.data.local.RoomNoteRepository
import com.example.data.location.AndroidLocationProvider
import com.example.data.preferences.DataStoreAppPreferencesRepository
import com.example.data.preferences.DataStoreColorPaletteRepository
import com.example.data.time.SystemClockProvider
import com.example.data.transfer.AndroidDocumentStorage
import com.example.data.transfer.CsvTransferCodec
import com.example.data.transfer.FullBackupCodec
import com.example.data.transfer.IcsEventCodec
import com.example.domain.port.AppPreferencesRepository
import com.example.domain.port.ClockProvider
import com.example.domain.port.ColorPaletteRepository
import com.example.domain.port.DeviceLocationProvider
import com.example.domain.port.DocumentStoragePort
import com.example.domain.port.EventRepository
import com.example.domain.port.GeocodingService
import com.example.domain.port.NoteRepository
import com.example.domain.port.ReminderPort
import com.example.domain.service.BackupRestoreService
import com.example.feature.calendar.CalendarViewModel
import com.example.feature.map.MapViewModel
import com.example.feature.notes.NotesViewModel
import com.example.feature.settings.SettingsViewModel
import com.example.feature.shell.ShellViewModel
import com.example.feature.transfer.TransferViewModel
import com.example.reminder.AndroidReminderScheduler

class AppContainer(appContext: Context) {
    private val context = appContext.applicationContext
    private val database: AppDatabase = AppDatabase.getInstance(context)

    val clockProvider: ClockProvider = SystemClockProvider()
    val eventRepository: EventRepository = RoomEventRepository(database.calendarEventDao(), clockProvider)
    val noteRepository: NoteRepository = RoomNoteRepository(database.noteDao(), clockProvider)
    val reminderPort: ReminderPort = AndroidReminderScheduler(context, clockProvider)
    val geocodingService: GeocodingService = NominatimGeocodingService()
    val deviceLocationProvider: DeviceLocationProvider = AndroidLocationProvider(context)
    val documentStoragePort: DocumentStoragePort = AndroidDocumentStorage(context.contentResolver)
    val icsEventCodec: IcsEventCodec = IcsEventCodec(clockProvider)
    val csvTransferCodec: CsvTransferCodec = CsvTransferCodec()
    val fullBackupCodec: FullBackupCodec = FullBackupCodec()
    val colorPaletteRepository: ColorPaletteRepository = DataStoreColorPaletteRepository(context)
    val appPreferencesRepository: AppPreferencesRepository = DataStoreAppPreferencesRepository(context)
    val backupRestoreService: BackupRestoreService = BackupRestoreService(
        eventRepository = eventRepository,
        noteRepository = noteRepository,
        colorPaletteRepository = colorPaletteRepository,
        appPreferencesRepository = appPreferencesRepository,
        reminderPort = reminderPort,
        clock = clockProvider,
    )

    fun shellViewModelFactory(): ViewModelProvider.Factory = ShellViewModel.factory()

    fun calendarViewModelFactory(): ViewModelProvider.Factory = CalendarViewModel.factory(
        eventRepository = eventRepository,
        reminderPort = reminderPort,
        clock = clockProvider,
        appPreferencesRepository = appPreferencesRepository,
    )

    fun mapViewModelFactory(): ViewModelProvider.Factory = MapViewModel.factory(
        eventRepository = eventRepository,
        geocodingService = geocodingService,
        locationProvider = deviceLocationProvider,
    )

    fun notesViewModelFactory(): ViewModelProvider.Factory = NotesViewModel.factory(
        noteRepository = noteRepository,
        eventRepository = eventRepository,
    )

    fun transferViewModelFactory(): ViewModelProvider.Factory = TransferViewModel.factory(
        eventRepository = eventRepository,
        noteRepository = noteRepository,
        documentStorage = documentStoragePort,
        icsCodec = icsEventCodec,
        csvCodec = csvTransferCodec,
        backupRestoreService = backupRestoreService,
        fullBackupCodec = fullBackupCodec,
        reminderPort = reminderPort,
    )

    fun settingsViewModelFactory(): ViewModelProvider.Factory = SettingsViewModel.factory(
        colorPaletteRepository = colorPaletteRepository,
        clock = clockProvider,
        appPreferencesRepository = appPreferencesRepository,
        eventRepository = eventRepository,
        noteRepository = noteRepository,
    )
}
