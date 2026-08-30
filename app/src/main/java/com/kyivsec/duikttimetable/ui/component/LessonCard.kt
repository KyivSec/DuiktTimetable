package com.kyivsec.duikttimetable.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun LessonCard(lesson: Lesson, onClick: () -> Unit, modifier: Modifier = Modifier, isPast: Boolean = false) {
    Card(
        modifier = modifier.graphicsLayer { alpha = if (isPast) 0.58f else 1f }.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(Modifier.fillMaxSize()) {
            androidx.compose.foundation.layout.Box(Modifier.width(5.dp).fillMaxHeight().background(lesson.type.accentColor()))
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(lesson.subject, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${lesson.startTime.format(timeFormatter)} – ${lesson.endTime.format(timeFormatter)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val location = listOfNotNull(lesson.room?.let { "ауд. $it" }, lesson.teacher).joinToString(" · ")
                if (location.isNotBlank()) Text(location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
