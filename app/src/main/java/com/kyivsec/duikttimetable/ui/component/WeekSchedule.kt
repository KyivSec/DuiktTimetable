package com.kyivsec.duikttimetable.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kyivsec.duikttimetable.model.Lesson
import com.kyivsec.duikttimetable.model.ScheduleWeek
import com.kyivsec.duikttimetable.util.weekRangeText
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
fun WeekSchedule(
    weeks: List<ScheduleWeek>, selectedWeek: LocalDate, now: LocalDateTime, expandedDates: Set<LocalDate>,
    onWeekSelected: (LocalDate) -> Unit, onToggleDay: (LocalDate) -> Unit, onLessonClick: (Lesson) -> Unit,
    modifier: Modifier = Modifier,
    state: PagerState? = null,
) {
    if (weeks.isEmpty()) return
    val selectedIndex = weeks.indexOfFirst { it.startDate == selectedWeek }.coerceAtLeast(0)
    val pagerState = state ?: rememberPagerState(initialPage = selectedIndex) { weeks.size }
    LaunchedEffect(selectedIndex) {
        if (pagerState.currentPage != selectedIndex) pagerState.scrollToPage(selectedIndex)
    }
    LaunchedEffect(pagerState, weeks) {
        snapshotFlow { pagerState.settledPage }.distinctUntilChanged().collect { page -> weeks.getOrNull(page)?.let { onWeekSelected(it.startDate) } }
    }
    val displayedPage = if (pagerState.isScrollInProgress) pagerState.targetPage else selectedIndex
    val displayedWeek = weeks[displayedPage.coerceIn(0, weeks.lastIndex)]
    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { weeks.getOrNull(displayedPage - 1)?.let { onWeekSelected(it.startDate) } }, enabled = displayedPage > 0) {
                Icon(Icons.Rounded.ChevronLeft, "Попередній тиждень")
            }
            Text(
                "${weekRangeText(displayedWeek.startDate, displayedWeek.endDate)} · Тиждень ${displayedWeek.weekNumber}",
                Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium,
            )
            IconButton(onClick = { weeks.getOrNull(displayedPage + 1)?.let { onWeekSelected(it.startDate) } }, enabled = displayedPage < weeks.lastIndex) {
                Icon(Icons.Rounded.ChevronRight, "Наступний тиждень")
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().testTag("weekPager"),
            beyondViewportPageCount = 1,
            key = { weeks[it].startDate.toEpochDay() },
        ) { page ->
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(weeks[page].days, key = { it.date.toEpochDay() }, contentType = { "scheduleDay" }) { day ->
                    WeekDayCard(day, day.date == now.toLocalDate(), day.date in expandedDates, now, { onToggleDay(day.date) }, onLessonClick)
                }
                item { androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = 76.dp)) }
            }
        }
    }
}
