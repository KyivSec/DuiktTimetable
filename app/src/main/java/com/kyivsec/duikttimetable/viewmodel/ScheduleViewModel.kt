package com.kyivsec.duikttimetable.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kyivsec.duikttimetable.data.DataError
import com.kyivsec.duikttimetable.data.DateRange
import com.kyivsec.duikttimetable.data.GroupDirectoryRepository
import com.kyivsec.duikttimetable.data.ScheduleRepository
import com.kyivsec.duikttimetable.data.SettingsRepository
import com.kyivsec.duikttimetable.data.TeacherDirectoryRepository
import com.kyivsec.duikttimetable.data.StudentDirectoryRepository
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
import com.kyivsec.duikttimetable.model.Occupation
import com.kyivsec.duikttimetable.model.TeacherInfo
import com.kyivsec.duikttimetable.model.TimetableOwner
import com.kyivsec.duikttimetable.model.StudentInfo
import com.kyivsec.duikttimetable.R
import com.kyivsec.duikttimetable.util.weekStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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
    private val teacherDirectoryRepository: TeacherDirectoryRepository = EmptyTeacherDirectoryRepository,
    private val studentDirectoryRepository: StudentDirectoryRepository = EmptyStudentDirectoryRepository,
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
    private val syncCoordinator = ScheduleSyncCoordinator(viewModelScope)
    private var directoryRefreshJob: Job? = null
    private var loadJob: Job? = null
    private var institutesJob: Job? = null
    private var coursesJob: Job? = null
    private var groupsJob: Job? = null
    private var chairsJob: Job? = null
    private var teachersJob: Job? = null
    private var studentsJob: Job? = null
    private var rememberedGroup: TimetableOwner.Group? = null
    private var rememberedTeacher: TimetableOwner.Teacher? = null
    private var rememberedStudent: TimetableOwner.Student? = null
    private var ownerGeneration = 0L

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
        _uiState.update { it.copy(selectedMode = mode) }
    }

    fun selectDate(date: LocalDate) {
        val state = _uiState.value
        if (date !in state.availableDates || date == state.selectedDate) return
        _uiState.update { it.copy(selectedDate = date) }
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
        cancelDirectoryJobs()
        observeInstitutes()
        directoryRefreshJob = viewModelScope.launch { handleSyncResult(groupDirectoryRepository.ensureInstitutes()) }
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

    fun startOccupationSelection() = _uiState.update { it.copy(needsOccupationSelection = true) }

    fun dismissSelection() = _uiState.update { it.copy(
        needsOccupationSelection = false,
        needsGroupSelection = false,
        needsTeacherSelection = false,
        needsStudentSelection = false,
    ) }

    fun selectOccupation(occupation: Occupation): Boolean {
        val remembered = when (occupation) {
            Occupation.GROUP -> rememberedGroup
            Occupation.STUDENT -> rememberedStudent
            Occupation.TEACHER -> rememberedTeacher
        }
        if (remembered != null) {
            applyOwner(remembered)
            return false
        }
        viewModelScope.launch { preferencesRepository.saveOccupation(occupation) }
        _uiState.update { state -> state.copy(
            activeOccupation = occupation,
            needsOccupationSelection = false,
            needsGroupSelection = occupation == Occupation.GROUP && state.activeOwner !is TimetableOwner.Group,
            needsTeacherSelection = occupation == Occupation.TEACHER && state.activeOwner !is TimetableOwner.Teacher,
            needsStudentSelection = occupation == Occupation.STUDENT && state.activeOwner !is TimetableOwner.Student,
        ) }
        when (occupation) {
            Occupation.GROUP -> startGroupSelection()
            Occupation.STUDENT -> startStudentSelection()
            Occupation.TEACHER -> startTeacherSelection()
        }
        return true
    }

    fun startStudentSelection() {
        cancelDirectoryJobs()
        observeInstitutes()
        directoryRefreshJob = viewModelScope.launch { handleSyncResult(studentDirectoryRepository.ensureStudentInstitutes()) }
        val selected = (_uiState.value.activeOwner as? TimetableOwner.Student)?.student
        if (selected == null) {
            _uiState.update { it.copy(
                draftInstituteId = null, draftCourse = null, draftGroup = null, draftStudent = null,
                directoryCourses = emptyList(), directoryGroups = emptyList(), directoryStudents = emptyList(),
            ) }
            return
        }
        val group = GroupInfo(
            selected.groupId, selected.groupName, selected.instituteId, selected.instituteName, selected.course,
        )
        _uiState.update { it.copy(
            draftInstituteId = selected.instituteId, draftCourse = selected.course,
            draftGroup = group, draftStudent = selected,
        ) }
        loadStudentDirectoryBranch(selected.instituteId, selected.course)
        loadStudents(group)
    }

    fun selectDraftStudentInstitute(instituteId: Long) {
        if (_uiState.value.draftInstituteId == instituteId) return
        cancelGroupAndStudentJobs()
        _uiState.update { it.copy(draftInstituteId = instituteId, draftCourse = null, draftGroup = null, draftStudent = null, directoryCourses = emptyList(), directoryGroups = emptyList(), directoryStudents = emptyList()) }
        loadStudentCourses(instituteId)
    }

    fun selectDraftStudentCourse(course: Int) {
        val instituteId = _uiState.value.draftInstituteId ?: return
        if (_uiState.value.draftCourse == course) return
        cancelStudentsJob()
        _uiState.update { it.copy(draftCourse = course, draftGroup = null, draftStudent = null, directoryGroups = emptyList(), directoryStudents = emptyList()) }
        loadStudentGroups(instituteId, course)
    }

    fun startTeacherSelection() {
        cancelDirectoryJobs()
        observeChairs()
        directoryRefreshJob = viewModelScope.launch { handleSyncResult(teacherDirectoryRepository.ensureChairs()) }
        val selected = (_uiState.value.activeOwner as? TimetableOwner.Teacher)?.teacher
        _uiState.update { it.copy(
            draftChairId = selected?.chairId,
            draftTeacher = selected,
            directoryTeachers = emptyList(),
        ) }
        selected?.chairId?.let(::loadTeachers)
    }

    fun selectDraftChair(chairId: Long) {
        if (_uiState.value.draftChairId == chairId) return
        _uiState.update { it.copy(draftChairId = chairId, draftTeacher = null, directoryTeachers = emptyList()) }
        loadTeachers(chairId)
    }

    fun selectDraftTeacher(teacher: TeacherInfo) = _uiState.update { it.copy(draftTeacher = teacher) }

    fun applyTeacher(teacher: TeacherInfo) = applyOwner(TimetableOwner.Teacher(teacher))

    fun selectDraftInstitute(instituteId: Long) {
        if (_uiState.value.draftInstituteId == instituteId) return
        cancelGroupAndStudentJobs()
        _uiState.update { it.copy(draftInstituteId = instituteId, draftCourse = null, draftGroup = null, draftStudent = null, directoryCourses = emptyList(), directoryGroups = emptyList(), directoryStudents = emptyList()) }
        loadCourses(instituteId)
    }

    fun selectDraftCourse(course: Int) {
        val instituteId = _uiState.value.draftInstituteId ?: return
        if (_uiState.value.draftCourse == course) return
        cancelStudentsJob()
        _uiState.update { it.copy(draftCourse = course, draftGroup = null, draftStudent = null, directoryGroups = emptyList(), directoryStudents = emptyList()) }
        loadGroups(instituteId, course)
    }

    fun selectDraftGroup(group: GroupInfo) {
        if (_uiState.value.draftGroup?.id != group.id) _uiState.update { it.copy(draftGroup = group) }
    }

    fun selectDraftStudentGroup(group: GroupInfo) {
        _uiState.update { it.copy(draftGroup = group, draftStudent = null, directoryStudents = emptyList()) }
        loadStudents(group)
    }

    fun selectDraftStudent(student: StudentInfo) = _uiState.update { it.copy(draftStudent = student) }

    fun applyStudent(student: StudentInfo) = applyOwner(TimetableOwner.Student(student))

    fun applyGroup(group: GroupInfo) {
        applyOwner(TimetableOwner.Group(group))
    }

    private fun applyOwner(owner: TimetableOwner) {
        val current = _uiState.value.activeOwner
        if (current?.type == owner.type && current.id == owner.id) return
        ownerGeneration++
        scheduleJob?.cancel()
        syncCoordinator.cancel()
        cancelDirectoryJobs()
        _selectedLesson.value = null
        allDays = emptyList()
        when (owner) {
            is TimetableOwner.Group -> rememberedGroup = owner
            is TimetableOwner.Student -> rememberedStudent = owner
            is TimetableOwner.Teacher -> rememberedTeacher = owner
        }
        val currentDate = today()
        _uiState.update { it.copy(
            isLoading = false,
            isRefreshing = false,
            isFullReloading = false,
            errorMessage = null,
            activeOwner = owner,
            activeOccupation = owner.type,
            selectedGroup = (owner as? TimetableOwner.Group)?.group,
            draftGroup = (owner as? TimetableOwner.Group)?.group ?: it.draftGroup,
            draftTeacher = (owner as? TimetableOwner.Teacher)?.teacher ?: it.draftTeacher,
            draftStudent = (owner as? TimetableOwner.Student)?.student ?: it.draftStudent,
            needsGroupSelection = false,
            needsTeacherSelection = false,
            needsStudentSelection = false,
            needsOccupationSelection = false,
            selectedDate = currentDate,
            selectedWeek = weekStart(currentDate, DayOfWeek.MONDAY),
            expandedDates = emptySet(),
            expandedDatesInitialized = false,
        ) }
        viewModelScope.launch { preferencesRepository.saveSelectedOwner(owner) }
        rebuildWindows()
        observeSchedule(owner)
        syncSelectedOwner(owner)
    }

    fun updateTheme(value: ThemeMode) = updateSettings { it.copy(themeMode = value) }
    fun updateStartupMode(value: ScheduleMode) = updateSettings { it.copy(startupMode = value) }
    fun updateHideClassesInWeekView(value: Boolean) = updateSettings { it.copy(hideClassesInWeekView = value) }
    fun updateFastUpdate(value: Boolean) = updateSettings { it.copy(fastUpdate = value) }
    fun updatePreviousDays(value: Int) = updateSettings { it.copy(previousDaysToKeep = value.coerceIn(0, 30)) }
    fun updatePreviousWeeks(value: Int) = updateSettings { it.copy(previousWeeksToKeep = value.coerceIn(0, 12)) }

    fun refreshVisible() {
        val snapshot = _uiState.value
        val owner = snapshot.activeOwner ?: return
        if (snapshot.isRefreshing || snapshot.isFullReloading) return
        val range = if (snapshot.settings.fastUpdate) fastUpdateRange(snapshot)
        else visibleRange(snapshot)
        launchRefresh { scheduleRepository.syncSchedule(owner, range, force = true) }
    }

    fun fullReload() {
        val snapshot = _uiState.value
        val owner = snapshot.activeOwner ?: return
        if (snapshot.isRefreshing || snapshot.isFullReloading) return
        launchRefresh(fullReload = true) {
            val directoryResults = when (owner) {
                is TimetableOwner.Group -> listOf(
                    groupDirectoryRepository.ensureInstitutes(force = true),
                    groupDirectoryRepository.ensureCourses(owner.group.instituteId, force = true),
                    groupDirectoryRepository.ensureGroups(owner.group.instituteId, owner.group.course, force = true),
                )
                is TimetableOwner.Student -> {
                    val student = owner.student
                    listOf(
                        studentDirectoryRepository.ensureStudentInstitutes(force = true),
                        studentDirectoryRepository.ensureStudentCourses(student.instituteId, force = true),
                        studentDirectoryRepository.ensureStudentGroups(student.instituteId, student.course, force = true),
                        studentDirectoryRepository.ensureStudents(
                            GroupInfo(student.groupId, student.groupName, student.instituteId, student.instituteName, student.course),
                            force = true,
                        ),
                    )
                }
                is TimetableOwner.Teacher -> listOf(
                    teacherDirectoryRepository.ensureChairs(force = true),
                    teacherDirectoryRepository.ensureTeachers(owner.teacher.chairId, force = true),
                )
            }
            currentCoroutineContext().ensureActive()
            val scheduleResult = scheduleRepository.syncCurrentSemester(owner, force = true)
            if (scheduleResult is SyncResult.Failure) scheduleResult
            else directoryResults.filterIsInstance<SyncResult.Failure>().firstOrNull() ?: scheduleResult
        }
    }

    private fun launchRefresh(fullReload: Boolean = false, work: suspend () -> SyncResult) {
        syncCoordinator.start(
            onStart = { _uiState.update { it.copy(isRefreshing = !fullReload, isFullReloading = fullReload) } },
            work = work,
            onResult = { result ->
                handleSyncResult(result)
                if (fullReload && result is SyncResult.Success) {
                    preferencesRepository.saveLastFullRefresh(result.completedAt.toEpochMilli())
                    currentCoroutineContext().ensureActive()
                    _uiState.update { it.copy(lastFullRefreshEpochMillis = result.completedAt.toEpochMilli()) }
                    pruneHistory()
                }
            },
            onFinish = { _uiState.update { it.copy(isRefreshing = false, isFullReloading = false) } },
        )
    }

    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val initialOwnerGeneration = ownerGeneration
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val preferences = runCatching { preferencesRepository.load() }.getOrElse {
                if (it is CancellationException) throw it
                currentCoroutineContext().ensureActive()
                if (initialOwnerGeneration != ownerGeneration) return@launch
                _uiState.update { state -> state.copy(isLoading = false, errorMessage = UiMessage(R.string.error_read_settings)) }
                return@launch
            }
            currentCoroutineContext().ensureActive()
            if (initialOwnerGeneration != ownerGeneration) return@launch
            if (preferences.selectedGroupId != null && preferences.selectedInstituteId != null && preferences.selectedCourse != null && preferences.selectedGroupName != null) {
                rememberedGroup = TimetableOwner.Group(GroupInfo(
                    preferences.selectedGroupId, preferences.selectedGroupName, preferences.selectedInstituteId,
                    "", preferences.selectedCourse,
                ))
            }
            if (preferences.selectedTeacherId != null && preferences.selectedChairId != null && preferences.selectedTeacherName != null) {
                rememberedTeacher = TimetableOwner.Teacher(TeacherInfo(
                    preferences.selectedTeacherId, preferences.selectedTeacherName, preferences.selectedChairId,
                    preferences.selectedChairName.orEmpty(),
                ))
            }
            if (
                preferences.selectedStudentId != null && preferences.selectedStudentName != null &&
                preferences.selectedStudentGroupId != null && preferences.selectedStudentGroupName != null &&
                preferences.selectedStudentInstituteId != null && preferences.selectedStudentCourse != null
            ) {
                rememberedStudent = TimetableOwner.Student(StudentInfo(
                    preferences.selectedStudentId, preferences.selectedStudentName,
                    preferences.selectedStudentGroupId, preferences.selectedStudentGroupName,
                    preferences.selectedStudentInstituteId, preferences.selectedStudentInstituteName.orEmpty(),
                    preferences.selectedStudentCourse,
                ))
            }
            _uiState.update { it.copy(
                settings = preferences.settings,
                selectedMode = preferences.settings.startupMode,
                weekSectionsStartExpanded = !preferences.settings.hideClassesInWeekView,
                lastFullRefreshEpochMillis = preferences.lastFullRefreshEpochMillis,
                activeOccupation = preferences.occupation,
            ) }
            val owner = when (preferences.occupation) {
                Occupation.TEACHER -> rememberedTeacher
                Occupation.STUDENT -> rememberedStudent
                Occupation.GROUP, null -> rememberedGroup
            }
            if (owner != null) {
                if (_uiState.value.activeOwner == owner) {
                    _uiState.update { it.copy(isLoading = false) }
                    observeSchedule(owner)
                    syncSelectedOwner(owner)
                } else {
                    applyOwner(owner)
                }
            } else {
                _uiState.update { it.copy(
                    isLoading = false,
                    needsOccupationSelection = preferences.occupation == null,
                    needsGroupSelection = preferences.occupation == null || preferences.occupation == Occupation.GROUP,
                    needsTeacherSelection = preferences.occupation == Occupation.TEACHER,
                    needsStudentSelection = preferences.occupation == Occupation.STUDENT,
                ) }
            }
            // Populate selectors independently; cached lessons never wait for directory requests.
            when (preferences.occupation) {
                Occupation.TEACHER -> startTeacherSelection()
                Occupation.STUDENT -> startStudentSelection()
                Occupation.GROUP, null -> startGroupSelection()
            }
        }
    }

    private fun cancelGroupAndStudentJobs() {
        groupsJob?.cancel()
        cancelStudentsJob()
        _uiState.update { it.copy(isGroupsLoading = false) }
    }

    private fun cancelStudentsJob() {
        studentsJob?.cancel()
        _uiState.update { it.copy(isStudentsLoading = false) }
    }

    private fun cancelDirectoryJobs() {
        directoryRefreshJob?.cancel()
        institutesJob?.cancel()
        coursesJob?.cancel()
        chairsJob?.cancel()
        teachersJob?.cancel()
        cancelGroupAndStudentJobs()
        _uiState.update { it.copy(isCoursesLoading = false, isTeachersLoading = false) }
    }

    private fun observeInstitutes() {
        institutesJob?.cancel()
        institutesJob = viewModelScope.launch {
            groupDirectoryRepository.observeInstitutes().collect { values -> _uiState.update { it.copy(institutes = values) } }
        }
    }

    private fun observeChairs() {
        chairsJob?.cancel()
        chairsJob = viewModelScope.launch {
            teacherDirectoryRepository.observeChairs().collect { values -> _uiState.update { it.copy(chairs = values) } }
        }
    }

    private fun loadTeachers(chairId: Long, ensure: Boolean = true) {
        teachersJob?.cancel()
        teachersJob = viewModelScope.launch {
            _uiState.update { it.copy(isTeachersLoading = true) }
            if (ensure) handleSyncResult(teacherDirectoryRepository.ensureTeachers(chairId))
            teacherDirectoryRepository.observeTeachers(chairId).collect { values ->
                _uiState.update { it.copy(directoryTeachers = values, isTeachersLoading = false) }
            }
        }
    }

    private fun loadStudents(group: GroupInfo, ensure: Boolean = true) {
        studentsJob?.cancel()
        studentsJob = viewModelScope.launch {
            _uiState.update { it.copy(isStudentsLoading = true) }
            if (ensure) handleSyncResult(studentDirectoryRepository.ensureStudents(group))
            studentDirectoryRepository.observeStudents(group.id).collect { values ->
                _uiState.update { it.copy(directoryStudents = values, isStudentsLoading = false) }
            }
        }
    }

    private fun loadDirectoryBranch(instituteId: Long, course: Int, ensure: Boolean = true) {
        loadCourses(instituteId, ensure = ensure)
        loadGroups(instituteId, course, ensure = ensure)
    }

    private fun loadStudentDirectoryBranch(instituteId: Long, course: Int, ensure: Boolean = true) {
        loadStudentCourses(instituteId, ensure)
        loadStudentGroups(instituteId, course, ensure)
    }

    private fun loadStudentCourses(instituteId: Long, ensure: Boolean = true) {
        coursesJob?.cancel()
        coursesJob = viewModelScope.launch {
            _uiState.update { it.copy(isCoursesLoading = true) }
            if (ensure) handleSyncResult(studentDirectoryRepository.ensureStudentCourses(instituteId))
            groupDirectoryRepository.observeCourses(instituteId).collect { values ->
                _uiState.update { it.copy(directoryCourses = values, isCoursesLoading = false) }
            }
        }
    }

    private fun loadStudentGroups(instituteId: Long, course: Int, ensure: Boolean = true) {
        groupsJob?.cancel()
        groupsJob = viewModelScope.launch {
            _uiState.update { it.copy(isGroupsLoading = true) }
            if (ensure) handleSyncResult(studentDirectoryRepository.ensureStudentGroups(instituteId, course))
            groupDirectoryRepository.observeGroups(instituteId, course).collect { values ->
                _uiState.update { it.copy(directoryGroups = values, isGroupsLoading = false) }
            }
        }
    }

    private fun loadCourses(instituteId: Long, ensure: Boolean = true) {
        coursesJob?.cancel()
        coursesJob = viewModelScope.launch {
            _uiState.update { it.copy(isCoursesLoading = true) }
            if (ensure) handleSyncResult(groupDirectoryRepository.ensureCourses(instituteId))
            groupDirectoryRepository.observeCourses(instituteId).collect { values ->
                _uiState.update { it.copy(directoryCourses = values, isCoursesLoading = false) }
            }
        }
    }

    private fun loadGroups(instituteId: Long, course: Int, ensure: Boolean = true) {
        groupsJob?.cancel()
        groupsJob = viewModelScope.launch {
            _uiState.update { it.copy(isGroupsLoading = true) }
            if (ensure) handleSyncResult(groupDirectoryRepository.ensureGroups(instituteId, course))
            groupDirectoryRepository.observeGroups(instituteId, course).collect { values ->
                _uiState.update { it.copy(directoryGroups = values, isGroupsLoading = false) }
            }
        }
    }

    private fun observeSchedule(owner: TimetableOwner) {
        scheduleJob?.cancel()
        scheduleJob = viewModelScope.launch {
            scheduleRepository.observeSchedule(owner, displayRange()).collect { days ->
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

    private fun syncSelectedOwner(owner: TimetableOwner) {
        val snapshot = _uiState.value
        launchRefresh {
            if (!snapshot.settings.fastUpdate) {
                scheduleRepository.syncCurrentSemester(owner)
            } else {
                val hasCachedSchedule = scheduleRepository.observeCachedDates(owner, displayRange()).first().isNotEmpty()
                if (hasCachedSchedule) {
                    scheduleRepository.syncSchedule(owner, fastUpdateRange(snapshot), force = true)
                } else {
                    scheduleRepository.syncCurrentSemester(owner, force = true)
                }
            }
        }
    }

    private fun visibleRange(state: ScheduleUiState): DateRange =
        if (state.selectedMode == ScheduleMode.DAY) DateRange(state.selectedDate, state.selectedDate)
        else DateRange(state.selectedWeek, state.selectedWeek.plusDays(6))

    private fun fastUpdateRange(state: ScheduleUiState): DateRange {
        val currentDate = today()
        val currentWeek = weekStart(currentDate, DayOfWeek.MONDAY)
        val end = currentWeek.plusWeeks(FUTURE_WEEKS).plusDays(6)
        val start = if (state.selectedMode == ScheduleMode.DAY) {
            currentDate.minusDays(state.settings.previousDaysToKeep.toLong())
        } else {
            currentWeek.minusWeeks(state.settings.previousWeeksToKeep.toLong())
        }
        return DateRange(start, end)
    }

    private suspend fun rebuildWindowsOffMain() {
        val snapshot = _uiState.value
        val days = allDays
        val result = withContext(computationDispatcher) { buildWindows(snapshot, days) }
        _uiState.update { current ->
            if (current.settings != snapshot.settings || current.activeOwner != snapshot.activeOwner) current
            else current.copy(
                weeks = reuseEqualItems(current.weeks, result.weeks),
                availableDates = if (current.availableDates == result.dates) current.availableDates else result.dates,
                selectedDate = closestAvailableDate(current.selectedDate, result.dates),
                selectedWeek = current.selectedWeek.takeIf { selected -> result.weeks.any { it.startDate == selected } }
                    ?: result.selectedWeek,
                expandedDates = if (current.expandedDates == snapshot.expandedDates &&
                    current.expandedDatesInitialized == snapshot.expandedDatesInitialized
                ) result.expandedDates else current.expandedDates.intersect(result.weeks.flatMap { week ->
                    week.days.filter { it.lessons.isNotEmpty() }.map { it.date }
                }.toSet()),
                expandedDatesInitialized = current.expandedDatesInitialized || result.expandedDatesInitialized,
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
            _uiState.value.activeOwner?.let(::observeSchedule)
        }
        viewModelScope.launch { preferencesRepository.saveSettings(settings) }
    }

    private suspend fun pruneHistory() {
        val settings = _uiState.value.settings
        val weekBoundary = weekStart(today(), DayOfWeek.MONDAY).minusWeeks(settings.previousWeeksToKeep.toLong())
        val dayBoundary = today().minusDays(settings.previousDaysToKeep.toLong())
        scheduleRepository.pruneBefore(minOf(weekBoundary, dayBoundary))
    }

    private suspend fun handleSyncResult(result: SyncResult) {
        currentCoroutineContext().ensureActive()
        if (result is SyncResult.Failure) showError(result.error)
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
        private val teacherDirectoryRepository: TeacherDirectoryRepository = EmptyTeacherDirectoryRepository,
        private val studentDirectoryRepository: StudentDirectoryRepository = EmptyStudentDirectoryRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ScheduleViewModel(
                scheduleRepository, groupDirectoryRepository, preferencesRepository, clock,
                teacherDirectoryRepository = teacherDirectoryRepository,
                studentDirectoryRepository = studentDirectoryRepository,
            ) as T
    }

    private companion object {
        const val FUTURE_WEEKS = 12L
    }
}

private object EmptyTeacherDirectoryRepository : TeacherDirectoryRepository {
    override fun observeChairs() = kotlinx.coroutines.flow.flowOf(emptyList<com.kyivsec.duikttimetable.model.ChairInfo>())
    override fun observeTeachers(chairId: Long) = kotlinx.coroutines.flow.flowOf(emptyList<TeacherInfo>())
    override suspend fun ensureChairs(force: Boolean) = SyncResult.Success(Instant.EPOCH, 0)
    override suspend fun ensureTeachers(chairId: Long, force: Boolean) = SyncResult.Success(Instant.EPOCH, 0)
}

private object EmptyStudentDirectoryRepository : StudentDirectoryRepository {
    override suspend fun ensureStudentInstitutes(force: Boolean) = SyncResult.Success(Instant.EPOCH, 0)
    override suspend fun ensureStudentCourses(instituteId: Long, force: Boolean) = SyncResult.Success(Instant.EPOCH, 0)
    override suspend fun ensureStudentGroups(instituteId: Long, course: Int, force: Boolean) = SyncResult.Success(Instant.EPOCH, 0)
    override fun observeStudents(groupId: Long) = kotlinx.coroutines.flow.flowOf(emptyList<StudentInfo>())
    override suspend fun ensureStudents(group: GroupInfo, force: Boolean) = SyncResult.Success(Instant.EPOCH, 0)
}
