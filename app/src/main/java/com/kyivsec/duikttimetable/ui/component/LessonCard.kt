package com.kyivsec.duikttimetable.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyivsec.duikttimetable.model.Lesson
import com.kyivsec.duikttimetable.model.LessonType
import com.kyivsec.duikttimetable.ui.theme.ExamAccent
import com.kyivsec.duikttimetable.ui.theme.LabAccent
import com.kyivsec.duikttimetable.ui.theme.LectureAccent
import com.kyivsec.duikttimetable.ui.theme.OtherAccent
import com.kyivsec.duikttimetable.ui.theme.PracticeAccent
import com.kyivsec.duikttimetable.ui.theme.SeminarAccent
import java.time.format.DateTimeFormatter

fun LessonType.accentColor(): Color = when (this) {
    LessonType.LECTURE -> LectureAccent
    LessonType.PRACTICE -> PracticeAccent
    LessonType.LAB -> LabAccent
    LessonType.SEMINAR -> SeminarAccent
    LessonType.EXAM -> ExamAccent
    LessonType.OTHER -> OtherAccent
}

private val classStartTimes = listOf(8 * 60, 9 * 60 + 30, 11 * 60 + 10, 12 * 60 + 40, 14 * 60 + 10)

/** Returns the class number for the standard timetable slot containing this start time. */
internal fun Lesson.classNumber(): Int = classStartTimes.indexOfLast { it <= startTime.hour * 60 + startTime.minute }
    .coerceAtLeast(0) + 1

@Composable
fun LessonCard(lesson: Lesson, onClick: () -> Unit, modifier: Modifier = Modifier, isPast: Boolean = false) {
    Card(
        modifier = modifier.graphicsLayer { alpha = if (isPast) 0.58f else 1f }.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            Modifier.fillMaxSize(),
        ) {
            Box(Modifier.width(5.dp).fillMaxHeight().background(lesson.type.accentColor()))
            Row(
                Modifier.weight(1f).fillMaxHeight().padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(lesson.type.accentColor()),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = lesson.classNumber().toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
                Column(Modifier.padding(start = 10.dp).weight(1f)) {
                    Text(lesson.subject, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${lesson.startTime.format(timeFormatter)} – ${lesson.endTime.format(timeFormatter)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
        }
    }
}

private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
