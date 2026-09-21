package com.kyivsec.duikt_timetable.model

import androidx.compose.runtime.Immutable

enum class ScheduleMode { DAY, WEEK }
enum class ThemeMode { SYSTEM, LIGHT, DARK, HIGH_CONTRAST_LIGHT, HIGH_CONTRAST_DARK, OLED }

@Immutable
data class ScheduleSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val startupMode: ScheduleMode = ScheduleMode.DAY,
    val hideClassesInWeekView: Boolean = false,
    val immediateNotificationsEnabled: Boolean = false,
    val persistentNotificationEnabled: Boolean = false,
    val fastUpdate: Boolean = false,
    val previousDaysToKeep: Int = 14,
    val previousWeeksToKeep: Int = 4,
)
