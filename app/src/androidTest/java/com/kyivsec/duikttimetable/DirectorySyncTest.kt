package com.kyivsec.duikttimetable

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.kyivsec.duikttimetable.data.*
import com.kyivsec.duikttimetable.data.local.*
import com.kyivsec.duikttimetable.data.remote.DirectoryClient
import com.kyivsec.duikttimetable.data.remote.RemoteOption
import com.kyivsec.duikttimetable.data.remote.SourceFormatException
import com.kyivsec.duikttimetable.model.GroupInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class DirectorySyncTest {
    private lateinit var database: TimetableDatabase
    private val client = FakeDirectoryClient()
    private val clock = Clock.fixed(Instant.parse("2026-09-04T10:00:00Z"), ZoneOffset.UTC)

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext, TimetableDatabase::class.java,
        ).build()
    }
    @After fun tearDown() = database.close()

    private data class Branch(
        val key: String,
        val refresh: suspend (Boolean) -> SyncResult,
        val ids: suspend () -> List<Long>,
    )

    private fun branches(at: Clock = clock): List<Branch> {
        val groups = RoomGroupDirectoryRepository(database, client, at)
        val students = RoomStudentDirectoryRepository(database, client, at)
        val teachers = RoomTeacherDirectoryRepository(database, client, at)
        val group = GroupInfo(17, "ПД-31", 1, "ІТ", 3)
        return listOf(
            Branch("GROUP/faculties", { groups.ensureInstitutes(it) }, { groups.observeInstitutes().first().map { it.id } }),
            Branch("GROUP/courses/1", { groups.ensureCourses(1, it) }, { groups.observeCourses(1).first().map(Int::toLong) }),
            Branch("GROUP/groups/1/3", { groups.ensureGroups(1, 3, it) }, { groups.observeGroups(1, 3).first().map { it.id } }),
            Branch("STUDENT/faculties", { students.ensureStudentInstitutes(it) }, { students.observeStudentInstitutes().first().map { it.id } }),
            Branch("STUDENT/courses/1", { students.ensureStudentCourses(1, it) }, { students.observeStudentCourses(1).first().map(Int::toLong) }),
            Branch("STUDENT/groups/1/3", { students.ensureStudentGroups(1, 3, it) }, { students.observeStudentGroups(1, 3).first().map { it.id } }),
            Branch("STUDENT/students/17", { students.ensureStudents(group, it) }, { students.observeStudents(17).first().map { it.id } }),
            Branch("TEACHER/chairs", { teachers.ensureChairs(it) }, { teachers.observeChairs().first().map { it.id } }),
            Branch("TEACHER/teachers/42", { teachers.ensureTeachers(42, it) }, { teachers.observeTeachers(42).first().map { it.id } }),
        )
    }

    @Test fun everyBranchReconcilesRemovedRowsAndCachesEmptySuccess() = runBlocking {
        for (branch in branches()) {
            client.values = listOf(1, 2)
            assertTrue(branch.refresh(true) is SyncResult.Success)
            client.values = listOf(2, 3)
            assertTrue(branch.refresh(true) is SyncResult.Success)
            assertEquals(branch.key, listOf(2L, 3L), branch.ids().sorted())

            client.values = emptyList()
            assertTrue(branch.refresh(true) is SyncResult.Success)
            assertTrue(branch.ids().isEmpty())
            val calls = client.calls
            assertTrue(branch.refresh(false) is SyncResult.Success)
            assertEquals(calls, client.calls)

            client.values = listOf(4)
            val expired = branches(Clock.offset(clock, Duration.ofDays(8))).first { it.key == branch.key }
            assertTrue(expired.refresh(false) is SyncResult.Success)
            assertEquals(listOf(4L), branch.ids())
        }
    }

    @Test fun failedAndCancelledFetchesPreserveEveryBranch() = runBlocking {
        for (branch in branches()) {
            client.values = listOf(1, 2)
            branch.refresh(true)
            val timestamp = database.timetableDao().directoryFetchedAt(branch.key)
            for (error in listOf(IOException("Offline"), SourceFormatException("Malformed response"), CancellationException("Cancelled"))) {
                client.failure = error
                try {
                    val result = branch.refresh(true)
                    assertTrue(result is SyncResult.Failure)
                    assertTrue((result as SyncResult.Failure).hasCachedData)
                    assertFalse(error is CancellationException)
                } catch (cancelled: CancellationException) {
                    assertTrue(error is CancellationException)
                } finally {
                    client.failure = null
                }
                assertEquals(listOf(1L, 2L), branch.ids().sorted())
                assertEquals(timestamp, database.timetableDao().directoryFetchedAt(branch.key))
            }
        }
    }

    @Test fun replacingOneSourceAndBranchDoesNotEraseAnother() = runBlocking {
        val groups = RoomGroupDirectoryRepository(database, client, clock)
        val students = RoomStudentDirectoryRepository(database, client, clock)
        client.values = listOf(1)
        groups.ensureGroups(1, 3, true)
        client.values = listOf(2)
        groups.ensureGroups(2, 4, true)
        client.values = listOf(3)
        students.ensureStudentGroups(1, 3, true)
        client.values = emptyList()
        groups.ensureGroups(1, 3, true)
        assertTrue(groups.observeGroups(1, 3).first().isEmpty())
        assertEquals(1, groups.observeGroups(2, 4).first().size)
        assertEquals(1, students.observeStudentGroups(1, 3).first().size)
    }

    @Test fun replacementFailureRollsBackRowsAndFreshness() = runBlocking {
        val dao = database.timetableDao()
        dao.insertChairs(listOf(ChairEntity(1, "Original", 123)))
        dao.insertDirectorySync(DirectorySyncEntity("TEACHER/chairs", 123))
        val result = DirectorySynchronizer(database, clock).sync(
            "TEACHER/chairs", true, { true }, { emptyList<Long>() },
        ) { _, _ ->
            dao.deleteChairs()
            throw IllegalStateException("Write failed")
        }
        assertTrue(result is SyncResult.Failure)
        assertEquals("Original", dao.observeChairs().first().single().name)
        assertEquals(123L, dao.directoryFetchedAt("TEACHER/chairs"))
    }

    @Test fun concurrentRefreshesRecheckFreshnessAfterWaiting() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        client.beforeFetch = { entered.complete(Unit); release.await() }
        val firstRepository = RoomGroupDirectoryRepository(database, client, clock)
        val secondRepository = RoomGroupDirectoryRepository(database, client, clock)
        val first = async { firstRepository.ensureInstitutes() }
        entered.await()
        val second = async { secondRepository.ensureInstitutes() }
        release.complete(Unit)
        assertTrue(first.await() is SyncResult.Success)
        assertTrue(second.await() is SyncResult.Success)
        assertEquals(1, client.calls)
    }

    private class FakeDirectoryClient : DirectoryClient {
        var values = listOf(1, 2)
        var failure: Exception? = null
        var calls = 0
        var beforeFetch: suspend () -> Unit = {}
        private suspend fun fetch(): List<RemoteOption> {
            calls++
            beforeFetch()
            failure?.let { throw it }
            return values.map { RemoteOption(it.toLong(), "Name $it") }
        }
        override suspend fun fetchInstitutes() = fetch()
        override suspend fun fetchCourses(facultyId: Long) = fetch().map { it.id.toInt() }
        override suspend fun fetchGroups(facultyId: Long, course: Int) = fetch()
        override suspend fun fetchChairs() = fetch()
        override suspend fun fetchTeachers(chairId: Long) = fetch()
        override suspend fun fetchStudentInstitutes() = fetch()
        override suspend fun fetchStudentCourses(facultyId: Long) = fetch().map { it.id.toInt() }
        override suspend fun fetchStudentGroups(facultyId: Long, course: Int) = fetch()
        override suspend fun fetchStudents(facultyId: Long, course: Int, groupId: Long) = fetch()
    }
}
