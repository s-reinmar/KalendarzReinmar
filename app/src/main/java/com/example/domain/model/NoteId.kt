package com.example.domain.model

@JvmInline
value class NoteId(val value: Long) {
    val isValid: Boolean
        get() = value > 0L

    companion object {
        val UNASSIGNED = NoteId(0L)
    }
}
