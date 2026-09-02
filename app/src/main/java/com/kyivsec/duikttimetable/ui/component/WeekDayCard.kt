package com.kyivsec.duikttimetable.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.platform.LocalConfiguration
import com.kyivsec.duikttimetable.R
import com.kyivsec.duikttimetable.model.Lesson
import com.kyivsec.duikttimetable.model.ScheduleDay
import com.kyivsec.duikttimetable.util.dayMonth
import com.kyivsec.duikttimetable.util.fullDayName
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime

@Composable
fun WeekDayCard(
    day: ScheduleDay,
    isToday: Boolean,
    expanded: Boolean,
    now: LocalDateTime,
    onToggle: () -> Unit,
    onLessonClick: (Lesson) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = if (isToday) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(enabled = day.lessons.isNotEmpty(), onClick = onToggle)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${day.date.fullDayName(locale)} · ${day.date.dayMonth(locale)}",
                    modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall,
                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
                if (day.lessons.isEmpty()) {
                    Text(stringResource(R.string.no_lessons), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text(pluralStringResource(R.plurals.lesson_count, day.lessons.size, day.lessons.size), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, Modifier.size(20.dp))
                }
            }
            AnimatedVisibility(
                visible = expanded && day.lessons.isNotEmpty(),
                enter = fadeIn(tween(110)) + expandVertically(tween(170), expandFrom = Alignment.Top),
                exit = fadeOut(tween(90)) + shrinkVertically(tween(150), shrinkTowards = Alignment.Top),
            ) {
                Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    day.lessons.forEach { lesson ->
                        val isPast = day.date.isBefore(now.toLocalDate()) ||
                            (day.date == now.toLocalDate() && !lesson.endTime.isAfter(now.toLocalTime()))
                        WeekLessonRow(lesson, isPast) { onLessonClick(lesson) }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekLessonRow(lesson: Lesson, isPast: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().graphicsLayer { alpha = if (isPast) 0.55f else 1f }
            .clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ClassNumberBadge(lesson)
        Spacer(Modifier.width(10.dp))
        Text("${lesson.startTime.format(weekTimeFormatter)} – ${lesson.endTime.format(weekTimeFormatter)}", Modifier.width(98.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(lesson.subject, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        lesson.room?.let { Text(stringResource(R.string.room_short, it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1) }
    }
}

private val weekTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
