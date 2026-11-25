package com.example.domain.port

import com.example.domain.model.CalendarDay

interface ClockProvider {
    fun today(): CalendarDay
    fun nowMillis(): Long
}
