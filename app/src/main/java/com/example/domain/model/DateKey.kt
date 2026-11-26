package com.example.domain.model

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@JvmInline
value class DateKey private constructor(val value: String) : Comparable<DateKey> {

    override fun compareTo(other: DateKey): Int = value.compareTo(other.value)

    fun toCalendarDay(): CalendarDay? {
        val parts = value.split("-")
        if (parts.size != 3) return null
        val year = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val day = parts[2].toIntOrNull() ?: return null
        return CalendarDay(year = year, month = month, day = day)
    }

    fun toCompactString(): String = value.replace("-", "")

    fun nextDay(): DateKey {
        val parsed = toCalendarDay() ?: return this
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(Calendar.YEAR, parsed.year)
            set(Calendar.MONTH, parsed.month - 1)
            set(Calendar.DAY_OF_MONTH, parsed.day)
            add(Calendar.DAY_OF_MONTH, 1)
        }
        return fromParts(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1,
            day = cal.get(Calendar.DAY_OF_MONTH),
        ) ?: this
    }

    fun plusDays(offsetDays: Int): DateKey {
        val parsed = toCalendarDay() ?: return this
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(Calendar.YEAR, parsed.year)
            set(Calendar.MONTH, parsed.month - 1)
            set(Calendar.DAY_OF_MONTH, parsed.day)
            add(Calendar.DAY_OF_MONTH, offsetDays)
        }
        return fromParts(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1,
            day = cal.get(Calendar.DAY_OF_MONTH),
        ) ?: this
    }

    fun daysUntil(other: DateKey): Long? {
        val start = toEpochDay() ?: return null
        val end = other.toEpochDay() ?: return null
        return end - start
    }

    private fun toEpochDay(): Long? {
        val parsed = toCalendarDay() ?: return null
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(Calendar.YEAR, parsed.year)
            set(Calendar.MONTH, parsed.month - 1)
            set(Calendar.DAY_OF_MONTH, parsed.day)
        }
        return cal.timeInMillis / MILLIS_PER_DAY
    }

    companion object {
        private const val MILLIS_PER_DAY = 86_400_000L
        private val COMPACT_REGEX = Regex("^(\\d{4})(\\d{2})(\\d{2})$")
        private val ISO_REGEX = Regex("^(\\d{4})[-/.](\\d{1,2})[-/.](\\d{1,2})$")
        private val DMY_REGEX = Regex("^(\\d{1,2})[-/.](\\d{1,2})[-/.](\\d{4})$")

        fun parseOrNull(raw: String): DateKey? {
            val value = raw.trim().substringBefore('T').substringBefore('t').substringBefore(' ')
            if (value.isEmpty()) return null

            COMPACT_REGEX.matchEntire(value)?.let { m ->
                return fromParts(
                    year = m.groupValues[1].toInt(),
                    month = m.groupValues[2].toInt(),
                    day = m.groupValues[3].toInt(),
                )
            }

            ISO_REGEX.matchEntire(value)?.let { m ->
                return fromParts(
                    year = m.groupValues[1].toInt(),
                    month = m.groupValues[2].toInt(),
                    day = m.groupValues[3].toInt(),
                )
            }

            DMY_REGEX.matchEntire(value)?.let { m ->
                return fromParts(
                    year = m.groupValues[3].toInt(),
                    month = m.groupValues[2].toInt(),
                    day = m.groupValues[1].toInt(),
                )
            }

            return null
        }

        fun fromParts(year: Int, month: Int, day: Int): DateKey? {
            if (year !in 1900..2999 || month !in 1..12 || day !in 1..31) return null
            return try {
                val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    isLenient = false
                    clear()
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month - 1)
                    set(Calendar.DAY_OF_MONTH, day)
                    timeInMillis
                }
                if (cal.get(Calendar.YEAR) == year &&
                    cal.get(Calendar.MONTH) == month - 1 &&
                    cal.get(Calendar.DAY_OF_MONTH) == day
                ) {
                    DateKey(String.format(Locale.US, "%04d-%02d-%02d", year, month, day))
                } else {
                    null
                }
            } catch (_: IllegalArgumentException) {
                null
            }
        }

        internal fun unsafe(raw: String): DateKey = DateKey(raw)
    }
}
