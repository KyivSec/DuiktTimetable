package com.kyivsec.duikttimetable

import com.kyivsec.duikttimetable.data.ScheduleRepository
import com.kyivsec.duikttimetable.data.GroupDirectoryRepository
import com.kyivsec.duikttimetable.data.DateRange
import com.kyivsec.duikttimetable.data.SyncResult
import com.kyivsec.duikttimetable.data.SettingsRepository
import com.kyivsec.duikttimetable.data.StoredPreferences
import com.kyivsec.duikttimetable.data.TeacherDirectoryRepository
import com.kyivsec.duikttimetable.data.StudentDirectoryRepository
import com.kyivsec.duikttimetable.model.CourseGroups
import com.kyivsec.duikttimetable.model.GroupInfo
import com.kyivsec.duikttimetable.model.Institute
import com.kyivsec.duikttimetable.model.Lesson
import com.kyivsec.duikttimetable.model.LessonType
import com.kyivsec.duikttimetable.model.ScheduleDay
import com.kyivsec.duikttimetable.model.ScheduleMode
import com.kyivsec.duikttimetable.model.ScheduleSettings
import com.kyivsec.duikttimetable.model.ScheduleWeek
import com.kyivsec.duikttimetable.model.TimetableOwner
import com.kyivsec.duikttimetable.model.ChairInfo
import com.kyivsec.duikttimetable.model.Occupation
import com.kyivsec.duikttimetable.model.TeacherInfo
import com.kyivsec.duikttimetable.model.StudentInfo
import com.kyivsec.duikttimetable.viewmodel.ScheduleViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val clock = Clock.fixed(Instant.parse("2026-08-28T10:00:00Z"), ZoneOffset.UTC)

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun `initial state selects today and preferred group`() = runTest(dispatcher) {
        val repository = FakeScheduleRepository()
        val viewModel = ScheduleViewModel(repository, repository, FakeSettingsRepository(), clock, dispatcher)
        runCurrent()
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(LocalDate.of(2026, 8, 28), viewModel.uiState.value.selectedDate)
        assertEquals("ПД-31", viewModel.uiState.value.selectedGroup?.name)
        viewModel.viewModelScope.cancel()
    }

    @Test fun `all saved owner types show cached lessons while directories are unavailable`() = runTest(dispatcher) {
        for (occupation in Occupation.entries) {
            val gate = CompletableDeferred<SyncResult>()
            val repository = FakeScheduleRepository()
            val groups = object : GroupDirectoryRepository by repository {
                override suspend fun ensureInstitutes(force: Boolean) = gate.await()
            }
            val teachers = object : TeacherDirectoryRepository by repository {
                override suspend fun ensureChairs(force: Boolean) = gate.await()
            }
            val students = object : StudentDirectoryRepository by repository {
                override suspend fun ensureStudentInstitutes(force: Boolean) = gate.await()
            }
            val preferences = FakeSettingsRepository(StoredPreferences(
                occupation = occupation,
                selectedGroupId = 1001, selectedInstituteId = 1,
                selectedCourse = 3, selectedGroupName = "ПД-31",
                selectedTeacherId = 7, selectedChairId = 42,
                selectedTeacherName = "Іваненко Іван Іванович",
                selectedStudentId = 9, selectedStudentName = "Петренко Петро Петрович",
                selectedStudentGroupId = 1001, selectedStudentGroupName = "ПД-31",
                selectedStudentInstituteId = 1, selectedStudentCourse = 3,
            ))
            val viewModel = ScheduleViewModel(repository, groups, preferences, clock, dispatcher, teachers, students)
            runCurrent()

            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(occupation, viewModel.uiState.value.activeOwner?.type)
            assertTrue(viewModel.uiState.value.weeks.flatMap { it.days }.any { it.lessons.isNotEmpty() })
            assertEquals(1, repository.semesterRefreshes)

            gate.complete(SyncResult.Failure(com.kyivsec.duikttimetable.data.DataError.Offline, true))
            runCurrent()
            assertEquals(occupation, viewModel.uiState.value.activeOwner?.type)
            assertTrue(viewModel.uiState.value.weeks.flatMap { it.days }.any { it.lessons.isNotEmpty() })
            viewModel.viewModelScope.cancel()
        }
    }

    @Test fun `first launch requires an explicit empty group selection`() = runTest(dispatcher) {
        val repository = FakeScheduleRepository()
        val viewModel = ScheduleViewModel(repository, repository, FakeSettingsRepository(StoredPreferences()), clock, dispatcher)
        runCurrent()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.needsGroupSelection)
        assertNull(viewModel.uiState.value.selectedGroup)
        assertNull(viewModel.uiState.value.draftInstituteId)
        assertNull(viewModel.uiState.value.draftCourse)
        assertNull(viewModel.uiState.value.draftGroup)
        assertEquals(0, repository.semesterRefreshes)
        viewModel.viewModelScope.cancel()
    }

    @Test fun `retry restores the current owner without leaving loading active`() = runTest(dispatcher) {
        val repository = FakeScheduleRepository()
        val viewModel = ScheduleViewModel(repository, repository, FakeSettingsRepository(), clock, dispatcher)
        runCurrent()
        viewModel.retry()
        runCurrent()
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(2, repository.semesterRefreshes)
        assertTrue(viewModel.uiState.value.weeks.flatMap { it.days }.any { it.lessons.isNotEmpty() })
        viewModel.viewModelScope.cancel()
    }

    @Test fun `dismissing incomplete selection releases required selection state`() = runTest(dispatcher) {
        val repository = FakeScheduleRepository()
        val viewModel = ScheduleViewModel(repository, repository, FakeSettingsRepository(StoredPreferences()), clock, dispatcher)
        runCurrent()

        assertTrue(viewModel.uiState.value.needsOccupationSelection)
        assertTrue(viewModel.uiState.value.needsGroupSelection)
        viewModel.dismissSelection()

        assertFalse(viewModel.uiState.value.needsOccupationSelection)
        assertFalse(viewModel.uiState.value.needsGroupSelection)
        assertFalse(viewModel.uiState.value.needsTeacherSelection)
        assertFalse(viewModel.uiState.value.needsStudentSelection)
        assertNull(viewModel.uiState.value.activeOwner)
        viewModel.viewModelScope.cancel()
    }

    @Test fun `preferred startup mode is applied without changing the current mode setting`() = runTest(dispatcher) {
        val repository = FakeScheduleRepository()
        val preferences = FakeSettingsRepository(StoredPreferences(
            settings = ScheduleSettings(startupMode = ScheduleMode.WEEK),
            selectedGroupId = 1001,
            selectedInstituteId = 1,
            selectedCourse = 3,
            selectedGroupName = "ПД-31",
        ))
        val viewModel = ScheduleViewModel(repository, repository, preferences, clock, dispatcher)
        runCurrent()

        assertEquals(ScheduleMode.WEEK, viewModel.uiState.value.selectedMode)
        viewModel.selectMode(ScheduleMode.DAY)
        assertEquals(ScheduleMode.DAY, viewModel.uiState.value.selectedMode)
        assertEquals(ScheduleMode.WEEK, preferences.stored.settings.startupMode)
        viewModel.viewModelScope.cancel()
    }

    @Test fun `visible refresh uses active mode scope`() = runTest(dispatcher) {
        val repository = FakeScheduleRepository()
        val viewModel = ScheduleViewModel(repository, repository, FakeSettingsRepository(), clock, dispatcher)
        runCurrent()
        viewModel.refreshVisible()
        runCurrent()
        assertEquals(LocalDate.of(2026, 8, 28), repository.refreshedDay)
        viewModel.selectMode(ScheduleMode.WEEK)
        viewModel.refreshVisible()
        runCurrent()
        assertEquals(LocalDate.of(2026, 8, 24), repository.refreshedWeek)
        viewModel.viewModelScope.cancel()
    }

    @Test fun `startup syncs once and swiping only changes cached selection`() = runTest(dispatcher) {
        val repository = FakeScheduleRepository()
        val viewModel = ScheduleViewModel(repository, repository, FakeSettingsRepository(), clock, dispatcher)
        runCurrent()
        viewModel.selectMode(ScheduleMode.WEEK)
        val nextWeek = LocalDate.of(2026, 8, 31)

        viewModel.selectWeek(nextWeek)
        runCurrent()

        assertEquals(nextWeek, viewModel.uiState.value.selectedWeek)
        assertEquals(1, repository.semesterRefreshes)
        assertFalse(repository.lastSemesterForce)
        viewModel.viewModelScope.cancel()
    }

    @Test fun `fast update refreshes every available day on launch`() = runTest(dispatcher) {
        val repository = FakeScheduleRepository()
        val preferences = FakeSettingsRepository(StoredPreferences(
            settings = ScheduleSettings(fastUpdate = true, previousDaysToKeep = 10),
            selectedGroupId = 1001,
            selectedInstituteId = 1,
            selectedCourse = 3,
            selectedGroupName = "ПД-31",
        ))

        val viewModel = ScheduleViewModel(repository, repository, preferences, clock, dispatcher)
        runCurrent()

        assertEquals(0, repository.semesterRefreshes)
        assertEquals(DateRange(LocalDate.of(2026, 8, 18), LocalDate.of(2026, 11, 22)), repository.lastRefreshedRange)
        assertTrue(repository.lastScheduleForce)
        viewModel.viewModelScope.cancel()
    }

    @Test fun `fast update refreshes every available week from toolbar`() = runTest(dispatcher) {
        val repository = FakeScheduleRepository()
        val preferences = FakeSettingsRepository(StoredPreferences(
            settings = ScheduleSettings(fastUpdate = true, startupMode = ScheduleMode.WEEK, previousWeeksToKeep = 3),
            selectedGroupId = 1001,
            selectedInstituteId = 1,
            selectedCourse = 3,
            selectedGroupName = "ПД-31",
        ))
        val viewModel = ScheduleViewModel(repository, repository, preferences, clock, dispatcher)
        runCurrent()
        repository.lastRefreshedRange = null

        viewModel.refreshVisible()
        runCurrent()

        assertEquals(DateRange(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 11, 22)), repository.lastRefreshedRange)
        viewModel.viewModelScope.cancel()
    }

    @Test fun `fast update runs full semester for an uncached owner`() = runTest(dispatcher) {
        val repository = FakeScheduleRepository(hasCachedSchedule = false)
        val preferences = FakeSettingsRepository(StoredPreferences(
            settings = ScheduleSettings(fastUpdate = true),
            selectedGroupId = 1001,
            selectedInstituteId = 1,
            selectedCourse = 3,
            selectedGroupName = "ПД-31",
        ))

        val viewModel = ScheduleViewModel(repository, repository, preferences, clock, dispatcher)
        runCurrent()

        assertEquals(1, repository.semesterRefreshes)
        assertTrue(repository.lastSemesterForce)
        assertNull(repository.lastRefreshedRange)
        viewModel.viewModelScope.cancel()
    }

    @Test fun `fast update is persisted and defaults to disabled`() = runTest(dispatcher) {
        val repository = FakeScheduleRepository()
        val preferences = FakeSettingsRepository()
        val viewModel = ScheduleViewModel(repository, repository, preferences, clock, dispatcher)
        runCurrent()

        assertFalse(viewModel.uiState.value.settings.fastUpdate)
        viewModel.updateFastUpdate(true)
        runCurrent()

        assertTrue(preferences.stored.settings.fastUpdate)
        viewModel.viewModelScope.cancel()
    }

    @Test fun `full reload bypasses semester freshness`() = runTest(dispatcher) {
        val repository = FakeScheduleRepository()
        val viewModel = ScheduleViewModel(repository, repository, FakeSettingsRepository(), clock, dispatcher)
        runCurrent()

        viewModel.fullReload()
        runCurrent()

        assertTrue(repository.lastSemesterForce)
        viewModel.viewModelScope.cancel()
    }

    @Test fun `day and week selections remain independent`() = runTest(dispatcher) {
        val repository = FakeScheduleRepository()
        val viewModel = ScheduleViewModel(repository, repository, FakeSettingsRepository(), clock, dispatcher)
        runCurrent()

        val initialDate = viewModel.uiState.value.selectedDate
        val nextWeek = initialDate.plusWeeks(1).let { com.kyivsec.duikttimetable.util.weekStart(it, java.time.DayOfWeek.MONDAY) }
        viewModel.selectWeek(nextWeek)
        assertEquals(initialDate, viewModel.uiState.value.selectedDate)

        val nextDate = initialDate.plusDays(1)
        viewModel.selectDate(nextDate)
        assertEquals(nextDate, viewModel.uiState.value.selectedDate)
        assertEquals(nextWeek, viewModel.uiState.value.selectedWeek)
        viewModel.viewModelScope.cancel()
    }

    @Test fun `obsolete manual and full refreshes cannot publish errors or clear a new refresh`() = runTest(dispatcher) {
        for (fullReload in listOf(false, true)) {
            val base = FakeScheduleRepository()
            val oldRequest = CompletableDeferred<SyncResult>()
            val newRequest = CompletableDeferred<SyncResult>()
            var delayOldRequest = false
            val repository = object : ScheduleRepository by base {
                override suspend fun syncSchedule(owner: TimetableOwner, range: DateRange, force: Boolean): SyncResult =
                    withContext(NonCancellable) { oldRequest.await() }
                override suspend fun syncCurrentSemester(owner: TimetableOwner, force: Boolean): SyncResult =
                    when {
                        owner.id == 2002L -> newRequest.await()
                        delayOldRequest -> withContext(NonCancellable) { oldRequest.await() }
                        else -> base.syncCurrentSemester(owner, force)
                    }
            }
            val viewModel = ScheduleViewModel(repository, base, FakeSettingsRepository(), clock, dispatcher)
            runCurrent()
            delayOldRequest = true
            if (fullReload) viewModel.fullReload() else viewModel.refreshVisible()
            runCurrent()
            viewModel.applyGroup(GroupInfo(2002, "ПД-32", 1, "ІТ", 3))
            runCurrent()
            assertTrue(viewModel.uiState.value.isRefreshing)

            oldRequest.complete(SyncResult.Failure(com.kyivsec.duikttimetable.data.DataError.Offline, true))
            runCurrent()
            assertEquals(2002L, viewModel.uiState.value.activeOwner?.id)
            assertNull(viewModel.uiState.value.errorMessage)
            assertTrue(viewModel.uiState.value.isRefreshing)
            assertFalse(viewModel.uiState.value.isFullReloading)
            newRequest.complete(SyncResult.Success(clock.instant(), 0))
            runCurrent()
            assertFalse(viewModel.uiState.value.isRefreshing)
            viewModel.viewModelScope.cancel()
        }
    }

    @Test fun `schedule calculation preserves navigation and expansion made while it was pending`() = runTest(dispatcher) {
        val base = FakeScheduleRepository()
        val date = LocalDate.of(2026, 8, 28)
        val lesson = Lesson("1", "Algorithms", LessonType.LAB, LocalTime.of(9, 30), LocalTime.of(10, 50))
        val days = MutableStateFlow(listOf(ScheduleDay(date, listOf(lesson))))
        val repository = object : ScheduleRepository by base {
            override fun observeSchedule(owner: TimetableOwner, range: DateRange) = days
        }
        val calculationScheduler = TestCoroutineScheduler()
        val viewModel = ScheduleViewModel(repository, base, FakeSettingsRepository(), clock, StandardTestDispatcher(calculationScheduler))
        runCurrent()
        calculationScheduler.runCurrent()
        runCurrent()
        assertTrue(date in viewModel.uiState.value.expandedDates)

        days.value = listOf(ScheduleDay(date, listOf(lesson.copy(subject = "Updated"))))
        runCurrent()
        val nextDate = date.plusDays(1)
        val nextWeek = viewModel.uiState.value.selectedWeek.plusWeeks(1)
        viewModel.selectDate(nextDate)
        viewModel.selectWeek(nextWeek)
        viewModel.toggleExpanded(date)
        calculationScheduler.runCurrent()
        runCurrent()

        assertEquals(nextDate, viewModel.uiState.value.selectedDate)
        assertEquals(nextWeek, viewModel.uiState.value.selectedWeek)
        assertFalse(date in viewModel.uiState.value.expandedDates)
        assertEquals("Updated", viewModel.uiState.value.weeks.flatMap { it.days }.first { it.date == date }.lessons.single().subject)
        viewModel.viewModelScope.cancel()
    }

    @Test fun `changing institute cancels the previous group observer`() = runTest(dispatcher) {
        val base = FakeScheduleRepository()
        val oldGroups = MutableStateFlow(listOf(GroupInfo(1001, "ПД-31", 1, "ІТ", 3)))
        val directory = object : GroupDirectoryRepository by base {
            override fun observeGroups(instituteId: Long, course: Int) = oldGroups
        }
        val viewModel = ScheduleViewModel(base, directory, FakeSettingsRepository(), clock, dispatcher)
        runCurrent()
        assertTrue(viewModel.uiState.value.directoryGroups.isNotEmpty())
        viewModel.selectDraftInstitute(2)
        oldGroups.value = listOf(GroupInfo(1002, "Old branch", 1, "ІТ", 3))
        runCurrent()
        assertTrue(viewModel.uiState.value.directoryGroups.isEmpty())
        assertNull(viewModel.uiState.value.draftCourse)
        assertFalse(viewModel.uiState.value.isGroupsLoading)
        viewModel.viewModelScope.cancel()
    }

    @Test fun `late preference load cannot replace an explicit owner selection`() = runTest(dispatcher) {
        val base = FakeScheduleRepository()
        val loaded = CompletableDeferred<StoredPreferences>()
        val preferences = object : SettingsRepository by FakeSettingsRepository() {
            override suspend fun load() = loaded.await()
        }
        val viewModel = ScheduleViewModel(base, base, preferences, clock, dispatcher)
        runCurrent()
        viewModel.applyGroup(GroupInfo(2002, "ПД-32", 1, "ІТ", 3))
        loaded.complete(StoredPreferences(selectedGroupId = 1001, selectedInstituteId = 1, selectedCourse = 3, selectedGroupName = "ПД-31"))
        runCurrent()
        assertEquals(2002L, viewModel.uiState.value.activeOwner?.id)
        assertFalse(viewModel.uiState.value.isLoading)
        viewModel.viewModelScope.cancel()
    }

    @Test fun `teacher startup and remembered group switch share the schedule pipeline`() = runTest(dispatcher) {
        val repository = FakeScheduleRepository()
        val preferences = FakeSettingsRepository(StoredPreferences(
            occupation = Occupation.TEACHER,
            selectedTeacherId = 7,
            selectedChairId = 42,
            selectedTeacherName = "Іваненко Іван Іванович",
            selectedChairName = "Кафедра ІТ",
            selectedGroupId = 1001,
            selectedInstituteId = 1,
            selectedCourse = 3,
            selectedGroupName = "ПД-31",
        ))
        val viewModel = ScheduleViewModel(repository, repository, preferences, clock, dispatcher, repository)
        runCurrent()

        assertEquals("Іваненко Іван Іванович", viewModel.uiState.value.activeOwner?.displayName)
        assertFalse(viewModel.selectOccupation(Occupation.GROUP))
        runCurrent()
        assertEquals("ПД-31", viewModel.uiState.value.activeOwner?.displayName)
        assertEquals(LocalDate.of(2026, 8, 28), viewModel.uiState.value.selectedDate)
        viewModel.viewModelScope.cancel()
    }

    @Test fun `individual student startup uses the full name as owner label`() = runTest(dispatcher) {
        val repository = FakeScheduleRepository()
        val preferences = FakeSettingsRepository(StoredPreferences(
            occupation = Occupation.STUDENT,
            selectedStudentId = 9,
            selectedStudentName = "Петренко Петро Петрович",
            selectedStudentGroupId = 1001,
            selectedStudentGroupName = "ПД-31",
            selectedStudentInstituteId = 1,
            selectedStudentInstituteName = "ННІ ІТ",
            selectedStudentCourse = 3,
        ))
        val viewModel = ScheduleViewModel(repository, repository, preferences, clock, dispatcher, repository, repository)
        runCurrent()

        assertEquals(Occupation.STUDENT, viewModel.uiState.value.activeOccupation)
        assertEquals("Петренко Петро Петрович", viewModel.uiState.value.activeOwner?.displayName)
        assertEquals(1, repository.semesterRefreshes)
        viewModel.viewModelScope.cancel()
    }
}

private class FakeSettingsRepository(
    initial: StoredPreferences = StoredPreferences(
        selectedGroupId = 1001,
        selectedInstituteId = 1,
        selectedCourse = 3,
        selectedGroupName = "ПД-31",
    ),
) : SettingsRepository {
    var stored = initial
    override suspend fun load() = stored
    override suspend fun saveSettings(settings: ScheduleSettings) { stored = stored.copy(settings = settings) }
    override suspend fun saveSelectedGroup(group: GroupInfo) { stored = stored.copy(selectedGroupId = group.id, selectedInstituteId = group.instituteId, selectedCourse = group.course, selectedGroupName = group.name) }
    override suspend fun saveLastFullRefresh(epochMillis: Long) { stored = stored.copy(lastFullRefreshEpochMillis = epochMillis) }
}

private class FakeScheduleRepository(
    private val hasCachedSchedule: Boolean = true,
) : ScheduleRepository, GroupDirectoryRepository, TeacherDirectoryRepository, StudentDirectoryRepository {
    var refreshedDay: LocalDate? = null
    var refreshedWeek: LocalDate? = null
    var lastRefreshedRange: DateRange? = null
    var lastScheduleForce: Boolean = false
    var semesterRefreshes: Int = 0
    var lastSemesterForce: Boolean = false
    private val start = LocalDate.of(2026, 8, 24)
    private val lesson = Lesson("1", "Програмування", LessonType.LAB, LocalTime.of(9, 35), LocalTime.of(10, 55))
    private val week = ScheduleWeek(35, start, start.plusDays(6), (0L..6).map { ScheduleDay(start.plusDays(it), if (it == 4L) listOf(lesson) else emptyList()) })
    private val group = GroupInfo(1001, "ПД-31", 1, "ННІ ІТ", 3)
    private val teacher = TeacherInfo(7, "Іваненко Іван Іванович", 42, "Кафедра ІТ")
    private val student = StudentInfo(9, "Петренко Петро Петрович", 1001, "ПД-31", 1, "ННІ ІТ", 3)
    override fun observeSchedule(owner: TimetableOwner, range: DateRange): Flow<List<ScheduleDay>> = flowOf(week.days)
    override fun observeCachedDates(owner: TimetableOwner, range: DateRange): Flow<Set<LocalDate>> =
        flowOf(if (hasCachedSchedule) week.days.map { it.date }.toSet() else emptySet())
    override suspend fun syncSchedule(owner: TimetableOwner, range: DateRange, force: Boolean): SyncResult {
        lastRefreshedRange = range
        lastScheduleForce = force
        if (force && range.startInclusive == range.endInclusive) refreshedDay = range.startInclusive
        if (force && range.endInclusive == range.startInclusive.plusDays(6)) refreshedWeek = range.startInclusive
        return SyncResult.Success(Instant.parse("2026-08-28T10:00:00Z"), 0)
    }
    override suspend fun syncCurrentSemester(owner: TimetableOwner, force: Boolean): SyncResult {
        semesterRefreshes++
        lastSemesterForce = force
        return SyncResult.Success(Instant.parse("2026-08-28T10:00:00Z"), 0)
    }
    override suspend fun pruneBefore(date: LocalDate) = Unit
    override fun observeInstitutes(): Flow<List<Institute>> = flowOf(listOf(Institute(1, "ННІ ІТ", emptyList())))
    override fun observeCourses(instituteId: Long): Flow<List<Int>> = flowOf(listOf(3))
    override fun observeGroups(instituteId: Long, course: Int): Flow<List<GroupInfo>> = flowOf(listOf(group))
    override suspend fun ensureInstitutes(force: Boolean) = SyncResult.Success(Instant.parse("2026-08-28T10:00:00Z"), 0)
    override suspend fun ensureCourses(instituteId: Long, force: Boolean) = SyncResult.Success(Instant.parse("2026-08-28T10:00:00Z"), 0)
    override suspend fun ensureGroups(instituteId: Long, course: Int, force: Boolean) = SyncResult.Success(Instant.parse("2026-08-28T10:00:00Z"), 0)
    override fun observeChairs(): Flow<List<ChairInfo>> = flowOf(listOf(ChairInfo(42, "Кафедра ІТ")))
    override fun observeTeachers(chairId: Long): Flow<List<TeacherInfo>> = flowOf(listOf(teacher))
    override suspend fun ensureChairs(force: Boolean) = SyncResult.Success(Instant.parse("2026-08-28T10:00:00Z"), 0)
    override suspend fun ensureTeachers(chairId: Long, force: Boolean) = SyncResult.Success(Instant.parse("2026-08-28T10:00:00Z"), 0)
    override fun observeStudents(groupId: Long): Flow<List<StudentInfo>> = flowOf(listOf(student))
    override fun observeStudentInstitutes() = observeInstitutes()
    override fun observeStudentCourses(instituteId: Long) = observeCourses(instituteId)
    override fun observeStudentGroups(instituteId: Long, course: Int) = observeGroups(instituteId, course)
    override suspend fun ensureStudentInstitutes(force: Boolean) = SyncResult.Success(Instant.parse("2026-08-28T10:00:00Z"), 0)
    override suspend fun ensureStudentCourses(instituteId: Long, force: Boolean) = SyncResult.Success(Instant.parse("2026-08-28T10:00:00Z"), 0)
    override suspend fun ensureStudentGroups(instituteId: Long, course: Int, force: Boolean) = SyncResult.Success(Instant.parse("2026-08-28T10:00:00Z"), 0)
    override suspend fun ensureStudents(group: GroupInfo, force: Boolean) = SyncResult.Success(Instant.parse("2026-08-28T10:00:00Z"), 0)
}
