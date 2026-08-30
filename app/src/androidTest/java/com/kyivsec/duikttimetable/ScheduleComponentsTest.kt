package com.kyivsec.duikttimetable

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kyivsec.duikttimetable.model.Lesson
import com.kyivsec.duikttimetable.model.LessonType
import com.kyivsec.duikttimetable.model.ScheduleDay
import com.kyivsec.duikttimetable.model.ScheduleMode
import com.kyivsec.duikttimetable.ui.component.ScheduleModeSelector
import com.kyivsec.duikttimetable.ui.component.DaySchedule
import com.kyivsec.duikttimetable.ui.component.WeekSchedule
import com.kyivsec.duikttimetable.ui.component.WeekDayCard
import com.kyivsec.duikttimetable.ui.theme.DuiktTimetableTheme
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import androidx.test.platform.app.InstrumentationRegistry

class ScheduleComponentsTest {
    @get:Rule val composeRule = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun modeSelectorChangesMode() {
        var selected by mutableStateOf(ScheduleMode.DAY)
        composeRule.setContent { DuiktTimetableTheme { ScheduleModeSelector(selectedMode = selected, onModeSelected = { selected = it }) } }
        composeRule.onNodeWithText(context.getString(R.string.week)).performClick()
        composeRule.runOnIdle { assert(selected == ScheduleMode.WEEK) }
    }

    @Test fun weekCardExpandsAndShowsLesson() {
        val lesson = Lesson("1", "Дуже довга назва предмета для перевірки", LessonType.LECTURE, LocalTime.of(8, 0), LocalTime.of(9, 20), room = "201")
        val day = ScheduleDay(LocalDate.of(2026, 8, 28), listOf(lesson))
        var expanded by mutableStateOf(false)
        composeRule.setContent {
            DuiktTimetableTheme {
                WeekDayCard(day, true, expanded, java.time.LocalDateTime.of(2026, 8, 28, 7, 30), { expanded = true }, {})
            }
        }
        composeRule.onNodeWithText(context.resources.getQuantityString(R.plurals.lesson_count, 1, 1)).performClick()
        composeRule.onNodeWithText("Дуже довга назва предмета для перевірки").assertIsDisplayed()
    }

    @Test fun swipingDayUpdatesSelectedPill() {
        val first = LocalDate.of(2026, 8, 28)
        val dates = listOf(first, first.plusDays(1))
        var selected by mutableStateOf(first)
        composeRule.setContent {
            DuiktTimetableTheme {
                DaySchedule(dates, emptyMap(), selected, LocalDateTime.of(2026, 8, 28, 10, 0), { selected = it }, {})
            }
        }
        composeRule.onNodeWithTag("dayPager").performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("29").assertIsSelected()
    }

    @Test fun swipingWeekUpdatesRangeWithoutWaitingForSettlementState() {
        val firstStart = LocalDate.of(2026, 8, 24)
        fun week(start: LocalDate, number: Int) = com.kyivsec.duikttimetable.model.ScheduleWeek(
            number, start, start.plusDays(6), (0L..6).map { ScheduleDay(start.plusDays(it), emptyList()) },
        )
        val weeks = listOf(week(firstStart, 35), week(firstStart.plusWeeks(1), 36))
        var selected by mutableStateOf(firstStart)
        composeRule.setContent {
            DuiktTimetableTheme {
                WeekSchedule(weeks, selected, LocalDateTime.of(2026, 8, 28, 10, 0), emptySet(), { selected = it }, {}, {})
            }
        }
        composeRule.onNodeWithTag("weekPager").performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(context.getString(R.string.week), substring = true).assertIsDisplayed()
    }
}
