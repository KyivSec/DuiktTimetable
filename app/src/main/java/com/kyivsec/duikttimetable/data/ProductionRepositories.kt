package com.kyivsec.duikttimetable.data

import androidx.room.withTransaction
import com.kyivsec.duikttimetable.data.local.CachedScheduleDayEntity
import com.kyivsec.duikttimetable.data.local.SemesterSyncEntity
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

    override suspend fun syncCurrentSemester(owner: TimetableOwner, force: Boolean): SyncResult {
        val today = LocalDate.now(clock)
        val probeRange = DateRange(today.minusWeeks(1), today.plusWeeks(1))
        val previous = dao.semesterSync(owner.type.name, owner.id)
        if (!force && previous != null &&
            today.toString() in previous.startDate..previous.endDate &&
            previous.fetchedAt >= Instant.now(clock).minus(SCHEDULE_STALE_AFTER).toEpochMilli()
        ) return SyncResult.Success(Instant.ofEpochMilli(previous.fetchedAt), 0)
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
                if (probe.semesterRange != null) {
                    dao.insertSemesterSync(SemesterSyncEntity(
                        owner.type.name, owner.id, target.startInclusive.toString(),
                        target.endInclusive.toString(), completed.toEpochMilli(),
                    ))
                }
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
        dao.invalidatePrunedSemesters(date.toString())
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
    private val client: com.kyivsec.duikttimetable.data.remote.DirectoryClient,
    private val clock: Clock = Clock.systemDefaultZone(),
) : TeacherDirectoryRepository {
    private val dao = database.timetableDao()
    private val synchronizer = DirectorySynchronizer(database, clock)

    override fun observeChairs(): Flow<List<ChairInfo>> = dao.observeChairs().map { rows -> rows.map { ChairInfo(it.id, it.name) } }
    override fun observeTeachers(chairId: Long): Flow<List<TeacherInfo>> = dao.observeTeachers(chairId).map { rows ->
        val chairName = dao.observeChairs().first().firstOrNull { it.id == chairId }?.name.orEmpty()
        rows.map { it.toDomain(chairName) }
    }
    override suspend fun ensureChairs(force: Boolean): SyncResult = synchronizer.sync(
        "TEACHER/chairs", force, { dao.chairsFetchedAt() != null }, client::fetchChairs,
    ) { values, fetchedAt ->
        dao.deleteChairs()
        dao.insertChairs(values.map { ChairEntity(it.id, it.name, fetchedAt) })
    }
    override suspend fun ensureTeachers(chairId: Long, force: Boolean): SyncResult = synchronizer.sync(
        "TEACHER/teachers/$chairId", force, { dao.teachersFetchedAt(chairId) != null },
        { client.fetchTeachers(chairId) },
    ) { values, fetchedAt ->
        dao.deleteTeachers(chairId)
        dao.insertTeachers(values.map { TeacherEntity(it.id, chairId, it.name, fetchedAt) })
    }
}

class RoomStudentDirectoryRepository(
    private val database: TimetableDatabase,
    private val client: com.kyivsec.duikttimetable.data.remote.DirectoryClient,
    private val clock: Clock = Clock.systemDefaultZone(),
) : StudentDirectoryRepository {
    private val dao = database.timetableDao()
    private val synchronizer = DirectorySynchronizer(database, clock)

    override fun observeStudentInstitutes(): Flow<List<Institute>> = dao.observeFaculties("STUDENT").map { rows ->
        rows.map { Institute(it.id, it.name, emptyList()) }
            .sortedWith(compareBy<Institute> { it.name.contains("Аспірантура", ignoreCase = true) }.thenBy { it.name })
    }
    override fun observeStudentCourses(instituteId: Long): Flow<List<Int>> =
        dao.observeCourses(instituteId, "STUDENT").map { rows -> rows.map { it.course } }
    override fun observeStudentGroups(instituteId: Long, course: Int): Flow<List<GroupInfo>> =
        dao.observeGroups(instituteId, course, "STUDENT").map { rows ->
            val facultyName = dao.faculty(instituteId, "STUDENT")?.name.orEmpty()
            rows.map { it.toDomain(facultyName) }
        }
    override suspend fun ensureStudentInstitutes(force: Boolean): SyncResult = synchronizer.sync(
        "STUDENT/faculties", force, { dao.facultiesFetchedAt("STUDENT") != null }, client::fetchStudentInstitutes,
    ) { values, fetchedAt ->
        dao.deleteFaculties("STUDENT")
        dao.insertFaculties(values.map { FacultyEntity(it.id, it.name, fetchedAt, "STUDENT") })
    }
    override suspend fun ensureStudentCourses(instituteId: Long, force: Boolean): SyncResult = synchronizer.sync(
        "STUDENT/courses/$instituteId", force, { dao.coursesFetchedAt(instituteId, "STUDENT") != null },
        { client.fetchStudentCourses(instituteId) },
    ) { values, fetchedAt ->
        dao.deleteCourses(instituteId, "STUDENT")
        dao.insertCourses(values.map { CourseEntity(instituteId, it, fetchedAt, "STUDENT") })
    }
    override suspend fun ensureStudentGroups(instituteId: Long, course: Int, force: Boolean): SyncResult = synchronizer.sync(
        "STUDENT/groups/$instituteId/$course", force, { dao.groupsFetchedAt(instituteId, course, "STUDENT") != null },
        { client.fetchStudentGroups(instituteId, course) },
    ) { values, fetchedAt ->
        dao.deleteGroups(instituteId, course, "STUDENT")
        dao.insertGroups(values.map { GroupEntity(it.id, instituteId, course, it.name, fetchedAt, "STUDENT") })
    }
    override fun observeStudents(groupId: Long): Flow<List<StudentInfo>> = dao.observeStudents(groupId).map { rows ->
        val facultyNames = rows.map(StudentEntity::facultyId).distinct().associateWith { dao.faculty(it, "STUDENT")?.name.orEmpty() }
        rows.map { it.toDomain(facultyNames[it.facultyId].orEmpty()) }
    }
    override suspend fun ensureStudents(group: GroupInfo, force: Boolean): SyncResult = synchronizer.sync(
        "STUDENT/students/${group.id}", force, { dao.studentsFetchedAt(group.id) != null },
        { client.fetchStudents(group.instituteId, group.course, group.id) },
    ) { values, fetchedAt ->
        dao.deleteStudents(group.id)
        dao.insertStudents(values.map { StudentEntity(it.id, group.id, group.name, group.instituteId, group.course, it.name, fetchedAt) })
    }
}

class RoomGroupDirectoryRepository(
    private val database: TimetableDatabase,
    private val client: com.kyivsec.duikttimetable.data.remote.DirectoryClient,
    private val clock: Clock = Clock.systemDefaultZone(),
) : GroupDirectoryRepository {
    private val dao = database.timetableDao()
    private val synchronizer = DirectorySynchronizer(database, clock)

    override fun observeInstitutes(): Flow<List<Institute>> = dao.observeFaculties().map { rows ->
        rows.map { Institute(it.id, it.name, emptyList()) }
            .sortedWith(compareBy<Institute> { it.name.contains("Аспірантура", ignoreCase = true) }.thenBy { it.name })
    }
    override fun observeCourses(instituteId: Long): Flow<List<Int>> = dao.observeCourses(instituteId).map { rows -> rows.map { it.course } }
    override fun observeGroups(instituteId: Long, course: Int): Flow<List<GroupInfo>> = dao.observeGroups(instituteId, course).map { rows ->
        val facultyName = dao.faculty(instituteId)?.name.orEmpty()
        rows.map { it.toDomain(facultyName) }
    }
    override suspend fun ensureInstitutes(force: Boolean): SyncResult = synchronizer.sync(
        "GROUP/faculties", force, { dao.facultiesFetchedAt() != null }, client::fetchInstitutes,
    ) { values, fetchedAt ->
        dao.deleteFaculties()
        dao.insertFaculties(values.map { FacultyEntity(it.id, it.name, fetchedAt) })
    }
    override suspend fun ensureCourses(instituteId: Long, force: Boolean): SyncResult = synchronizer.sync(
        "GROUP/courses/$instituteId", force, { dao.coursesFetchedAt(instituteId) != null },
        { client.fetchCourses(instituteId) },
    ) { values, fetchedAt ->
        dao.deleteCourses(instituteId)
        dao.insertCourses(values.map { CourseEntity(instituteId, it, fetchedAt) })
    }
    override suspend fun ensureGroups(instituteId: Long, course: Int, force: Boolean): SyncResult = synchronizer.sync(
        "GROUP/groups/$instituteId/$course", force, { dao.groupsFetchedAt(instituteId, course) != null },
        { client.fetchGroups(instituteId, course) },
    ) { values, fetchedAt ->
        dao.deleteGroups(instituteId, course)
        dao.insertGroups(values.map { GroupEntity(it.id, instituteId, course, it.name, fetchedAt) })
    }
}

internal fun Throwable.toDataError(): DataError = when (this) {
    is SocketTimeoutException -> DataError.Timeout
    is RejectedSessionException -> DataError.RejectedSession
    is HttpStatusException -> DataError.Http(statusCode)
    is SourceFormatException -> DataError.SourceFormat(message.orEmpty())
    is IOException -> DataError.Offline
    else -> DataError.Database(message.orEmpty())
}
