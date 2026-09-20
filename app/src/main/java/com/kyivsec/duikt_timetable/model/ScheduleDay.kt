package com.kyivsec.duikt_timetable.model

import androidx.compose.runtime.Immutable
import java.time.LocalDate

@Immutable
data class ScheduleDay(
    val date: LocalDate,
    val lessons: List<Lesson>,
)
