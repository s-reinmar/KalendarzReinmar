package com.example.feature.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.transfer.CsvTransferCodec
import com.example.data.transfer.FullBackupCodec
import com.example.data.transfer.IcsEventCodec
import com.example.domain.port.DocumentStoragePort
import com.example.domain.port.EventRepository
import com.example.domain.port.NoteRepository
import com.example.domain.port.ReminderPort
import com.example.domain.service.BackupRestoreService

class TransferViewModel(
    eventRepository: EventRepository,
    noteRepository: NoteRepository,
    documentStorage: DocumentStoragePort,
    icsCodec: IcsEventCodec,
    csvCodec: CsvTransferCodec,
    backupRestoreService: BackupRestoreService,
    fullBackupCodec: FullBackupCodec,
    reminderPort: ReminderPort,
) : ViewModel() {

    val controller = TransferController(
        eventRepository = eventRepository,
        noteRepository = noteRepository,
        documentStorage = documentStorage,
        icsCodec = icsCodec,
        csvCodec = csvCodec,
        scope = viewModelScope,
        backupRestoreService = backupRestoreService,
        fullBackupCodec = fullBackupCodec,
        reminderPort = reminderPort,
    )

    companion object {
        fun factory(
            eventRepository: EventRepository,
            noteRepository: NoteRepository,
            documentStorage: DocumentStoragePort,
            icsCodec: IcsEventCodec,
            csvCodec: CsvTransferCodec,
            backupRestoreService: BackupRestoreService,
            fullBackupCodec: FullBackupCodec,
            reminderPort: ReminderPort,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TransferViewModel(
                    eventRepository = eventRepository,
                    noteRepository = noteRepository,
                    documentStorage = documentStorage,
                    icsCodec = icsCodec,
                    csvCodec = csvCodec,
                    backupRestoreService = backupRestoreService,
                    fullBackupCodec = fullBackupCodec,
                    reminderPort = reminderPort,
                ) as T
        }
    }
}
