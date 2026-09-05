package com.kyivsec.duikttimetable

import androidx.compose.runtime.getValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.test.platform.app.InstrumentationRegistry
import com.kyivsec.duikttimetable.data.*
import com.kyivsec.duikttimetable.model.*
import com.kyivsec.duikttimetable.ui.screen.ScheduleScreen
import com.kyivsec.duikttimetable.ui.theme.DuiktTimetableTheme
import com.kyivsec.duikttimetable.viewmodel.ScheduleViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

class ScheduleScreenInteractionTest {
    @get:Rule val composeRule = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var viewModel: ScheduleViewModel
    private lateinit var fullReload: CompletableDeferred<SyncResult>

    @Before fun setUp() {
        val clock = Clock.fixed(Instant.parse("2026-09-07T07:00:00Z"), ZoneOffset.UTC)
        val mock = MockScheduleRepository(MockDataLoader(context, clock), clock)
        fullReload = CompletableDeferred()
        val repository = object : ScheduleRepository by mock {
            override fun observeSchedule(owner: TimetableOwner, range: DateRange) = flowOf(listOf(
                ScheduleDay(LocalDate.of(2026, 9, 7), listOf(
                    Lesson("test", "Проєктний практикум", LessonType.PRACTICE, LocalTime.of(9, 30), LocalTime.of(10, 50)),
                )),
            ))
            override suspend fun syncCurrentSemester(owner: TimetableOwner, force: Boolean): SyncResult =
                if (force) fullReload.await() else SyncResult.Success(clock.instant(), 0)
        }
        val preferences = object : SettingsRepository {
            override suspend fun load() = StoredPreferences(
                occupation = Occupation.GROUP,
                selectedGroupId = 1001, selectedInstituteId = 1, selectedCourse = 3, selectedGroupName = "ПД-31",
            )
            override suspend fun saveSettings(settings: ScheduleSettings) = Unit
            override suspend fun saveSelectedGroup(group: GroupInfo) = Unit
            override suspend fun saveLastFullRefresh(epochMillis: Long) = Unit
        }
        composeRule.runOnUiThread { viewModel = ScheduleViewModel(repository, mock, preferences, clock) }
        composeRule.setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            DuiktTimetableTheme { ScheduleScreen(state, viewModel) }
        }
        composeRule.waitUntil(5_000) {
            !viewModel.uiState.value.isLoading && !viewModel.uiState.value.isRefreshing &&
                viewModel.uiState.value.weeks.flatMap { it.days }.any { it.lessons.isNotEmpty() }
        }
    }

    @After fun tearDown() {
        composeRule.runOnUiThread { viewModel.viewModelScope.cancel() }
    }

    @Test fun weekModeAndLessonDetailsWork() {
        composeRule.onNodeWithTag("scheduleMode:WEEK").performClick()
        composeRule.onNodeWithText("Проєктний практикум").assertIsDisplayed().performClick()
        composeRule.onNodeWithText(context.getString(R.string.lesson_type_practice)).assertIsDisplayed()
    }

    @Test fun groupSelectorWorks() {
        composeRule.onNodeWithText("ПД-31").performClick()
        composeRule.onNodeWithText(context.getString(R.string.select_group)).assertIsDisplayed()
        composeRule.onNodeWithText("ННІ Інформаційних технологій").performClick()
        composeRule.onNodeWithText("ННІ Кібербезпеки").performClick()
        composeRule.onNodeWithText(context.getString(R.string.course)).performClick()
        composeRule.onNodeWithTag("dropdownOption:2").performClick()
        composeRule.onNodeWithText(context.getString(R.string.group)).performClick()
        composeRule.onNodeWithText("БКС-21").performClick()
        composeRule.onNodeWithText(context.getString(R.string.apply)).performClick()
        composeRule.onNodeWithText("БКС-21").assertIsDisplayed()
    }

    @Test fun settingsDrawerWorks() {
        composeRule.onNodeWithContentDescription(context.getString(R.string.open_settings)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.settings)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.reload_all)).performScrollTo().performClick()
        composeRule.onNodeWithText(context.getString(R.string.reload_in_progress)).assertIsDisplayed()
        fullReload.complete(SyncResult.Success(Instant.parse("2026-09-07T07:00:00Z"), 0))
        composeRule.waitUntil(5_000) { !viewModel.uiState.value.isFullReloading }
        composeRule.onNodeWithText(context.getString(R.string.reload_all)).assertIsDisplayed()
    }
}
