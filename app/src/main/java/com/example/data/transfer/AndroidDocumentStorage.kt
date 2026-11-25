package com.example.data.transfer

import android.content.ContentResolver
import android.net.Uri
import com.example.domain.port.DocumentStoragePort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidDocumentStorage(
    private val contentResolver: ContentResolver,
) : DocumentStoragePort {

    override suspend fun readText(documentUri: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val uri = Uri.parse(documentUri)
            contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                it.readText()
            } ?: error("Cannot open input stream for $documentUri")
        }
    }

    override suspend fun writeText(documentUri: String, content: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val uri = Uri.parse(documentUri)
                contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use {
                    it.write(content)
                } ?: error("Cannot open output stream for $documentUri")
            }
        }
}
