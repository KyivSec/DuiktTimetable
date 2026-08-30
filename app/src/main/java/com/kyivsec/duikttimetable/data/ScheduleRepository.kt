package com.kyivsec.duikttimetable.data

import com.kyivsec.duikttimetable.model.Institute
import com.kyivsec.duikttimetable.model.GroupInfo
import com.kyivsec.duikttimetable.model.ScheduleDay
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

data class DateRange(val startInclusive: LocalDate, val endInclusive: LocalDate) {
    init { require(!endInclusive.isBefore(startInclusive)) }
    val dates: List<LocalDate> get() = generateSequence(startInclusive) { current ->
        current.plusDays(1).takeIf { !it.isAfter(endInclusive) }
    }.toList()
}

sealed interface DataError {
    data object Offline : DataError
    data object Timeout : DataError
    data class Http(val code: Int) : DataError
    data object RejectedSession : DataError
    data class SourceFormat(val detail: String) : DataError
    data class Database(val detail: String) : DataError
}

sealed interface SyncResult {
    data class Success(val completedAt: Instant, val changedLessonCount: Int) : SyncResult
    data class Failure(val error: DataError, val hasCachedData: Boolean) : SyncResult
}

interface ScheduleRepository {
    fun observeSchedule(groupId: Long, range: DateRange): Flow<List<ScheduleDay>>
    fun observeCachedDates(groupId: Long, range: DateRange): Flow<Set<LocalDate>>
    suspend fun syncSchedule(group: GroupInfo, range: DateRange, force: Boolean = false): SyncResult
    suspend fun syncCurrentSemester(group: GroupInfo): SyncResult
    suspend fun pruneBefore(date: LocalDate)
}

interface GroupDirectoryRepository {
    fun observeInstitutes(): Flow<List<Institute>>
    fun observeCourses(instituteId: Long): Flow<List<Int>>
    fun observeGroups(instituteId: Long, course: Int): Flow<List<GroupInfo>>
    suspend fun ensureInstitutes(force: Boolean = false): SyncResult
    suspend fun ensureCourses(instituteId: Long, force: Boolean = false): SyncResult
    suspend fun ensureGroups(instituteId: Long, course: Int, force: Boolean = false): SyncResult
}
