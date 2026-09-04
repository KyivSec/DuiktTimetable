package com.kyivsec.duikttimetable.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.stringResource
import com.kyivsec.duikttimetable.R
import com.kyivsec.duikttimetable.model.Lesson
import com.kyivsec.duikttimetable.model.ScheduleDay
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Duration
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.floor

private val MinuteHeight = 1.dp
private val RailWidth = 72.dp
private val TimelineTopPadding = 16.dp

@Composable
fun Timeline(day: ScheduleDay, now: LocalDateTime, onLessonClick: (Lesson) -> Unit, modifier: Modifier = Modifier) {
    if (day.lessons.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_lessons), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    val firstMinutes = day.lessons.minOf { it.startTime.toMinuteOfDay() }
    val lastMinutes = day.lessons.maxOf { it.endTime.toMinuteOfDay() }
    val startMinutes = minOf(8 * 60, floor(firstMinutes / 60f).toInt() * 60)
    val endMinutes = maxOf(17 * 60, ceil(lastMinutes / 60f).toInt() * 60)
    val totalMinutes = endMinutes - startMinutes
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val selectedIsToday = day.date == now.toLocalDate()
    LaunchedEffect(day.date) {
        if (selectedIsToday) {
            val nowMinutes = now.toLocalTime().toMinuteOfDay()
            val targetLesson = day.lessons.firstOrNull { now.toLocalTime() in it.startTime..it.endTime }
                ?: day.lessons.firstOrNull { it.startTime > now.toLocalTime() }
                ?: day.lessons.last()
            val targetMinutes = if (nowMinutes in startMinutes..endMinutes) nowMinutes else targetLesson.startTime.toMinuteOfDay()
            val targetPx = with(density) { (TimelineTopPadding + MinuteHeight * (targetMinutes - startMinutes).coerceAtLeast(0)).roundToPx() }
            scrollState.scrollTo((targetPx - with(density) { 140.dp.roundToPx() }).coerceAtLeast(0))
        } else scrollState.scrollTo(0)
    }
    val indicatorMinutes = now.toLocalTime().toMinuteOfDay()
    val showIndicator = selectedIsToday && indicatorMinutes in startMinutes..endMinutes
    val indicatorY = if (showIndicator) {
        val currentLesson = day.lessons.firstOrNull { lesson ->
            !now.toLocalTime().isBefore(lesson.startTime) && !now.toLocalTime().isAfter(lesson.endTime)
        }
        currentLesson?.let { lesson ->
            val duration = lesson.endTime.toMinuteOfDay() - lesson.startTime.toMinuteOfDay()
            val elapsed = Duration.between(lesson.startTime, now.toLocalTime()).toMillis()
            val total = Duration.between(lesson.startTime, lesson.endTime).toMillis().coerceAtLeast(1L)
            val progress = (elapsed.toFloat() / total).coerceIn(0f, 1f)
            val cardTop = TimelineTopPadding + MinuteHeight * (lesson.startTime.toMinuteOfDay() - startMinutes) + 4.dp
            cardTop + lessonCardHeight(duration) * progress
        } ?: TimelineTopPadding + MinuteHeight * (indicatorMinutes - startMinutes)
    } else 0.dp
    Box(modifier.fillMaxSize().verticalScroll(scrollState)) {
        Box(Modifier.fillMaxWidth().height(TimelineTopPadding + MinuteHeight * totalMinutes + 12.dp).padding(end = 12.dp)) {
            val lineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
            for (minute in startMinutes..endMinutes step 60) {
                val offset = TimelineTopPadding + MinuteHeight * (minute - startMinutes)
                Text(
                    text = LocalTime.of(minute / 60 % 24, 0).format(timelineTimeFormatter),
                    modifier = Modifier.offset(y = offset - 8.dp).padding(start = 12.dp).then(Modifier),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                )
                Canvas(Modifier.fillMaxWidth().height(1.dp).offset(y = offset)) {
                    drawLine(lineColor, Offset(RailWidth.toPx(), 0f), Offset(size.width, 0f), 1.dp.toPx())
                }
            }
            day.lessons.forEach { lesson ->
                val top = TimelineTopPadding + MinuteHeight * (lesson.startTime.toMinuteOfDay() - startMinutes) + 4.dp
                val duration = lesson.endTime.toMinuteOfDay() - lesson.startTime.toMinuteOfDay()
                LessonCard(
                    lesson = lesson,
                    onClick = { onLessonClick(lesson) },
                    isPast = day.date.isBefore(now.toLocalDate()) ||
                        (day.date == now.toLocalDate() && !lesson.endTime.isAfter(now.toLocalTime())),
                    modifier = Modifier.padding(start = RailWidth + 4.dp).offset(y = top).fillMaxWidth()
                        .height(lessonCardHeight(duration)),
                )
            }
            if (showIndicator) {
                CurrentTimeIndicator(now.toLocalTime(), indicatorY)
            }
        }
    }
}

@Composable
private fun CurrentTimeIndicator(time: LocalTime, y: Dp) {
    val red = Color(0xFFFF5D5D)
    Box(Modifier.fillMaxWidth().height(28.dp).offset(y = y - 14.dp)) {
        Canvas(Modifier.fillMaxWidth().height(12.dp).align(Alignment.Center)) {
            val railX = RailWidth.toPx()
            drawCircle(red, radius = 5.dp.toPx(), center = Offset(railX - 5.dp.toPx(), size.height / 2))
            drawLine(red, Offset(railX - 5.dp.toPx(), size.height / 2), Offset(size.width, size.height / 2), 1.dp.toPx())
        }
        Text(
            time.format(timelineTimeFormatter),
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 10.dp)
                .background(red, RoundedCornerShape(5.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
        )
    }
}

private fun lessonCardHeight(durationMinutes: Int): Dp = (MinuteHeight * durationMinutes - 8.dp).coerceAtLeast(72.dp)

private fun LocalTime.toMinuteOfDay(): Int = hour * 60 + minute
private val timelineTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
