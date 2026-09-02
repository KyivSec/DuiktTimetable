package com.kyivsec.duikttimetable.model

import androidx.compose.runtime.Immutable
import androidx.annotation.StringRes
import java.time.LocalDate

@Immutable
data class ScheduleUiState(
    val isLoading: Boolean = true,
    val errorMessage: UiMessage? = null,
    val selectedMode: ScheduleMode = ScheduleMode.DAY,
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedWeek: LocalDate = LocalDate.now(),
    val selectedGroup: GroupInfo? = null,
    val needsGroupSelection: Boolean = false,
    val institutes: List<Institute> = emptyList(),
    val directoryCourses: List<Int> = emptyList(),
    val directoryGroups: List<GroupInfo> = emptyList(),
    val draftInstituteId: Long? = null,
    val draftCourse: Int? = null,
    val draftGroup: GroupInfo? = null,
    val isCoursesLoading: Boolean = false,
    val isGroupsLoading: Boolean = false,
    val weeks: List<ScheduleWeek> = emptyList(),
    val availableDates: List<LocalDate> = emptyList(),
    val expandedDates: Set<LocalDate> = emptySet(),
    val expandedDatesInitialized: Boolean = false,
    val weekSectionsStartExpanded: Boolean = true,
    val isRefreshing: Boolean = false,
    val isFullReloading: Boolean = false,
    val settings: ScheduleSettings = ScheduleSettings(),
    val lastFullRefreshEpochMillis: Long? = null,
)

@Immutable
data class UiMessage(@param:StringRes val resourceId: Int, val numberArgument: Int? = null)
