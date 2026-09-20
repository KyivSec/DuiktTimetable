package com.kyivsec.duikt_timetable.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kyivsec.duikt_timetable.R
import com.kyivsec.duikt_timetable.model.Lesson
import com.kyivsec.duikt_timetable.model.LessonType
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonDetailsSheet(lesson: Lesson, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text(lesson.subject, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(lesson.type.nameResource()),
                modifier = Modifier.padding(top = 6.dp),
                color = lesson.type.foregroundAccentColor(),
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.height(24.dp))
            DetailRow(stringResource(R.string.detail_time), "${lesson.startTime.format(timeFormatter)} – ${lesson.endTime.format(timeFormatter)}")
            lesson.room?.let { DetailRow(stringResource(R.string.detail_room), it) }
            lesson.building?.let { DetailRow(stringResource(R.string.detail_building), it) }
            lesson.teacher?.let { DetailRow(stringResource(R.string.detail_teacher), it) }
            lesson.subgroup?.let { DetailRow(stringResource(R.string.detail_subgroup), it) }
            lesson.sourceGroups?.let { DetailRow(stringResource(R.string.detail_groups), it) }
            lesson.chairName?.let { DetailRow(stringResource(R.string.detail_chair), it) }
            lesson.notes?.let { DetailRow(stringResource(R.string.detail_note), it) }
            lesson.onlineUrl?.let { DetailRow(stringResource(R.string.detail_online_link), it) }
            lesson.sourceUpdatedAt?.let {
                DetailRow(stringResource(R.string.detail_source_updated), sourceUpdatedFormatter.format(it.atZone(ZoneId.systemDefault())))
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 9.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, modifier = Modifier.padding(top = 2.dp), style = MaterialTheme.typography.bodyLarge)
    }
}

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val sourceUpdatedFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

@StringRes
fun LessonType.nameResource(): Int = when (this) {
    LessonType.LECTURE -> R.string.lesson_type_lecture
    LessonType.PRACTICE -> R.string.lesson_type_practice
    LessonType.LAB -> R.string.lesson_type_lab
    LessonType.SEMINAR -> R.string.lesson_type_seminar
    LessonType.EXAM -> R.string.lesson_type_exam
    LessonType.OTHER -> R.string.lesson_type_other
}
