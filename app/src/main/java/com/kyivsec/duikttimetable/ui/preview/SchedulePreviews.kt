package com.kyivsec.duikttimetable.ui.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kyivsec.duikttimetable.model.ScheduleMode
import com.kyivsec.duikttimetable.model.ThemeMode
import com.kyivsec.duikttimetable.ui.component.DaySchedule
import com.kyivsec.duikttimetable.ui.component.ScheduleModeSelector
import com.kyivsec.duikttimetable.ui.component.TopScheduleBar
import com.kyivsec.duikttimetable.ui.component.WeekSchedule
import com.kyivsec.duikttimetable.ui.theme.DuiktTimetableTheme
import java.time.LocalDateTime

@Preview(name = "Day · Dark", widthDp = 360, heightDp = 780, showBackground = true)
@Composable
private fun DayPreview() {
    DuiktTimetableTheme(ThemeMode.DARK) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                TopScheduleBar("ПД-31", false, {}, {}, {})
                ScheduleModeSelector(ScheduleMode.DAY, {}, Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                DaySchedule(
                    dates = (0L..6).map { PreviewData.date.minusDays(4).plusDays(it) },
                    days = mapOf(PreviewData.date to PreviewData.day),
                    selectedDate = PreviewData.date,
                    now = LocalDateTime.of(PreviewData.date, java.time.LocalTime.of(10, 35)),
                    onDateSelected = {}, onLessonClick = {}, modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Preview(name = "Week · Narrow light", widthDp = 320, heightDp = 720, showBackground = true)
@Composable
private fun WeekPreview() {
    DuiktTimetableTheme(ThemeMode.LIGHT) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                TopScheduleBar("ПД-31", false, {}, {}, {})
                ScheduleModeSelector(ScheduleMode.WEEK, {}, Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                WeekSchedule(
                    weeks = listOf(PreviewData.week), selectedWeek = PreviewData.week.startDate,
                    now = LocalDateTime.of(PreviewData.date, java.time.LocalTime.of(10, 35)), expandedDates = setOf(PreviewData.week.startDate),
                    onWeekSelected = {}, onToggleDay = {}, onLessonClick = {}, modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
