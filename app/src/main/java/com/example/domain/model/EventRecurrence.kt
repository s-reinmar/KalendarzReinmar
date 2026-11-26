package com.example.domain.model

import java.util.Locale

enum class CustomRecurrenceUnit(val code: String) {
    HOURS("HOURS"),
    DAYS("DAYS"),
    ;

    companion object {
        fun fromCodeOrNull(raw: String?): CustomRecurrenceUnit? {
            val normalized = raw?.trim()?.uppercase(Locale.US) ?: return null
            return entries.firstOrNull { it.code == normalized }
        }
    }
}

sealed interface EventRecurrence {
    val code: String
    val isRepeating: Boolean

    data object None : EventRecurrence {
        override val code: String = "NONE"
        override val isRepeating: Boolean = false
    }

    data object Daily : EventRecurrence {
        override val code: String = "DAILY"
        override val isRepeating: Boolean = true
    }

    data object Weekly : EventRecurrence {
        override val code: String = "WEEKLY"
        override val isRepeating: Boolean = true
    }

    data object Monthly : EventRecurrence {
        override val code: String = "MONTHLY"
        override val isRepeating: Boolean = true
    }

    data object Yearly : EventRecurrence {
        override val code: String = "YEARLY"
        override val isRepeating: Boolean = true
    }

    data class Custom(
        val interval: Int,
        val unit: CustomRecurrenceUnit,
    ) : EventRecurrence {
        init {
            require(interval in MIN_INTERVAL..MAX_INTERVAL) {
                "Custom recurrence interval must be in $MIN_INTERVAL..$MAX_INTERVAL, got $interval"
            }
        }

        override val code: String
            get() = "CUSTOM:${unit.code}:$interval"

        override val isRepeating: Boolean = true
    }

    companion object {
        const val MIN_INTERVAL: Int = 1
        const val MAX_INTERVAL: Int = 999
        const val DEFAULT_CUSTOM_INTERVAL: Int = 2

        val NONE: EventRecurrence = None
        val DAILY: EventRecurrence = Daily
        val WEEKLY: EventRecurrence = Weekly
        val MONTHLY: EventRecurrence = Monthly
        val YEARLY: EventRecurrence = Yearly

        val standardOptions: List<EventRecurrence> = listOf(NONE, DAILY, WEEKLY, MONTHLY, YEARLY)

        fun custom(
            interval: Int,
            unit: CustomRecurrenceUnit,
        ): Custom = Custom(
            interval = interval.coerceIn(MIN_INTERVAL, MAX_INTERVAL),
            unit = unit,
        )

        fun fromCodeOrDefault(raw: String?): EventRecurrence {
            val normalized = raw?.trim()?.uppercase(Locale.US) ?: return NONE
            return when (normalized) {
                "NONE" -> NONE
                "DAILY" -> DAILY
                "WEEKLY" -> WEEKLY
                "MONTHLY" -> MONTHLY
                "YEARLY" -> YEARLY
                else -> parseCustomCode(normalized) ?: NONE
            }
        }

        private fun parseCustomCode(normalized: String): Custom? {
            if (!normalized.startsWith("CUSTOM:")) return null
            val parts = normalized.split(':')
            if (parts.size != 3) return null
            val unit = CustomRecurrenceUnit.fromCodeOrNull(parts[1]) ?: return null
            val interval = parts[2].toIntOrNull() ?: return null
            if (interval !in MIN_INTERVAL..MAX_INTERVAL) return null
            return Custom(interval = interval, unit = unit)
        }
    }
}
