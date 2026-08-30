package com.kyivsec.duikttimetable

import com.kyivsec.duikttimetable.data.ScheduleRepository
import com.kyivsec.duikttimetable.data.GroupDirectoryRepository
import com.kyivsec.duikttimetable.data.DateRange
import com.kyivsec.duikttimetable.data.SyncResult
import com.kyivsec.duikttimetable.data.SettingsRepository
import com.kyivsec.duikttimetable.data.StoredPreferences
import com.kyivsec.duikttimetable.model.CourseGroups
import com.kyivsec.duikttimetable.model.GroupInfo
import com.kyivsec.duikttimetable.model.Institute
import com.kyivsec.duikttimetable.model.Lesson
import com.kyivsec.duikttimetable.model.LessonType
import com.kyivsec.duikttimetable.model.ScheduleDay
import com.kyivsec.duikttimetable.model.ScheduleMode
import com.kyivsec.duikttimetable.model.ScheduleSettings
import com.kyivsec.duikttimetable.model.ScheduleWeek
import com.kyivsec.duikttimetable.viewmodel.ScheduleViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
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

private class FakeScheduleRepository : ScheduleRepository, GroupDirectoryRepository {
    var refreshedDay: LocalDate? = null
    var refreshedWeek: LocalDate? = null
    var semesterRefreshes: Int = 0
    private val start = LocalDate.of(2026, 8, 24)
    private val lesson = Lesson("1", "Програмування", LessonType.LAB, LocalTime.of(9, 35), LocalTime.of(10, 55))
    private val week = ScheduleWeek(35, start, start.plusDays(6), (0L..6).map { ScheduleDay(start.plusDays(it), if (it == 4L) listOf(lesson) else emptyList()) })
    private val group = GroupInfo(1001, "ПД-31", 1, "ННІ ІТ", 3)
    override fun observeSchedule(groupId: Long, range: DateRange): Flow<List<ScheduleDay>> = flowOf(week.days)
    override fun observeCachedDates(groupId: Long, range: DateRange): Flow<Set<LocalDate>> = flowOf(week.days.map { it.date }.toSet())
    override suspend fun syncSchedule(group: GroupInfo, range: DateRange, force: Boolean): SyncResult {
        if (force && range.startInclusive == range.endInclusive) refreshedDay = range.startInclusive
        if (force && range.endInclusive == range.startInclusive.plusDays(6)) refreshedWeek = range.startInclusive
        return SyncResult.Success(Instant.parse("2026-08-28T10:00:00Z"), 0)
    }
    override suspend fun syncCurrentSemester(group: GroupInfo): SyncResult {
        semesterRefreshes++
        return SyncResult.Success(Instant.parse("2026-08-28T10:00:00Z"), 0)
    }
    override suspend fun pruneBefore(date: LocalDate) = Unit
    override fun observeInstitutes(): Flow<List<Institute>> = flowOf(listOf(Institute(1, "ННІ ІТ", emptyList())))
    override fun observeCourses(instituteId: Long): Flow<List<Int>> = flowOf(listOf(3))
    override fun observeGroups(instituteId: Long, course: Int): Flow<List<GroupInfo>> = flowOf(listOf(group))
    override suspend fun ensureInstitutes(force: Boolean) = SyncResult.Success(Instant.parse("2026-08-28T10:00:00Z"), 0)
    override suspend fun ensureCourses(instituteId: Long, force: Boolean) = SyncResult.Success(Instant.parse("2026-08-28T10:00:00Z"), 0)
    override suspend fun ensureGroups(instituteId: Long, course: Int, force: Boolean) = SyncResult.Success(Instant.parse("2026-08-28T10:00:00Z"), 0)
}
