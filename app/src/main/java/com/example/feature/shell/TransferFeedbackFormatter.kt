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
        TransferFeedback.NothingToImport -> {
            context.getString(R.string.transfer_nothing_to_import)
        }
        TransferFeedback.Error -> {
            context.getString(R.string.transfer_error)
        }
        is TransferFeedback.DemoLoaded -> {
            context.getString(
                R.string.demo_loaded_feedback,
                feedback.eventsCount,
                feedback.notesCount,
            )
        }
        TransferFeedback.DemoAlreadyLoaded -> {
            context.getString(R.string.demo_already_loaded_feedback)
        }
        is TransferFeedback.DemoCleared -> {
            context.getString(
                R.string.demo_cleared_feedback,
                feedback.eventsCount,
                feedback.notesCount,
            )
        }
        TransferFeedback.DemoNothingToClear -> {
            context.getString(R.string.demo_nothing_to_clear_feedback)
        }
    }
}
