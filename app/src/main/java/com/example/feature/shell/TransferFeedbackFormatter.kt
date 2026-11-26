package com.example.feature.shell

import android.content.Context
import com.example.R
import com.example.domain.model.TransferFeedback

internal object TransferFeedbackFormatter {
    fun format(context: Context, feedback: TransferFeedback): String = when (feedback) {
        is TransferFeedback.ImportedEvents -> {
            context.getString(R.string.transfer_imported_events, feedback.count)
        }
        is TransferFeedback.ImportedNotes -> {
            context.getString(R.string.transfer_imported_notes, feedback.count)
        }
        is TransferFeedback.ImportedBoth -> {
            context.getString(
                R.string.transfer_imported_both,
                feedback.eventsCount,
                feedback.notesCount,
            )
        }
        is TransferFeedback.ExportedEvents -> {
            context.getString(R.string.transfer_exported_events, feedback.count)
        }
        is TransferFeedback.ExportedNotes -> {
            context.getString(R.string.transfer_exported_notes, feedback.count)
        }
        is TransferFeedback.ExportedBackup -> {
            context.getString(
                R.string.transfer_exported_backup,
                feedback.eventsCount,
                feedback.notesCount,
            )
        }
        is TransferFeedback.RestoredBackup -> {
            context.getString(
                R.string.transfer_restored_backup,
                feedback.eventsCount,
                feedback.notesCount,
                feedback.palettesCount,
            )
        }
        TransferFeedback.InvalidBackupFile -> {
            context.getString(R.string.transfer_invalid_backup)
        }
        TransferFeedback.NothingToImport -> {
            context.getString(R.string.transfer_nothing_to_import)
        }
        TransferFeedback.Error -> {
            context.getString(R.string.transfer_error)
        }
    }
}
