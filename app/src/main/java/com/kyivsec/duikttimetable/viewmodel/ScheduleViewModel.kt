package com.kyivsec.duikttimetable.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kyivsec.duikttimetable.data.DataError
import com.kyivsec.duikttimetable.data.DateRange
import com.kyivsec.duikttimetable.data.GroupDirectoryRepository
import com.kyivsec.duikttimetable.data.ScheduleRepository
import com.kyivsec.duikttimetable.data.SettingsRepository
import com.kyivsec.duikttimetable.data.SyncResult
import com.kyivsec.duikttimetable.model.GroupInfo
import com.kyivsec.duikttimetable.model.Lesson
import com.kyivsec.duikttimetable.model.ScheduleDay
import com.kyivsec.duikttimetable.model.ScheduleMode
import com.kyivsec.duikttimetable.model.ScheduleSettings
import com.kyivsec.duikttimetable.model.ScheduleUiState
import com.kyivsec.duikttimetable.model.ScheduleWeek
import com.kyivsec.duikttimetable.model.ThemeMode
import com.kyivsec.duikttimetable.model.UiMessage
import com.kyivsec.duikttimetable.R
import com.kyivsec.duikttimetable.util.weekStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.WeekFields
import kotlin.math.abs

class ScheduleViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val groupDirectoryRepository: GroupDirectoryRepository,
    private val preferencesRepository: SettingsRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val computationDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val initialDate = today()
    private val _uiState = MutableStateFlow(ScheduleUiState(
        selectedDate = initialDate,
        selectedWeek = weekStart(initialDate, DayOfWeek.MONDAY),
    ))
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()
    private val _currentTime = MutableStateFlow(now())
    val currentTime: StateFlow<LocalDateTime> = _currentTime.asStateFlow()
    private val _selectedLesson = MutableStateFlow<Lesson?>(null)
    val selectedLesson: StateFlow<Lesson?> = _selectedLesson.asStateFlow()

    private var allDays: List<ScheduleDay> = emptyList()
    private var scheduleJob: Job? = null
    private var automaticSyncJob: Job? = null
    private var loadJob: Job? = null
    private var institutesJob: Job? = null
    private var coursesJob: Job? = null
    private var groupsJob: Job? = null

    init {
        load()
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                delay(30_000)
                _currentTime.value = now()
            }
        }
    }

    fun retry() = load()

    fun selectMode(mode: ScheduleMode) {
        if (_uiState.value.selectedMode == mode) return
        _uiState.update { state ->
            if (mode == ScheduleMode.WEEK) {
                state.copy(selectedMode = mode, selectedWeek = weekStart(state.selectedDate, DayOfWeek.MONDAY))
            } else {
                val end = state.selectedWeek.plusDays(6)
                val target = when {
                    state.selectedDate in state.selectedWeek..end -> state.selectedDate
                    today() in state.selectedWeek..end -> today()
                    else -> state.selectedWeek
                }
                state.copy(selectedMode = mode, selectedDate = closestAvailableDate(target, state.availableDates))
            }
        }
    }

    fun selectDate(date: LocalDate) {
        val state = _uiState.value
        if (date !in state.availableDates || date == state.selectedDate) return
        _uiState.update { it.copy(selectedDate = date, selectedWeek = weekStart(date, DayOfWeek.MONDAY)) }
    }

    fun selectWeek(start: LocalDate) {
        val state = _uiState.value
        if (start == state.selectedWeek || state.weeks.none { it.startDate == start }) return
        _uiState.update { it.copy(selectedWeek = start) }
    }

    fun goToToday() {
        _uiState.update { state -> state.copy(
            selectedDate = closestAvailableDate(today(), state.availableDates),
            selectedWeek = weekStart(today(), DayOfWeek.MONDAY),
        ) }
    }

    fun toggleExpanded(date: LocalDate) = _uiState.update { state ->
        val values = state.expandedDates.toMutableSet()
        if (!values.add(date)) values.remove(date)
        state.copy(expandedDates = values)
    }
    fun selectLesson(lesson: Lesson?) { _selectedLesson.value = lesson }

    fun startGroupSelection() {
        val selected = _uiState.value.selectedGroup
        if (selected == null) {
            _uiState.update { it.copy(
                draftInstituteId = null,
                draftCourse = null,
                draftGroup = null,
                directoryCourses = emptyList(),
                directoryGroups = emptyList(),
            ) }
            return
        }
        val instituteId = selected.instituteId
        val course = selected.course
        _uiState.update { it.copy(draftInstituteId = instituteId, draftCourse = course, draftGroup = selected) }
        loadDirectoryBranch(instituteId, course)
    }

    fun selectDraftInstitute(instituteId: Long) {
        if (_uiState.value.draftInstituteId == instituteId) return
        _uiState.update { it.copy(draftInstituteId = instituteId, draftCourse = null, draftGroup = null, directoryCourses = emptyList(), directoryGroups = emptyList()) }
        loadCourses(instituteId)
    }

    fun selectDraftCourse(course: Int) {
        val instituteId = _uiState.value.draftInstituteId ?: return
        if (_uiState.value.draftCourse == course) return
        _uiState.update { it.copy(draftCourse = course, draftGroup = null, directoryGroups = emptyList()) }
        loadGroups(instituteId, course)
    }

    fun selectDraftGroup(group: GroupInfo) {
        if (_uiState.value.draftGroup?.id != group.id) _uiState.update { it.copy(draftGroup = group) }
    }

    fun applyGroup(group: GroupInfo) {
        if (_uiState.value.selectedGroup?.id == group.id) return
        allDays = emptyList()
        _uiState.update { it.copy(
            selectedGroup = group,
            draftGroup = group,
            needsGroupSelection = false,
            expandedDates = emptySet(),
            expandedDatesInitialized = false,
        ) }
        viewModelScope.launch { preferencesRepository.saveSelectedGroup(group) }
        observeSchedule(group)
        syncSelectedGroup(group)
    }

    fun updateTheme(value: ThemeMode) = updateSettings { it.copy(themeMode = value) }
    fun updateStartupMode(value: ScheduleMode) = updateSettings { it.copy(startupMode = value) }
    fun updateHideClassesInWeekView(value: Boolean) = updateSettings { it.copy(hideClassesInWeekView = value) }
    fun updatePreviousDays(value: Int) = updateSettings { it.copy(previousDaysToKeep = value.coerceIn(0, 30)) }
    fun updatePreviousWeeks(value: Int) = updateSettings { it.copy(previousWeeksToKeep = value.coerceIn(0, 12)) }

    fun refreshVisible() {
        val snapshot = _uiState.value
        val group = snapshot.selectedGroup ?: return
        if (snapshot.isRefreshing || snapshot.isFullReloading) return
        val range = if (snapshot.selectedMode == ScheduleMode.DAY) DateRange(snapshot.selectedDate, snapshot.selectedDate)
        else DateRange(snapshot.selectedWeek, snapshot.selectedWeek.plusDays(6))
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            handleSyncResult(scheduleRepository.syncSchedule(group, range, force = true))
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun fullReload() {
        val snapshot = _uiState.value
        val group = snapshot.selectedGroup ?: return
        if (snapshot.isRefreshing || snapshot.isFullReloading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isFullReloading = true) }
            groupDirectoryRepository.ensureInstitutes(force = true)
            groupDirectoryRepository.ensureCourses(group.instituteId, force = true)
            groupDirectoryRepository.ensureGroups(group.instituteId, group.course, force = true)
            when (val result = scheduleRepository.syncCurrentSemester(group)) {
                is SyncResult.Success -> {
                    preferencesRepository.saveLastFullRefresh(result.completedAt.toEpochMilli())
                    _uiState.update { it.copy(lastFullRefreshEpochMillis = result.completedAt.toEpochMilli()) }
                    pruneHistory()
                }
                is SyncResult.Failure -> showError(result.error)
            }
            _uiState.update { it.copy(isFullReloading = false) }
        }
    }

    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val preferences = runCatching { preferencesRepository.load() }.getOrElse {
                _uiState.update { state -> state.copy(isLoading = false, errorMessage = UiMessage(R.string.error_read_settings)) }
                return@launch
            }
            _uiState.update { it.copy(
                settings = preferences.settings,
                selectedMode = preferences.settings.startupMode,
                weekSectionsStartExpanded = !preferences.settings.hideClassesInWeekView,
                lastFullRefreshEpochMillis = preferences.lastFullRefreshEpochMillis,
            ) }
            observeInstitutes()
            groupDirectoryRepository.ensureInstitutes()
            val instituteId = preferences.selectedInstituteId
            val course = preferences.selectedCourse
            if (preferences.selectedGroupId == null || instituteId == null || course == null) {
                _uiState.update { it.copy(
                    selectedGroup = null,
                    draftInstituteId = null,
                    draftCourse = null,
                    draftGroup = null,
                    isLoading = false,
                    needsGroupSelection = true,
                ) }
                return@launch
            }
            groupDirectoryRepository.ensureCourses(instituteId)
            groupDirectoryRepository.ensureGroups(instituteId, course)
            val groups = groupDirectoryRepository.observeGroups(instituteId, course).first()
            val selected = groups.firstOrNull { it.id == preferences.selectedGroupId }
                ?: groups.firstOrNull { it.name == preferences.selectedGroupName }
            if (selected == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = UiMessage(R.string.error_load_groups)) }
                return@launch
            }
            preferencesRepository.saveSelectedGroup(selected)
            _uiState.update { it.copy(selectedGroup = selected, draftInstituteId = selected.instituteId, draftCourse = selected.course, draftGroup = selected, isLoading = false) }
            loadDirectoryBranch(selected.instituteId, selected.course, ensure = false)
            rebuildWindows()
            observeSchedule(selected)
            syncSelectedGroup(selected, initial = true)
        }
    }

    private fun observeInstitutes() {
        institutesJob?.cancel()
        institutesJob = viewModelScope.launch {
            groupDirectoryRepository.observeInstitutes().collect { values -> _uiState.update { it.copy(institutes = values) } }
        }
    }

    private fun loadDirectoryBranch(instituteId: Long, course: Int, ensure: Boolean = true) {
        loadCourses(instituteId, ensure = ensure)
        loadGroups(instituteId, course, ensure = ensure)
    }

    private fun loadCourses(instituteId: Long, ensure: Boolean = true) {
        coursesJob?.cancel()
        coursesJob = viewModelScope.launch {
            _uiState.update { it.copy(isCoursesLoading = true) }
            if (ensure) groupDirectoryRepository.ensureCourses(instituteId)
            groupDirectoryRepository.observeCourses(instituteId).collect { values ->
                _uiState.update { it.copy(directoryCourses = values, isCoursesLoading = false) }
            }
        }
    }

    private fun loadGroups(instituteId: Long, course: Int, ensure: Boolean = true) {
        groupsJob?.cancel()
        groupsJob = viewModelScope.launch {
            _uiState.update { it.copy(isGroupsLoading = true) }
            if (ensure) groupDirectoryRepository.ensureGroups(instituteId, course)
            groupDirectoryRepository.observeGroups(instituteId, course).collect { values ->
                _uiState.update { it.copy(directoryGroups = values, isGroupsLoading = false) }
            }
        }
    }

    private fun observeSchedule(group: GroupInfo) {
        scheduleJob?.cancel()
        scheduleJob = viewModelScope.launch {
            scheduleRepository.observeSchedule(group.id, displayRange()).collect { days ->
                allDays = days
                rebuildWindowsOffMain()
            }
        }
    }

    private fun displayRange(): DateRange {
        val state = _uiState.value
        val currentWeek = weekStart(today(), DayOfWeek.MONDAY)
        return DateRange(
            currentWeek.minusWeeks(state.settings.previousWeeksToKeep.toLong()),
            currentWeek.plusWeeks(FUTURE_WEEKS).plusDays(6),
        )
    }

    private fun syncSelectedGroup(group: GroupInfo, initial: Boolean = false) {
        automaticSyncJob?.cancel()
        automaticSyncJob = viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            handleSyncResult(scheduleRepository.syncCurrentSemester(group), initial)
            _uiState.update { state ->
                if (state.selectedGroup?.id == group.id) state.copy(isRefreshing = false) else state
            }
        }
    }

    private suspend fun rebuildWindowsOffMain() {
        val snapshot = _uiState.value
        val days = allDays
        val result = withContext(computationDispatcher) { buildWindows(snapshot, days) }
        _uiState.update { current ->
            if (current.settings != snapshot.settings || current.selectedGroup != snapshot.selectedGroup) current
            else current.copy(
                weeks = reuseEqualItems(current.weeks, result.weeks),
                availableDates = if (current.availableDates == result.dates) current.availableDates else result.dates,
                selectedDate = result.selectedDate,
                selectedWeek = result.selectedWeek,
                expandedDates = result.expandedDates,
                expandedDatesInitialized = result.expandedDatesInitialized,
            )
        }
    }

    private fun rebuildWindows() {
        val state = _uiState.value
        val result = buildWindows(state, allDays)
        _uiState.update { it.copy(
            weeks = reuseEqualItems(it.weeks, result.weeks),
            availableDates = if (it.availableDates == result.dates) it.availableDates else result.dates,
            selectedDate = result.selectedDate,
            selectedWeek = result.selectedWeek,
            expandedDates = result.expandedDates,
            expandedDatesInitialized = result.expandedDatesInitialized,
        ) }
    }

    private fun buildWindows(state: ScheduleUiState, days: List<ScheduleDay>): WindowResult {
        val firstDay = DayOfWeek.MONDAY
        val currentWeek = weekStart(today(), firstDay)
        val earliest = currentWeek.minusWeeks(state.settings.previousWeeksToKeep.toLong())
        val latest = currentWeek.plusWeeks(FUTURE_WEEKS)
        val dayMap = days.associateBy { it.date }
        val weeks = generateSequence(earliest) { it.plusWeeks(1).takeIf { next -> !next.isAfter(latest) } }.map { start ->
            ScheduleWeek(start.get(WeekFields.ISO.weekOfWeekBasedYear()), start, start.plusDays(6),
                (0L..6L).map { offset -> dayMap[start.plusDays(offset)] ?: ScheduleDay(start.plusDays(offset), emptyList()) })
        }.toList()
        val earliestDate = maxOf(earliest, today().minusDays(state.settings.previousDaysToKeep.toLong()))
        val dates = generateSequence(earliestDate) { it.plusDays(1).takeIf { next -> !next.isAfter(latest.plusDays(6)) } }.toList()
        val selectedDate = closestAvailableDate(state.selectedDate, dates)
        val requestedWeek = weekStart(state.selectedWeek, firstDay)
        val selectedWeek = weeks.firstOrNull { it.startDate == requestedWeek }?.startDate
            ?: weekStart(selectedDate, firstDay)
        val populatedDates = weeks.flatMap { it.days }
            .filter { it.lessons.isNotEmpty() }
            .map { it.date }
            .toSet()
        val expanded = if (!state.expandedDatesInitialized) {
            if (state.weekSectionsStartExpanded) populatedDates else emptySet()
        } else {
            state.expandedDates.intersect(populatedDates)
        }
        return WindowResult(
            weeks, dates, selectedDate, selectedWeek, expanded,
            expandedDatesInitialized = state.expandedDatesInitialized || populatedDates.isNotEmpty(),
        )
    }

    private fun updateSettings(transform: (ScheduleSettings) -> ScheduleSettings) {
        val previous = _uiState.value.settings
        val settings = transform(previous)
        if (settings == previous) return
        _uiState.update { it.copy(settings = settings) }
        rebuildWindows()
        if (settings.previousWeeksToKeep != previous.previousWeeksToKeep) {
            _uiState.value.selectedGroup?.let(::observeSchedule)
        }
        viewModelScope.launch { preferencesRepository.saveSettings(settings) }
    }

    private suspend fun pruneHistory() {
        val settings = _uiState.value.settings
        val weekBoundary = weekStart(today(), DayOfWeek.MONDAY).minusWeeks(settings.previousWeeksToKeep.toLong())
        val dayBoundary = today().minusDays(settings.previousDaysToKeep.toLong())
        scheduleRepository.pruneBefore(minOf(weekBoundary, dayBoundary))
    }

    private fun handleSyncResult(result: SyncResult, initial: Boolean = false) {
        if (result is SyncResult.Failure && (!initial || !result.hasCachedData)) showError(result.error)
    }
    private fun showError(error: DataError) = _uiState.update { it.copy(errorMessage = when (error) {
        DataError.Offline -> UiMessage(R.string.error_offline)
        DataError.Timeout -> UiMessage(R.string.error_timeout)
        is DataError.Http -> UiMessage(R.string.error_http, error.code)
        DataError.RejectedSession -> UiMessage(R.string.error_rejected_session)
        is DataError.SourceFormat -> UiMessage(R.string.error_source_format)
        is DataError.Database -> UiMessage(R.string.error_database)
    }) }

    private fun closestAvailableDate(date: LocalDate, values: List<LocalDate>): LocalDate =
        values.minByOrNull { abs(it.toEpochDay() - date.toEpochDay()) } ?: date
    private fun now(): LocalDateTime = LocalDateTime.ofInstant(clock.instant(), clock.zone)
    private fun today(): LocalDate = LocalDate.now(clock)

    private fun <T> reuseEqualItems(previous: List<T>, next: List<T>): List<T> {
        if (previous == next) return previous
        val old = previous.associateBy { it }
        return next.map { old[it] ?: it }
    }

    private data class WindowResult(
        val weeks: List<ScheduleWeek>,
        val dates: List<LocalDate>,
        val selectedDate: LocalDate,
        val selectedWeek: LocalDate,
        val expandedDates: Set<LocalDate>,
        val expandedDatesInitialized: Boolean,
    )

    class Factory(
        private val scheduleRepository: ScheduleRepository,
        private val groupDirectoryRepository: GroupDirectoryRepository,
        private val preferencesRepository: SettingsRepository,
        private val clock: Clock = Clock.systemDefaultZone(),
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ScheduleViewModel(scheduleRepository, groupDirectoryRepository, preferencesRepository, clock) as T
    }

    private companion object {
        const val FUTURE_WEEKS = 12L
    }
}
