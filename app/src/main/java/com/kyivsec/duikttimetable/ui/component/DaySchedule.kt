package com.kyivsec.duikttimetable.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kyivsec.duikttimetable.model.Lesson
import com.kyivsec.duikttimetable.model.ScheduleDay
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
fun DaySchedule(
    dates: List<LocalDate>,
    days: Map<LocalDate, ScheduleDay>,
    selectedDate: LocalDate,
    now: LocalDateTime,
    onDateSelected: (LocalDate) -> Unit,
    onLessonClick: (Lesson) -> Unit,
    modifier: Modifier = Modifier,
    state: PagerState? = null,
    selectorState: androidx.compose.foundation.lazy.LazyListState? = null,
) {
    if (dates.isEmpty()) return
    val selectedIndex = dates.indexOf(selectedDate).coerceAtLeast(0)
    val pagerState = state ?: rememberPagerState(initialPage = selectedIndex) { dates.size }
    var animatedTarget by remember { mutableStateOf<LocalDate?>(null) }
    LaunchedEffect(selectedIndex) {
        if (pagerState.currentPage != selectedIndex) {
            if (animatedTarget == selectedDate) {
                pagerState.animateScrollToPage(selectedIndex, animationSpec = tween(220))
            } else {
                pagerState.scrollToPage(selectedIndex)
            }
        }
        animatedTarget = null
    }
    LaunchedEffect(pagerState, dates) {
        snapshotFlow { pagerState.settledPage }.distinctUntilChanged().collect { page ->
            dates.getOrNull(page)?.let(onDateSelected)
        }
    }
    val visualPage = if (pagerState.isScrollInProgress) pagerState.targetPage else selectedIndex
    val visualDate = dates.getOrNull(visualPage) ?: selectedDate
    Column(modifier.fillMaxSize()) {
        DaySelector(
            dates,
            visualDate,
            now.toLocalDate(),
            onDateSelected = { date ->
                if (date != selectedDate) {
                    animatedTarget = date
                    onDateSelected(date)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            state = selectorState,
        )
        Spacer(Modifier.height(6.dp))
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().testTag("dayPager"),
            beyondViewportPageCount = 1,
            key = { dates[it].toEpochDay() },
        ) { page ->
            val date = dates[page]
            Timeline(days[date] ?: ScheduleDay(date, emptyList()), now, onLessonClick, Modifier.fillMaxSize())
        }
    }
}
