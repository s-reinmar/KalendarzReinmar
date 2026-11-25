package com.example.domain.model

import java.util.Locale

enum class EventColorTag(val code: String) {
    PINE("PINE"),
    COBALT("COBALT"),
    AMBER("AMBER"),
    TERRACOTTA("TERRACOTTA"),
    PLUM("PLUM"),
    ;

    companion object {
        fun fromCodeOrDefault(raw: String?, fallback: EventColorTag = PINE): EventColorTag {
            val normalized = raw?.trim()?.uppercase(Locale.US) ?: return fallback
            return entries.firstOrNull { it.code == normalized } ?: fallback
        }
    }
}
