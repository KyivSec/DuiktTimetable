package com.kyivsec.duikttimetable.model

import androidx.compose.runtime.Immutable
import java.time.DayOfWeek

enum class ScheduleMode { DAY, WEEK }
enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Immutable
data class ScheduleSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val previousDaysToKeep: Int = 14,
    val previousWeeksToKeep: Int = 4,
)
