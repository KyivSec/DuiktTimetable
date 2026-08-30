package com.kyivsec.duikttimetable.data

import androidx.room.withTransaction
import com.kyivsec.duikttimetable.data.local.CachedScheduleDayEntity
import com.kyivsec.duikttimetable.data.local.CourseEntity
import com.kyivsec.duikttimetable.data.local.FacultyEntity
import com.kyivsec.duikttimetable.data.local.GroupEntity
import com.kyivsec.duikttimetable.data.local.TimetableDatabase
import com.kyivsec.duikttimetable.data.local.toDomain
import com.kyivsec.duikttimetable.data.local.toEntity
import com.kyivsec.duikttimetable.data.remote.DuiktLessonMapper
import com.kyivsec.duikttimetable.data.remote.DuiktScheduleClient
import com.kyivsec.duikttimetable.data.remote.HttpStatusException
import com.kyivsec.duikttimetable.data.remote.RejectedSessionException
import com.kyivsec.duikttimetable.data.remote.SourceFormatException
import com.kyivsec.duikttimetable.model.CourseGroups
import com.kyivsec.duikttimetable.model.GroupInfo
import com.kyivsec.duikttimetable.model.Institute
import com.kyivsec.duikttimetable.model.ScheduleDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

class RoomScheduleRepository(
    private val database: TimetableDatabase,
    private val client: DuiktScheduleClient,
    private val mapper: DuiktLessonMapper = DuiktLessonMapper(),
    private val clock: Clock = Clock.systemDefaultZone(),
) : ScheduleRepository {
    private val dao = database.timetableDao()

    override fun observeSchedule(groupId: Long, range: DateRange): Flow<List<ScheduleDay>> =
        dao.observeLessons(groupId, range.startInclusive.toString(), range.endInclusive.toString()).map { rows ->
            val grouped = rows.groupBy { LocalDate.parse(it.date) }
            range.dates.map { date -> ScheduleDay(date, grouped[date].orEmpty().map { it.toDomain() }) }
        }

    override fun observeCachedDates(groupId: Long, range: DateRange): Flow<Set<LocalDate>> =
        dao.observeCachedDays(groupId, range.startInclusive.toString(), range.endInclusive.toString())
            .map { rows -> rows.mapTo(mutableSetOf()) { LocalDate.parse(it.date) } }

    override suspend fun syncSchedule(group: GroupInfo, range: DateRange, force: Boolean): SyncResult {
        if (!force && isFresh(group.id, range)) return SyncResult.Success(Instant.now(clock), 0)
        return runCatching {
            val remote = client.fetchSchedule(group.instituteId, group.course, group.id, range)
            val mapped = remote.lessons.map { mapper.map(group.id, it) }
                .filter { (date, _) -> date in range.startInclusive..range.endInclusive }
            val completed = Instant.now(clock)
            database.withTransaction {
                dao.deleteLessons(group.id, range.startInclusive.toString(), range.endInclusive.toString())
                dao.insertLessons(mapped.map { (date, lesson) -> lesson.toEntity(group.id, date) })
                dao.insertCachedDays(range.dates.map { CachedScheduleDayEntity(group.id, it.toString(), completed.toEpochMilli()) })
            }
            SyncResult.Success(completed, mapped.size)
        }.getOrElse {
            if (it is CancellationException) throw it
            failure(group.id, range, it)
        }
    }

    override suspend fun syncCurrentSemester(group: GroupInfo): SyncResult {
        val today = LocalDate.now(clock)
        val probeRange = DateRange(today.minusWeeks(1), today.plusWeeks(1))
        return runCatching {
            val probe = client.fetchSchedule(group.instituteId, group.course, group.id, probeRange)
            val target = probe.semesterRange?.let { DateRange(it.start, it.endInclusive) } ?: probeRange
            val remote = if (target == probeRange) probe else client.fetchSchedule(group.instituteId, group.course, group.id, target)
            val mapped = remote.lessons.map { mapper.map(group.id, it) }
                .filter { (date, _) -> date in target.startInclusive..target.endInclusive }
            val completed = Instant.now(clock)
            database.withTransaction {
                dao.deleteLessons(group.id, target.startInclusive.toString(), target.endInclusive.toString())
                dao.insertLessons(mapped.map { (date, lesson) -> lesson.toEntity(group.id, date) })
                dao.insertCachedDays(target.dates.map { CachedScheduleDayEntity(group.id, it.toString(), completed.toEpochMilli()) })
            }
            SyncResult.Success(completed, mapped.size)
        }.getOrElse {
            if (it is CancellationException) throw it
            failure(group.id, probeRange, it)
        }
    }

    override suspend fun pruneBefore(date: LocalDate) = database.withTransaction {
        dao.pruneLessons(date.toString())
        dao.pruneCachedDays(date.toString())
    }

    private suspend fun isFresh(groupId: Long, range: DateRange): Boolean {
        val cutoff = Instant.now(clock).minus(SCHEDULE_STALE_AFTER).toEpochMilli()
        val rows = dao.cachedDays(groupId, range.startInclusive.toString(), range.endInclusive.toString())
        return rows.size == range.dates.size && rows.all { it.fetchedAt >= cutoff }
    }

    private suspend fun failure(groupId: Long, range: DateRange, error: Throwable): SyncResult.Failure {
        val hasCache = dao.cachedDays(groupId, range.startInclusive.toString(), range.endInclusive.toString()).isNotEmpty()
        return SyncResult.Failure(error.toDataError(), hasCache)
    }

    private companion object { val SCHEDULE_STALE_AFTER: Duration = Duration.ofMinutes(30) }
}

class RoomGroupDirectoryRepository(
    private val database: TimetableDatabase,
    private val client: DuiktScheduleClient,
    private val clock: Clock = Clock.systemDefaultZone(),
) : GroupDirectoryRepository {
    private val dao = database.timetableDao()

    override fun observeInstitutes(): Flow<List<Institute>> = dao.observeFaculties().map { rows ->
        rows.map { Institute(it.id, it.name, emptyList()) }
            .sortedWith(compareBy<Institute> { it.name.contains("Аспірантура", ignoreCase = true) }.thenBy { it.name })
    }
    override fun observeCourses(instituteId: Long): Flow<List<Int>> = dao.observeCourses(instituteId).map { rows -> rows.map { it.course } }
    override fun observeGroups(instituteId: Long, course: Int): Flow<List<GroupInfo>> = dao.observeGroups(instituteId, course).map { rows ->
        val facultyName = dao.faculty(instituteId)?.name.orEmpty()
        rows.map { it.toDomain(facultyName) }
    }

    override suspend fun ensureInstitutes(force: Boolean): SyncResult = syncIfNeeded(force, dao.facultiesFetchedAt()) {
        val now = Instant.now(clock)
        val values = client.fetchInstitutes().map { FacultyEntity(it.id, it.name, now.toEpochMilli()) }
        dao.insertFaculties(values)
        SyncResult.Success(now, values.size)
    }
    override suspend fun ensureCourses(instituteId: Long, force: Boolean): SyncResult = syncIfNeeded(force, dao.coursesFetchedAt(instituteId)) {
        val now = Instant.now(clock)
        val values = client.fetchCourses(instituteId).map { CourseEntity(instituteId, it, now.toEpochMilli()) }
        dao.insertCourses(values)
        SyncResult.Success(now, values.size)
    }
    override suspend fun ensureGroups(instituteId: Long, course: Int, force: Boolean): SyncResult = syncIfNeeded(force, dao.groupsFetchedAt(instituteId, course)) {
        val now = Instant.now(clock)
        val values = client.fetchGroups(instituteId, course).map { GroupEntity(it.id, instituteId, course, it.name, now.toEpochMilli()) }
        dao.insertGroups(values)
        SyncResult.Success(now, values.size)
    }

    private suspend fun syncIfNeeded(force: Boolean, fetchedAt: Long?, block: suspend () -> SyncResult): SyncResult {
        if (!force && fetchedAt != null && fetchedAt >= Instant.now(clock).minus(DIRECTORY_STALE_AFTER).toEpochMilli()) {
            return SyncResult.Success(Instant.now(clock), 0)
        }
        return runCatching { block() }.getOrElse {
            if (it is CancellationException) throw it
            SyncResult.Failure(it.toDataError(), fetchedAt != null)
        }
    }
    private companion object { val DIRECTORY_STALE_AFTER: Duration = Duration.ofDays(7) }
}

private fun Throwable.toDataError(): DataError = when (this) {
    is SocketTimeoutException -> DataError.Timeout
    is RejectedSessionException -> DataError.RejectedSession
    is HttpStatusException -> DataError.Http(statusCode)
    is SourceFormatException -> DataError.SourceFormat(message.orEmpty())
    is IOException -> DataError.Offline
    else -> DataError.Database(message.orEmpty())
}
