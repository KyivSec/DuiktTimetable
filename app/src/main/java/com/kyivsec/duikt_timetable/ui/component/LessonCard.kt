package com.kyivsec.duikt_timetable.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyivsec.duikt_timetable.model.Lesson
import com.kyivsec.duikt_timetable.model.LessonType
import com.kyivsec.duikt_timetable.ui.theme.ExamAccent
import com.kyivsec.duikt_timetable.ui.theme.ExamAccentLightForeground
import com.kyivsec.duikt_timetable.ui.theme.LabAccent
import com.kyivsec.duikt_timetable.ui.theme.LabAccentLightForeground
import com.kyivsec.duikt_timetable.ui.theme.LectureAccent
import com.kyivsec.duikt_timetable.ui.theme.LectureAccentLightForeground
import com.kyivsec.duikt_timetable.ui.theme.OtherAccent
import com.kyivsec.duikt_timetable.ui.theme.OtherAccentLightForeground
import com.kyivsec.duikt_timetable.ui.theme.PracticeAccent
import com.kyivsec.duikt_timetable.ui.theme.PracticeAccentLightForeground
import com.kyivsec.duikt_timetable.ui.theme.SeminarAccent
import com.kyivsec.duikt_timetable.ui.theme.SeminarAccentLightForeground
import java.time.format.DateTimeFormatter

fun LessonType.accentColor(): Color = when (this) {
    LessonType.LECTURE -> LectureAccent
    LessonType.PRACTICE -> PracticeAccent
    LessonType.LAB -> LabAccent
    LessonType.SEMINAR -> SeminarAccent
    LessonType.EXAM -> ExamAccent
    LessonType.OTHER -> OtherAccent
}

@Composable
fun LessonType.foregroundAccentColor(): Color {
    if (MaterialTheme.colorScheme.background.luminance() <= 0.5f) return accentColor()
    return when (this) {
        LessonType.LECTURE -> LectureAccentLightForeground
        LessonType.PRACTICE -> PracticeAccentLightForeground
        LessonType.LAB -> LabAccentLightForeground
        LessonType.SEMINAR -> SeminarAccentLightForeground
        LessonType.EXAM -> ExamAccentLightForeground
        LessonType.OTHER -> OtherAccentLightForeground
    }
}

private val classStartTimes = listOf(8 * 60, 9 * 60 + 30, 11 * 60 + 10, 12 * 60 + 40, 14 * 60 + 10, 15 * 60 + 40)

/** Returns the class number for the standard timetable slot containing this start time. */
internal fun Lesson.classNumber(): Int = classStartTimes.indexOfLast { it <= startTime.hour * 60 + startTime.minute }
    .coerceAtLeast(0) + 1

@Composable
internal fun ClassNumberBadge(lesson: Lesson) {
    Box(
        Modifier.size(22.dp).clip(RoundedCornerShape(5.dp)).background(lesson.type.accentColor()),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = lesson.classNumber().toString(),
            style = MaterialTheme.typography.labelLarge.copy(
                shadow = Shadow(Color.Black.copy(alpha = 0.45f), offset = Offset(0f, 1f), blurRadius = 1f),
            ),
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

@Composable
fun LessonCard(lesson: Lesson, onClick: () -> Unit, modifier: Modifier = Modifier, isPast: Boolean = false) {
    val shape = RoundedCornerShape(12.dp)
    val accent = lesson.type.accentColor()
    Box(modifier.clickable(onClick = onClick)) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = if (isPast) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            Row(
                Modifier.fillMaxSize().padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ClassNumberBadge(lesson)
                Column(Modifier.padding(start = 10.dp).weight(1f)) {
                    Text(
                        lesson.subject,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isPast) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "${lesson.startTime.format(timeFormatter)} – ${lesson.endTime.format(timeFormatter)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = if (isPast) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                        lesson.room?.takeIf { it.isNotBlank() }?.let { room ->
                            Text(room, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        Box(Modifier.fillMaxSize().clip(shape)) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(accent))
        }
    }
}

private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
