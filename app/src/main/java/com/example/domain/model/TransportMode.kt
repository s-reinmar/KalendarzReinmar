package com.example.domain.model

import java.util.Locale

enum class TransportMode(val code: String) {
    CAR("CAR"),
    TRAIN("TRAIN"),
    PLANE("PLANE"),
    BUS("BUS"),
    OTHER("OTHER"),
    ;

    companion object {
        fun fromCodeOrDefault(raw: String?): TransportMode {
            val normalized = raw?.trim()?.uppercase(Locale.US) ?: return CAR
            return entries.firstOrNull { it.code == normalized } ?: CAR
        }
    }
}
