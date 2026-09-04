package com.kyivsec.duikttimetable.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import com.kyivsec.duikttimetable.R
import com.kyivsec.duikttimetable.util.LanguageHandler
import com.kyivsec.duikttimetable.model.ScheduleMode
import com.kyivsec.duikttimetable.model.ScheduleUiState
import com.kyivsec.duikttimetable.ui.component.DaySchedule
import com.kyivsec.duikttimetable.ui.component.GroupSelectorSheet
import com.kyivsec.duikttimetable.ui.component.OccupationSelectorSheet
import com.kyivsec.duikttimetable.ui.component.TeacherSelectorSheet
import com.kyivsec.duikttimetable.ui.component.StudentSelectorSheet
import com.kyivsec.duikttimetable.model.Occupation
import com.kyivsec.duikttimetable.ui.component.LessonDetailsSheet
import com.kyivsec.duikttimetable.ui.component.ScheduleModeSelector
import com.kyivsec.duikttimetable.ui.component.SettingsDrawer
import com.kyivsec.duikttimetable.ui.component.TopScheduleBar
import com.kyivsec.duikttimetable.ui.component.WeekSchedule
import com.kyivsec.duikttimetable.util.weekStart
import com.kyivsec.duikttimetable.viewmodel.ScheduleViewModel
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@Composable
fun ScheduleScreen(state: ScheduleUiState, viewModel: ScheduleViewModel) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showGroupSheet by remember { mutableStateOf(false) }
    var showTeacherSheet by remember { mutableStateOf(false) }
    var showOccupationSheet by remember { mutableStateOf(false) }
    var showStudentSheet by remember { mutableStateOf(false) }
    val localizedError = state.errorMessage?.let { message ->
        message.numberArgument?.let { stringResource(message.resourceId, it) }
            ?: stringResource(message.resourceId)
    }

    LaunchedEffect(state.needsOccupationSelection, state.needsGroupSelection, state.needsTeacherSelection, state.needsStudentSelection) {
        if (state.needsOccupationSelection) {
            showOccupationSheet = true
        } else if (state.needsGroupSelection) {
            viewModel.startGroupSelection()
            showGroupSheet = true
        } else if (state.needsTeacherSelection) {
            viewModel.startTeacherSelection()
            showTeacherSheet = true
        } else if (state.needsStudentSelection) {
            viewModel.startStudentSelection()
            showStudentSheet = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            SettingsDrawer(
                settings = state.settings,
                isFullReloading = state.isFullReloading,
                lastRefreshEpochMillis = state.lastFullRefreshEpochMillis,
                selectedLanguage = LanguageHandler.selectedLanguage(context),
                onLanguageChange = { language -> LanguageHandler.setLanguageAndRecreate(context, language) },
                onThemeChange = viewModel::updateTheme,
                onStartupModeChange = viewModel::updateStartupMode,
                onHideClassesInWeekViewChange = viewModel::updateHideClassesInWeekView,
                onPreviousDaysChange = viewModel::updatePreviousDays,
                onPreviousWeeksChange = viewModel::updatePreviousWeeks,
                onFullReload = viewModel::fullReload,
                onChangeOccupation = {
                    scope.launch { drawerState.close() }
                    showOccupationSheet = true
                },
            )
        },
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars),
            ) {
                Surface(color = MaterialTheme.colorScheme.background, tonalElevation = 1.dp) {
                    TopScheduleBar(
                        groupName = state.activeOwner?.displayName ?: stringResource(R.string.select_timetable_owner),
                        isRefreshing = state.isRefreshing,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onGroupClick = {
                            when (state.activeOccupation) {
                                Occupation.TEACHER -> { viewModel.startTeacherSelection(); showTeacherSheet = true }
                                Occupation.STUDENT -> { viewModel.startStudentSelection(); showStudentSheet = true }
                                else -> { viewModel.startGroupSelection(); showGroupSheet = true }
                            }
                        },
                        onRefreshClick = viewModel::refreshVisible,
                    )
                }
                ScheduleModeSelector(state.selectedMode, viewModel::selectMode, Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                ScheduleBody(state, viewModel)
            }
        }
    }

    localizedError?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text(stringResource(R.string.error_title)) },
            text = { Text(message) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = viewModel::dismissError) {
                    Text(stringResource(R.string.dismiss))
                }
            },
        )
    }

    if (showGroupSheet) {
        GroupSelectorSheet(
            institutes = state.institutes,
            courses = state.directoryCourses,
            groups = state.directoryGroups,
            draftInstituteId = state.draftInstituteId,
            draftCourse = state.draftCourse,
            draftGroup = state.draftGroup,
            isCoursesLoading = state.isCoursesLoading,
            isGroupsLoading = state.isGroupsLoading,
            onInstituteSelected = viewModel::selectDraftInstitute,
            onCourseSelected = viewModel::selectDraftCourse,
            onGroupSelected = viewModel::selectDraftGroup,
            onApply = { viewModel.applyGroup(it); showGroupSheet = false },
            onDismiss = { showGroupSheet = false },
        )
    }
    if (showTeacherSheet) {
        TeacherSelectorSheet(
            chairs = state.chairs, teachers = state.directoryTeachers,
            draftChairId = state.draftChairId, draftTeacher = state.draftTeacher,
            isLoading = state.isTeachersLoading,
            onChairSelected = viewModel::selectDraftChair,
            onTeacherSelected = viewModel::selectDraftTeacher,
            onApply = { viewModel.applyTeacher(it); showTeacherSheet = false },
            onDismiss = { showTeacherSheet = false },
        )
    }
    if (showStudentSheet) {
        StudentSelectorSheet(
            institutes = state.institutes, courses = state.directoryCourses,
            groups = state.directoryGroups, students = state.directoryStudents,
            draftInstituteId = state.draftInstituteId, draftCourse = state.draftCourse,
            draftGroup = state.draftGroup, draftStudent = state.draftStudent,
            isCoursesLoading = state.isCoursesLoading, isGroupsLoading = state.isGroupsLoading,
            isStudentsLoading = state.isStudentsLoading,
            onInstituteSelected = viewModel::selectDraftStudentInstitute,
            onCourseSelected = viewModel::selectDraftStudentCourse,
            onGroupSelected = viewModel::selectDraftStudentGroup,
            onStudentSelected = viewModel::selectDraftStudent,
            onApply = { viewModel.applyStudent(it); showStudentSheet = false },
            onDismiss = { showStudentSheet = false },
        )
    }
    if (showOccupationSheet) {
        OccupationSelectorSheet(
            onSelected = { occupation ->
                showOccupationSheet = false
                val needsSelection = viewModel.selectOccupation(occupation)
                if (needsSelection) {
                    when (occupation) {
                        Occupation.GROUP -> showGroupSheet = true
                        Occupation.STUDENT -> showStudentSheet = true
                        Occupation.TEACHER -> showTeacherSheet = true
                    }
                }
            },
            onDismiss = { if (state.activeOwner != null) showOccupationSheet = false },
        )
    }
    LessonDetailsHost(viewModel)
}

@Composable
private fun ScheduleBody(state: ScheduleUiState, viewModel: ScheduleViewModel) {
    val now by viewModel.currentTime.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize()) {
        when {
            state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            state.weeks.isEmpty() || state.availableDates.isEmpty() -> EmptySchedule(
                message = if (state.activeOccupation == null) {
                    stringResource(R.string.choose_occupation)
                } else {
                    stringResource(R.string.schedule_unavailable)
                },
                onRetry = viewModel::retry.takeIf { state.activeOccupation != null },
            )
            else -> ScheduleContent(state, now, viewModel)
        }
        val currentWeek = weekStart(now.toLocalDate(), java.time.DayOfWeek.MONDAY)
        val showToday = if (state.selectedMode == ScheduleMode.DAY) {
            state.selectedDate != now.toLocalDate()
        } else state.selectedWeek != currentWeek
        TodayAction(showToday && !state.isLoading, viewModel::goToToday)
    }
}

@Composable
private fun LessonDetailsHost(viewModel: ScheduleViewModel) {
    val lesson by viewModel.selectedLesson.collectAsStateWithLifecycle()
    lesson?.let { LessonDetailsSheet(it, onDismiss = { viewModel.selectLesson(null) }) }
}

@Composable
private fun BoxScope.TodayAction(visible: Boolean, onClick: () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
        enter = fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.9f),
        exit = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.9f),
    ) {
        ExtendedFloatingActionButton(onClick = onClick, icon = { Icon(Icons.Rounded.Today, null) }, text = { Text(stringResource(R.string.today)) })
    }
}

@Composable
private fun ScheduleContent(state: ScheduleUiState, now: LocalDateTime, viewModel: ScheduleViewModel) {
    val dayMap = remember(state.weeks) { state.weeks.flatMap { it.days }.associateBy { it.date } }
    val dayIndex = state.availableDates.indexOf(state.selectedDate).coerceAtLeast(0)
    val weekIndex = state.weeks.indexOfFirst { it.startDate == state.selectedWeek }.coerceAtLeast(0)
    val dayPagerState = rememberPagerState(initialPage = dayIndex) { state.availableDates.size }
    val weekPagerState = rememberPagerState(initialPage = weekIndex) { state.weeks.size }
    val daySelectorState = rememberLazyListState(initialFirstVisibleItemIndex = dayIndex)
    when (state.selectedMode) {
        ScheduleMode.DAY -> DaySchedule(
            dates = state.availableDates,
            days = dayMap,
            selectedDate = state.selectedDate,
            now = now,
            onDateSelected = viewModel::selectDate,
            onLessonClick = viewModel::selectLesson,
            modifier = Modifier.fillMaxSize(),
            state = dayPagerState,
            selectorState = daySelectorState,
        )
        ScheduleMode.WEEK -> WeekSchedule(
            weeks = state.weeks,
            selectedWeek = state.selectedWeek,
            now = now,
            expandedDates = state.expandedDates,
            onWeekSelected = viewModel::selectWeek,
            onToggleDay = viewModel::toggleExpanded,
            onLessonClick = viewModel::selectLesson,
            modifier = Modifier.fillMaxSize(),
            state = weekPagerState,
        )
    }
}

@Composable
private fun EmptySchedule(message: String, onRetry: (() -> Unit)?) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, style = MaterialTheme.typography.titleMedium)
            onRetry?.let {
                androidx.compose.material3.TextButton(onClick = it) { Text(stringResource(R.string.try_again)) }
            }
        }
    }
}
