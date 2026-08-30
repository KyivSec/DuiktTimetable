package com.kyivsec.duikttimetable

import com.kyivsec.duikttimetable.util.weekStart
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class ScheduleFormattersTest {
    @Test fun `week start respects preference`() {
        val friday = LocalDate.of(2026, 8, 28)
        assertEquals(LocalDate.of(2026, 8, 24), weekStart(friday, DayOfWeek.MONDAY))
        assertEquals(LocalDate.of(2026, 8, 23), weekStart(friday, DayOfWeek.SUNDAY))
    }
}
