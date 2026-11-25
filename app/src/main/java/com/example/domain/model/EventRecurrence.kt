package com.example.domain.model

import java.util.Locale

enum class EventRecurrence(val code: String) {
    NONE("NONE"),
    DAILY("DAILY"),
    WEEKLY("WEEKLY"),
    MONTHLY("MONTHLY"),
    YEARLY("YEARLY"),
    ;

    val isRepeating: Boolean
        get() = this != NONE

    companion object {
        fun fromCodeOrDefault(raw: String?): EventRecurrence {
            val normalized = raw?.trim()?.uppercase(Locale.US) ?: return NONE
            return entries.firstOrNull { it.code == normalized } ?: NONE
        }
    }
}
