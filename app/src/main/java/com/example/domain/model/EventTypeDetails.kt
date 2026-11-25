package com.example.domain.model

sealed interface EventTypeDetails {
    val type: CalendarEntryType

    data class StandardEvent(
        val guests: List<String> = emptyList(),
        val meetingLink: String? = null,
    ) : EventTypeDetails {
        override val type: CalendarEntryType = CalendarEntryType.EVENT

        fun sanitized(): StandardEvent = StandardEvent(
            guests = guests.mapNotNull { it.trim().takeIf(String::isNotEmpty) }.distinct(),
            meetingLink = meetingLink?.trim()?.takeIf(String::isNotEmpty),
        )
    }

    data class Task(
        val priority: TaskPriority = TaskPriority.MEDIUM,
        val isCompleted: Boolean = false,
        val checklist: List<String> = emptyList(),
    ) : EventTypeDetails {
        override val type: CalendarEntryType = CalendarEntryType.TASK

        fun sanitized(): Task = Task(
            priority = priority,
            isCompleted = isCompleted,
            checklist = checklist.mapNotNull { it.trim().takeIf(String::isNotEmpty) },
        )
    }

    data class Birthday(
        val celebrantName: String? = null,
        val birthYear: Int? = null,
        val giftIdea: String? = null,
    ) : EventTypeDetails {
        override val type: CalendarEntryType = CalendarEntryType.BIRTHDAY

        fun turningAgeIn(targetYear: Int): Int? {
            val validBirthYear = birthYear?.takeIf { it in MIN_BIRTH_YEAR..MAX_BIRTH_YEAR } ?: return null
            val age = targetYear - validBirthYear
            return age.takeIf { it in 1..150 }
        }

        fun sanitized(): Birthday = Birthday(
            celebrantName = celebrantName?.trim()?.takeIf(String::isNotEmpty),
            birthYear = birthYear?.takeIf { it in MIN_BIRTH_YEAR..MAX_BIRTH_YEAR },
            giftIdea = giftIdea?.trim()?.takeIf(String::isNotEmpty),
        )
    }

    data class Appointment(
        val specialistName: String? = null,
        val contactPhone: String? = null,
        val preparationNotes: String? = null,
    ) : EventTypeDetails {
        override val type: CalendarEntryType = CalendarEntryType.APPOINTMENT

        fun sanitized(): Appointment = Appointment(
            specialistName = specialistName?.trim()?.takeIf(String::isNotEmpty),
            contactPhone = contactPhone?.trim()?.takeIf(String::isNotEmpty),
            preparationNotes = preparationNotes?.trim()?.takeIf(String::isNotEmpty),
        )
    }

    data class Trip(
        val transportMode: TransportMode = TransportMode.CAR,
        val bookingReference: String? = null,
        val packingChecklist: List<String> = emptyList(),
    ) : EventTypeDetails {
        override val type: CalendarEntryType = CalendarEntryType.TRIP

        fun sanitized(): Trip = Trip(
            transportMode = transportMode,
            bookingReference = bookingReference?.trim()?.takeIf(String::isNotEmpty),
            packingChecklist = packingChecklist.mapNotNull { it.trim().takeIf(String::isNotEmpty) },
        )
    }

    companion object {
        const val MIN_BIRTH_YEAR = 1900
        const val MAX_BIRTH_YEAR = 2100

        fun defaultFor(type: CalendarEntryType): EventTypeDetails = when (type) {
            CalendarEntryType.EVENT -> StandardEvent()
            CalendarEntryType.TASK -> Task()
            CalendarEntryType.BIRTHDAY -> Birthday()
            CalendarEntryType.APPOINTMENT -> Appointment()
            CalendarEntryType.TRIP -> Trip()
        }

        fun sanitize(details: EventTypeDetails): EventTypeDetails = when (details) {
            is StandardEvent -> details.sanitized()
            is Task -> details.sanitized()
            is Birthday -> details.sanitized()
            is Appointment -> details.sanitized()
            is Trip -> details.sanitized()
        }
    }
}
