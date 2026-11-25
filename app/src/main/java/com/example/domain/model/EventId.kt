package com.example.domain.model

@JvmInline
value class EventId(val value: Long) {
    val isValid: Boolean
        get() = value > 0L

    companion object {
        val UNASSIGNED = EventId(0L)
    }
}
