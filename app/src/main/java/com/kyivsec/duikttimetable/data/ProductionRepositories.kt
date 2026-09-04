package com.kyivsec.duikttimetable.data

import androidx.room.withTransaction
import com.kyivsec.duikttimetable.data.local.CachedScheduleDayEntity
import com.kyivsec.duikttimetable.data.local.CourseEntity
import com.kyivsec.duikttimetable.data.local.FacultyEntity
import com.kyivsec.duikttimetable.data.local.GroupEntity
import com.kyivsec.duikttimetable.data.local.ChairEntity
import com.kyivsec.duikttimetable.data.local.TeacherEntity
import com.kyivsec.duikttimetable.data.local.StudentEntity
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
import com.kyivsec.duikttimetable.model.ChairInfo
import com.kyivsec.duikttimetable.model.TeacherInfo
import com.kyivsec.duikttimetable.model.TimetableOwner
import com.kyivsec.duikttimetable.model.StudentInfo
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
    private val providers: TimetableProviderRegistry = TimetableProviderRegistry(
        listOf(GroupTimetableProvider(client), StudentTimetableProvider(client), TeacherTimetableProvider(client)),
    ),
) : ScheduleRepository {
    private val dao = database.timetableDao()

    override fun observeSchedule(owner: TimetableOwner, range: DateRange): Flow<List<ScheduleDay>> =
        dao.observeLessons(owner.type.name, owner.id, range.startInclusive.toString(), range.endInclusive.toString()).map { rows ->
            val grouped = rows.groupBy { LocalDate.parse(it.date) }
            range.dates.map { date -> ScheduleDay(date, grouped[date].orEmpty().map { it.toDomain() }) }
        }

    override fun observeCachedDates(owner: TimetableOwner, range: DateRange): Flow<Set<LocalDate>> =
        dao.observeCachedDays(owner.type.name, owner.id, range.startInclusive.toString(), range.endInclusive.toString())
            .map { rows -> rows.mapTo(mutableSetOf()) { LocalDate.parse(it.date) } }

    override suspend fun syncSchedule(owner: TimetableOwner, range: DateRange, force: Boolean): SyncResult {
        if (!force && isFresh(owner, range)) return SyncResult.Success(Instant.now(clock), 0)
        return runCatching {
            val remote = providers.providerFor(owner).fetch(owner, range)
            val mapped = remote.lessons.map { mapper.map(owner, it) }
                .filter { (date, _) -> date in range.startInclusive..range.endInclusive }
            val completed = Instant.now(clock)
            database.withTransaction {
                dao.deleteLessons(owner.type.name, owner.id, range.startInclusive.toString(), range.endInclusive.toString())
                dao.insertLessons(mapped.map { (date, lesson) -> lesson.toEntity(owner, date) })
                dao.insertCachedDays(range.dates.map { CachedScheduleDayEntity(owner.type.name, owner.id, it.toString(), completed.toEpochMilli()) })
            }
            SyncResult.Success(completed, mapped.size)
        }.getOrElse {
            if (it is CancellationException) throw it
            failure(owner, range, it)
        }
    }

    override suspend fun syncCurrentSemester(owner: TimetableOwner): SyncResult {
        val today = LocalDate.now(clock)
        val probeRange = DateRange(today.minusWeeks(1), today.plusWeeks(1))
        return runCatching {
            val provider = providers.providerFor(owner)
            val probe = provider.fetch(owner, probeRange)
            val target = probe.semesterRange?.let { DateRange(it.start, it.endInclusive) } ?: probeRange
            val remote = if (target == probeRange) probe else provider.fetch(owner, target)
            val mapped = remote.lessons.map { mapper.map(owner, it) }
                .filter { (date, _) -> date in target.startInclusive..target.endInclusive }
            val completed = Instant.now(clock)
            database.withTransaction {
                dao.deleteLessons(owner.type.name, owner.id, target.startInclusive.toString(), target.endInclusive.toString())
                dao.insertLessons(mapped.map { (date, lesson) -> lesson.toEntity(owner, date) })
                dao.insertCachedDays(target.dates.map { CachedScheduleDayEntity(owner.type.name, owner.id, it.toString(), completed.toEpochMilli()) })
            }
            SyncResult.Success(completed, mapped.size)
        }.getOrElse {
            if (it is CancellationException) throw it
            failure(owner, probeRange, it)
        }
    }

    override suspend fun pruneBefore(date: LocalDate) = database.withTransaction {
        dao.pruneLessons(date.toString())
        dao.pruneCachedDays(date.toString())
    }

    private suspend fun isFresh(owner: TimetableOwner, range: DateRange): Boolean {
        val cutoff = Instant.now(clock).minus(SCHEDULE_STALE_AFTER).toEpochMilli()
        val rows = dao.cachedDays(owner.type.name, owner.id, range.startInclusive.toString(), range.endInclusive.toString())
        return rows.size == range.dates.size && rows.all { it.fetchedAt >= cutoff }
    }

    private suspend fun failure(owner: TimetableOwner, range: DateRange, error: Throwable): SyncResult.Failure {
        val hasCache = dao.cachedDays(owner.type.name, owner.id, range.startInclusive.toString(), range.endInclusive.toString()).isNotEmpty()
        return SyncResult.Failure(error.toDataError(), hasCache)
    }

    private companion object { val SCHEDULE_STALE_AFTER: Duration = Duration.ofMinutes(30) }
}

class RoomTeacherDirectoryRepository(
    private val database: TimetableDatabase,
    private val client: DuiktScheduleClient,
    private val clock: Clock = Clock.systemDefaultZone(),
) : TeacherDirectoryRepository {
    private val dao = database.timetableDao()
    override fun observeChairs(): Flow<List<ChairInfo>> = dao.observeChairs().map { rows -> rows.map { ChairInfo(it.id, it.name) } }
    override fun observeTeachers(chairId: Long): Flow<List<TeacherInfo>> = dao.observeTeachers(chairId).map { rows ->
        val chairName = dao.observeChairs().first().firstOrNull { it.id == chairId }?.name.orEmpty()
        rows.map { it.toDomain(chairName) }
    }
    override suspend fun ensureChairs(force: Boolean): SyncResult = syncIfNeeded(force, dao.chairsFetchedAt()) {
        val now = Instant.now(clock)
        val values = client.fetchChairs().map { ChairEntity(it.id, it.name, now.toEpochMilli()) }
        dao.insertChairs(values)
        SyncResult.Success(now, values.size)
    }
    override suspend fun ensureTeachers(chairId: Long, force: Boolean): SyncResult = syncIfNeeded(force, dao.teachersFetchedAt(chairId)) {
        val now = Instant.now(clock)
        val values = client.fetchTeachers(chairId).map { TeacherEntity(it.id, chairId, it.name, now.toEpochMilli()) }
        dao.insertTeachers(values)
        SyncResult.Success(now, values.size)
    }
    private suspend fun syncIfNeeded(force: Boolean, fetchedAt: Long?, block: suspend () -> SyncResult): SyncResult {
        if (!force && fetchedAt != null && fetchedAt >= Instant.now(clock).minus(Duration.ofDays(7)).toEpochMilli()) return SyncResult.Success(Instant.now(clock), 0)
        return runCatching { block() }.getOrElse {
            if (it is CancellationException) throw it
            SyncResult.Failure(it.toDataError(), fetchedAt != null)
        }
    }
}

class RoomStudentDirectoryRepository(
    private val database: TimetableDatabase,
    private val client: DuiktScheduleClient,
    private val clock: Clock = Clock.systemDefaultZone(),
) : StudentDirectoryRepository {
    private val dao = database.timetableDao()

    override suspend fun ensureStudentInstitutes(force: Boolean): SyncResult = syncDirectory(force, dao.facultiesFetchedAt()) {
        val now = Instant.now(clock)
        val values = client.fetchStudentInstitutes().map { FacultyEntity(it.id, it.name, now.toEpochMilli()) }
        dao.insertFaculties(values)
        SyncResult.Success(now, values.size)
    }

    override suspend fun ensureStudentCourses(instituteId: Long, force: Boolean): SyncResult = syncDirectory(force, dao.coursesFetchedAt(instituteId)) {
        val now = Instant.now(clock)
        val values = client.fetchStudentCourses(instituteId).map { CourseEntity(instituteId, it, now.toEpochMilli()) }
        dao.insertCourses(values)
        SyncResult.Success(now, values.size)
    }

    override suspend fun ensureStudentGroups(instituteId: Long, course: Int, force: Boolean): SyncResult = syncDirectory(force, dao.groupsFetchedAt(instituteId, course)) {
        val now = Instant.now(clock)
        val values = client.fetchStudentGroups(instituteId, course).map { GroupEntity(it.id, instituteId, course, it.name, now.toEpochMilli()) }
        dao.insertGroups(values)
        SyncResult.Success(now, values.size)
    }

    override fun observeStudents(groupId: Long): Flow<List<StudentInfo>> = dao.observeStudents(groupId).map { rows ->
        val facultyNames = rows.map(StudentEntity::facultyId).distinct().associateWith { dao.faculty(it)?.name.orEmpty() }
        rows.map { it.toDomain(facultyNames[it.facultyId].orEmpty()) }
    }

    override suspend fun ensureStudents(group: GroupInfo, force: Boolean): SyncResult {
        val fetchedAt = dao.studentsFetchedAt(group.id)
        if (!force && fetchedAt != null && fetchedAt >= Instant.now(clock).minus(Duration.ofDays(7)).toEpochMilli()) {
            return SyncResult.Success(Instant.now(clock), 0)
        }
        return runCatching {
            val now = Instant.now(clock)
            val values = client.fetchStudents(group.instituteId, group.course, group.id).map {
                StudentEntity(it.id, group.id, group.name, group.instituteId, group.course, it.name, now.toEpochMilli())
            }
            dao.insertStudents(values)
            SyncResult.Success(now, values.size)
        }.getOrElse {
            if (it is CancellationException) throw it
            SyncResult.Failure(it.toDataError(), fetchedAt != null)
        }
    }

    private suspend fun syncDirectory(force: Boolean, fetchedAt: Long?, block: suspend () -> SyncResult): SyncResult {
        if (!force && fetchedAt != null && fetchedAt >= Instant.now(clock).minus(Duration.ofDays(7)).toEpochMilli()) {
            return SyncResult.Success(Instant.now(clock), 0)
        }
        return runCatching { block() }.getOrElse {
            if (it is CancellationException) throw it
            SyncResult.Failure(it.toDataError(), fetchedAt != null)
        }
    }
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
