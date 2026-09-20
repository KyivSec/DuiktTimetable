package com.kyivsec.duikt_timetable.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import com.kyivsec.duikt_timetable.util.shortDayName
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DayItemWidth = 58.dp
private val DayMonthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM")

@Composable
fun DaySelector(
    dates: List<LocalDate>,
    selectedDate: LocalDate,
    today: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyListState? = null,
) {
    val locale = LocalConfiguration.current.locales[0]
    val listState = state ?: rememberLazyListState()
    val selectedIndex = dates.indexOf(selectedDate).coerceAtLeast(0)
    var hasPositioned by remember { mutableStateOf(false) }
    LaunchedEffect(selectedIndex) {
        if (dates.isNotEmpty()) {
            val targetIsVisible = listState.layoutInfo.visibleItemsInfo.any { it.index == selectedIndex }
            if (hasPositioned && targetIsVisible) listState.animateScrollToItem(selectedIndex)
            else listState.scrollToItem(selectedIndex)
            hasPositioned = true
        }
    }
    BoxWithConstraints(modifier) {
        val centerPadding = ((maxWidth - DayItemWidth) / 2).coerceAtLeast(12.dp)
        LazyRow(
            modifier = Modifier,
            state = listState,
            contentPadding = PaddingValues(horizontal = centerPadding),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(dates.size, key = { dates[it].toEpochDay() }) { index ->
                val date = dates[index]
                val selected = date == selectedDate
                val background by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.background,
                    tween(180), label = "dayBackground",
                )
                val content by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    tween(180), label = "dayContent",
                )
                val shape = RoundedCornerShape(15.dp)
                Column(
                    Modifier.size(width = DayItemWidth, height = 64.dp).clip(shape).background(background)
                        .selectable(selected = selected, onClick = { onDateSelected(date) }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(date.shortDayName(locale), style = MaterialTheme.typography.labelMedium, color = content)
                    Text(
                        date.format(DayMonthFormatter), style = MaterialTheme.typography.titleSmall, color = content,
                        fontWeight = if (selected || date == today) FontWeight.Bold else FontWeight.Normal,
                    )
                    if (date == today) {
                        Box(
                            Modifier.size(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary),
                        )
                    }
                }
            }
        }
    }
}
