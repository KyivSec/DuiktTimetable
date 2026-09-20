package com.kyivsec.duikt_timetable.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.platform.LocalConfiguration
import com.kyivsec.duikt_timetable.R
import com.kyivsec.duikt_timetable.model.Lesson
import com.kyivsec.duikt_timetable.model.ScheduleDay
import com.kyivsec.duikt_timetable.util.dayMonth
import com.kyivsec.duikt_timetable.util.fullDayName
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
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
                    color = if (isToday) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                )
                if (day.lessons.isEmpty()) {
                    Text(stringResource(R.string.no_lessons), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text(
                        pluralStringResource(R.plurals.lesson_count, day.lessons.size, day.lessons.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isToday) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        null,
                        Modifier.size(20.dp),
                        tint = if (isToday) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            AnimatedVisibility(
                visible = expanded && day.lessons.isNotEmpty(),
                enter = fadeIn(tween(110)) + expandVertically(tween(170), expandFrom = Alignment.Top),
                exit = fadeOut(tween(90)) + shrinkVertically(tween(150), shrinkTowards = Alignment.Top),
            ) {
                Column(Modifier.padding(horizontal = 14.dp)) {
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
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick).padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ClassNumberBadge(lesson)
        Text(
            "${lesson.startTime.format(weekTimeFormatter)} – ${lesson.endTime.format(weekTimeFormatter)}",
            Modifier.width(98.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = if (isPast) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            lesson.subject,
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isPast) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isPast) FontWeight.Normal else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        lesson.room?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
}

private val weekTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
