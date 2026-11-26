package com.example.feature.transfer

import com.example.domain.model.BackupPreview
import com.example.domain.model.FullBackupSnapshot

data class PendingBackupRestore(
    val documentUri: String,
    val preview: BackupPreview,
    val snapshot: FullBackupSnapshot,
)

data class TransferUiState(
    val isBusy: Boolean = false,
    val pendingBackupRestore: PendingBackupRestore? = null,
)
