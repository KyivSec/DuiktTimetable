package com.kyivsec.duikttimetable.data

import com.kyivsec.duikttimetable.model.GroupInfo
import com.kyivsec.duikttimetable.model.Institute
import com.kyivsec.duikttimetable.model.ScheduleDay
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

class MockScheduleRepository(loader: MockDataLoader, private val clock: Clock = Clock.systemDefaultZone()) :
    ScheduleRepository, GroupDirectoryRepository {
    private val days = MutableStateFlow(loader.loadSchedule().flatMap { it.days })
    private val institutes = MutableStateFlow(loader.loadGroups())

    override fun observeSchedule(groupId: Long, range: DateRange): Flow<List<ScheduleDay>> = days.map { values ->
        values.filter { it.date in range.startInclusive..range.endInclusive }
    }
    override fun observeCachedDates(groupId: Long, range: DateRange): Flow<Set<LocalDate>> = days.map { values ->
        values.map { it.date }.filter { it in range.startInclusive..range.endInclusive }.toSet()
    }
    override suspend fun syncSchedule(group: GroupInfo, range: DateRange, force: Boolean): SyncResult {
        delay(650)
        return SyncResult.Success(Instant.now(clock), 0)
    }
    override suspend fun syncCurrentSemester(group: GroupInfo): SyncResult {
        delay(1_100)
        return SyncResult.Success(Instant.now(clock), days.value.sumOf { it.lessons.size })
    }
    override suspend fun pruneBefore(date: LocalDate) = Unit
    override fun observeInstitutes(): Flow<List<Institute>> = institutes
    override fun observeCourses(instituteId: Long): Flow<List<Int>> = institutes.map { list ->
        list.firstOrNull { it.id == instituteId }?.courses?.map { it.course }.orEmpty()
    }
    override fun observeGroups(instituteId: Long, course: Int): Flow<List<GroupInfo>> = institutes.map { list ->
        list.firstOrNull { it.id == instituteId }?.courses?.firstOrNull { it.course == course }?.groups.orEmpty()
    }
    override suspend fun ensureInstitutes(force: Boolean) = SyncResult.Success(Instant.now(clock), 0)
    override suspend fun ensureCourses(instituteId: Long, force: Boolean) = SyncResult.Success(Instant.now(clock), 0)
    override suspend fun ensureGroups(instituteId: Long, course: Int, force: Boolean) = SyncResult.Success(Instant.now(clock), 0)
}
