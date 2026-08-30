package com.kyivsec.duikttimetable.ui.preview

import com.kyivsec.duikttimetable.model.Lesson
import com.kyivsec.duikttimetable.model.LessonType
import com.kyivsec.duikttimetable.model.ScheduleDay
import com.kyivsec.duikttimetable.model.ScheduleWeek
import java.time.LocalDate
import java.time.LocalTime

object PreviewData {
    val date: LocalDate = LocalDate.of(2026, 8, 28)
    val lessons = listOf(
        Lesson("p1", "Вища математика", LessonType.LECTURE, LocalTime.of(8, 0), LocalTime.of(9, 20), "201", teacher = "Іваненко І. І."),
        Lesson("p2", "Програмування мобільних пристроїв", LessonType.LAB, LocalTime.of(11, 10), LocalTime.of(12, 30), "301", teacher = "Сидоренко М. С."),
        Lesson("p3", "Бази даних", LessonType.SEMINAR, LocalTime.of(14, 45), LocalTime.of(16, 5), "205", teacher = "Коваленко Т. М."),
    )
    val day = ScheduleDay(date, lessons)
    val week = ScheduleWeek(35, date.minusDays(4), date.plusDays(2), (0L..6).map { offset ->
        val value = date.minusDays(4).plusDays(offset)
        ScheduleDay(value, when (offset) { 0L -> lessons; 2L -> lessons.take(1); else -> emptyList() })
    })
}
