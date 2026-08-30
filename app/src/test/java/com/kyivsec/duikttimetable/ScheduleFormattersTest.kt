package com.kyivsec.duikttimetable

import com.kyivsec.duikttimetable.util.pairCountText
import com.kyivsec.duikttimetable.util.weekStart
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class ScheduleFormattersTest {
    @Test fun `pair wording follows Ukrainian plural rules`() {
        assertEquals("1 пара", pairCountText(1))
        assertEquals("2 пари", pairCountText(2))
        assertEquals("4 пари", pairCountText(4))
        assertEquals("5 пар", pairCountText(5))
        assertEquals("11 пар", pairCountText(11))
        assertEquals("21 пара", pairCountText(21))
    }

    @Test fun `week start respects preference`() {
        val friday = LocalDate.of(2026, 8, 28)
        assertEquals(LocalDate.of(2026, 8, 24), weekStart(friday, DayOfWeek.MONDAY))
        assertEquals(LocalDate.of(2026, 8, 23), weekStart(friday, DayOfWeek.SUNDAY))
    }
}
