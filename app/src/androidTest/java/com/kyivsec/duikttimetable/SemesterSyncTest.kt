package com.kyivsec.duikttimetable

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.kyivsec.duikttimetable.data.DateRange
import com.kyivsec.duikttimetable.data.RoomScheduleRepository
import com.kyivsec.duikttimetable.data.SyncResult
import com.kyivsec.duikttimetable.data.TimetableProvider
import com.kyivsec.duikttimetable.data.TimetableProviderRegistry
import com.kyivsec.duikttimetable.data.local.TimetableDatabase
import com.kyivsec.duikttimetable.data.remote.DuiktLessonDto
import com.kyivsec.duikttimetable.data.remote.DuiktScheduleClient
import com.kyivsec.duikttimetable.data.remote.RemoteSchedule
import com.kyivsec.duikttimetable.model.GroupInfo
import com.kyivsec.duikttimetable.model.Occupation
import com.kyivsec.duikttimetable.model.TimetableOwner
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class SemesterSyncTest {
    private lateinit var database: TimetableDatabase
    private val today = LocalDate.of(2026, 9, 4)
    private val clock = Clock.fixed(Instant.parse("2026-09-04T10:00:00Z"), ZoneOffset.UTC)
    private val owner = TimetableOwner.Group(GroupInfo(17, "ПД-31", 1, "ІТ", 3))
    private val semester = LocalDate.of(2026, 9, 1)..LocalDate.of(2026, 12, 31)
    private val provider = FakeProvider()

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext, TimetableDatabase::class.java,
        ).build()
    }

    @After fun tearDown() = database.close()

    private fun repository(at: Clock = clock) = RoomScheduleRepository(
        database, DuiktScheduleClient(), clock = at,
        providers = TimetableProviderRegistry(listOf(provider)),
    )

    @Test fun dayRefreshDoesNotSuppressSemesterSync() = runBlocking {
        val repository = repository()
        repository.syncSchedule(owner, DateRange(today, today), force = true)
        assertNull(database.timetableDao().semesterSync(owner.type.name, owner.id))
        assertTrue(repository.syncCurrentSemester(owner) is SyncResult.Success)
        assertEquals(3, provider.calls.size)
        assertEquals(DateRange(semester.start, semester.endInclusive), provider.calls.last())
        assertEquals(semester.endInclusive.toString(), database.timetableDao().semesterSync(owner.type.name, owner.id)?.endDate)
    }

    @Test fun onlyFreshSemesterForTheSameOwnerSkipsFetching() = runBlocking {
        repository().syncCurrentSemester(owner)
        val later = Clock.offset(clock, Duration.ofMinutes(10))
        val result = repository(later).syncCurrentSemester(owner) as SyncResult.Success
        assertEquals(clock.instant(), result.completedAt)
        assertEquals(2, provider.calls.size)

        repository(later).syncCurrentSemester(TimetableOwner.Group(owner.group.copy(id = 18)))
        assertEquals(4, provider.calls.size)
        repository(Clock.offset(clock, Duration.ofMinutes(31))).syncCurrentSemester(owner)
        assertEquals(6, provider.calls.size)
    }

    @Test fun nextSemesterAndForcedRefreshFetchAgain() = runBlocking {
        repository().syncCurrentSemester(owner)
        repository().syncCurrentSemester(owner, force = true)
        assertEquals(4, provider.calls.size)
        // Even a recent record for a semester ending yesterday cannot satisfy today's request.
        val dao = database.timetableDao()
        dao.insertSemesterSync(requireNotNull(dao.semesterSync(owner.type.name, owner.id)).copy(endDate = today.minusDays(1).toString()))
        repository().syncCurrentSemester(owner)
        assertEquals(6, provider.calls.size)
    }

    @Test fun failedRefreshPreservesLessonsAndSemesterTimestamp() = runBlocking {
        val repository = repository()
        repository.syncCurrentSemester(owner)
        val before = database.timetableDao().semesterSync(owner.type.name, owner.id)
        val range = DateRange(today, today)
        val lessons = repository.observeSchedule(owner, range).first()
        provider.fail = true

        assertTrue(repository.syncCurrentSemester(owner, force = true) is SyncResult.Failure)
        assertEquals(before, database.timetableDao().semesterSync(owner.type.name, owner.id))
        assertEquals(lessons, repository.observeSchedule(owner, range).first())
        assertTrue(lessons.single().lessons.isNotEmpty())
    }

    @Test fun fallbackProbeNeverMarksACompleteSemester() = runBlocking {
        provider.hasSemester = false
        repository().syncCurrentSemester(owner)
        repository().syncCurrentSemester(owner)
        assertEquals(2, provider.calls.size)
        assertNull(database.timetableDao().semesterSync(owner.type.name, owner.id))
    }

    @Test fun pruningInvalidatesSemesterCoverage() = runBlocking {
        val repository = repository()
        repository.syncCurrentSemester(owner)
        repository.pruneBefore(today)
        repository.syncCurrentSemester(owner)
        assertEquals(4, provider.calls.size)
    }

    private inner class FakeProvider : TimetableProvider {
        override val occupation = Occupation.GROUP
        val calls = mutableListOf<DateRange>()
        var fail = false
        var hasSemester = true
        override suspend fun fetch(owner: TimetableOwner, range: DateRange): RemoteSchedule {
            calls += range
            if (fail) throw IOException("Offline")
            return RemoteSchedule(
                listOf(DuiktLessonDto(r1 = 1, disciplineFullName = "Algorithms", date = "04.09.2026", timeStart = "09:30", timeEnd = "10:50")),
                semester.takeIf { hasSemester },
            )
        }
    }
}
