package com.example.domain.model

data class Note(
    val id: NoteId,
    val title: String,
    val content: String,
    val createdAtMillis: Long = 0L,
)
