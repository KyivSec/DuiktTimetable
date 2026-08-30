package com.kyivsec.duikttimetable.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kyivsec.duikttimetable.model.Lesson
import com.kyivsec.duikttimetable.model.LessonType
import java.time.format.DateTimeFormatter
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonDetailsSheet(lesson: Lesson, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text(lesson.subject, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(lesson.type.ukrainianName(), color = lesson.type.accentColor(), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            DetailRow(Icons.Rounded.AccessTime, "Час", "${lesson.startTime.format(timeFormatter)} – ${lesson.endTime.format(timeFormatter)}")
            lesson.room?.let { DetailRow(Icons.Rounded.MeetingRoom, "Аудиторія", it) }
            lesson.building?.let { DetailRow(Icons.Rounded.Business, "Корпус", it) }
            lesson.teacher?.let { DetailRow(Icons.Rounded.Person, "Викладач", it) }
            lesson.subgroup?.let { DetailRow(Icons.Rounded.Groups, "Підгрупа", it) }
            lesson.sourceGroups?.let { DetailRow(Icons.Rounded.Groups, "Групи", it) }
            lesson.rawType?.let { DetailRow(Icons.Rounded.Info, "Тип у джерелі", it) }
            lesson.chairName?.let { DetailRow(Icons.Rounded.Business, "Кафедра", it) }
            lesson.notes?.let { DetailRow(Icons.Rounded.Info, "Примітка", it) }
            lesson.onlineUrl?.let { DetailRow(Icons.Rounded.Link, "Онлайн-посилання", it) }
            lesson.sourceUpdatedAt?.let {
                DetailRow(Icons.Rounded.Info, "Оновлено у джерелі", sourceUpdatedFormatter.format(it.atZone(ZoneId.systemDefault())))
            }
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val sourceUpdatedFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

fun LessonType.ukrainianName(): String = when (this) {
    LessonType.LECTURE -> "Лекція"
    LessonType.PRACTICE -> "Практична робота"
    LessonType.LAB -> "Лабораторна робота"
    LessonType.SEMINAR -> "Семінар"
    LessonType.EXAM -> "Іспит"
    LessonType.OTHER -> "Інше"
}
