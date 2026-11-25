package com.example.domain.model

import java.util.Locale

enum class TaskPriority(val code: String) {
    LOW("LOW"),
    MEDIUM("MEDIUM"),
    HIGH("HIGH"),
    ;

    companion object {
        fun fromCodeOrDefault(raw: String?): TaskPriority {
            val normalized = raw?.trim()?.uppercase(Locale.US) ?: return MEDIUM
            return entries.firstOrNull { it.code == normalized } ?: MEDIUM
        }
    }
}
