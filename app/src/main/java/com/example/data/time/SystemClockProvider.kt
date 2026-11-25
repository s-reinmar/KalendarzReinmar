package com.example.data.time

import com.example.domain.model.CalendarDay
import com.example.domain.port.ClockProvider
import java.util.Calendar

class SystemClockProvider : ClockProvider {
    override fun today(): CalendarDay {
        val cal = Calendar.getInstance()
        return CalendarDay(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1,
            day = cal.get(Calendar.DAY_OF_MONTH),
        )
    }

    override fun nowMillis(): Long = System.currentTimeMillis()
}
