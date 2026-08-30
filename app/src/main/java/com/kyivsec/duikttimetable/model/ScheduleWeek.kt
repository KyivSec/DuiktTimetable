package com.kyivsec.duikttimetable.model

import androidx.compose.runtime.Immutable
import java.time.LocalDate

@Immutable
data class ScheduleWeek(
    val weekNumber: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val days: List<ScheduleDay>,
)
