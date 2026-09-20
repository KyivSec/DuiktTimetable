package com.kyivsec.duikt_timetable.data

import com.kyivsec.duikt_timetable.model.Institute
import com.kyivsec.duikt_timetable.model.GroupInfo
import com.kyivsec.duikt_timetable.model.ScheduleDay
import com.kyivsec.duikt_timetable.model.ChairInfo
import com.kyivsec.duikt_timetable.model.TeacherInfo
import com.kyivsec.duikt_timetable.model.TimetableOwner
import com.kyivsec.duikt_timetable.model.StudentInfo
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
    fun observeSchedule(owner: TimetableOwner, range: DateRange): Flow<List<ScheduleDay>>
    fun observeCachedDates(owner: TimetableOwner, range: DateRange): Flow<Set<LocalDate>>
    suspend fun syncSchedule(owner: TimetableOwner, range: DateRange, force: Boolean = false): SyncResult
    suspend fun syncCurrentSemester(owner: TimetableOwner, force: Boolean = false): SyncResult
    suspend fun pruneBefore(date: LocalDate)
}

interface TeacherDirectoryRepository {
    fun observeChairs(): Flow<List<ChairInfo>>
    fun observeTeachers(chairId: Long): Flow<List<TeacherInfo>>
    suspend fun ensureChairs(force: Boolean = false): SyncResult
    suspend fun ensureTeachers(chairId: Long, force: Boolean = false): SyncResult
}

interface StudentDirectoryRepository {
    fun observeStudentInstitutes(): Flow<List<Institute>>
    fun observeStudentCourses(instituteId: Long): Flow<List<Int>>
    fun observeStudentGroups(instituteId: Long, course: Int): Flow<List<GroupInfo>>
    suspend fun ensureStudentInstitutes(force: Boolean = false): SyncResult
    suspend fun ensureStudentCourses(instituteId: Long, force: Boolean = false): SyncResult
    suspend fun ensureStudentGroups(instituteId: Long, course: Int, force: Boolean = false): SyncResult
    fun observeStudents(groupId: Long): Flow<List<StudentInfo>>
    suspend fun ensureStudents(group: GroupInfo, force: Boolean = false): SyncResult
}

interface GroupDirectoryRepository {
    fun observeInstitutes(): Flow<List<Institute>>
    fun observeCourses(instituteId: Long): Flow<List<Int>>
    fun observeGroups(instituteId: Long, course: Int): Flow<List<GroupInfo>>
    suspend fun ensureInstitutes(force: Boolean = false): SyncResult
    suspend fun ensureCourses(instituteId: Long, force: Boolean = false): SyncResult
    suspend fun ensureGroups(instituteId: Long, course: Int, force: Boolean = false): SyncResult
}
