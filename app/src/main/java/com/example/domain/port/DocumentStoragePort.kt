package com.example.domain.port

interface DocumentStoragePort {
    suspend fun readText(documentUri: String): Result<String>
    suspend fun writeText(documentUri: String, content: String): Result<Unit>
}
