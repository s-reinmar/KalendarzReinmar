package com.example.feature.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.transfer.CsvTransferCodec
import com.example.data.transfer.IcsEventCodec
import com.example.domain.port.DocumentStoragePort
import com.example.domain.port.EventRepository
import com.example.domain.port.NoteRepository
import com.example.domain.port.ReminderPort
import com.example.domain.service.DemoDataFactory

class TransferViewModel(
    eventRepository: EventRepository,
    noteRepository: NoteRepository,
    reminderPort: ReminderPort,
    documentStorage: DocumentStoragePort,
    icsCodec: IcsEventCodec,
    csvCodec: CsvTransferCodec,
    demoDataFactory: DemoDataFactory,
) : ViewModel() {

    val controller = TransferController(
        eventRepository = eventRepository,
        noteRepository = noteRepository,
        reminderPort = reminderPort,
        documentStorage = documentStorage,
        icsCodec = icsCodec,
        csvCodec = csvCodec,
        demoDataFactory = demoDataFactory,
        scope = viewModelScope,
    )

    companion object {
        fun factory(
            eventRepository: EventRepository,
            noteRepository: NoteRepository,
            reminderPort: ReminderPort,
            documentStorage: DocumentStoragePort,
            icsCodec: IcsEventCodec,
            csvCodec: CsvTransferCodec,
            demoDataFactory: DemoDataFactory,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TransferViewModel(
                    eventRepository = eventRepository,
                    noteRepository = noteRepository,
                    reminderPort = reminderPort,
                    documentStorage = documentStorage,
                    icsCodec = icsCodec,
                    csvCodec = csvCodec,
                    demoDataFactory = demoDataFactory,
                ) as T
        }
    }
}
