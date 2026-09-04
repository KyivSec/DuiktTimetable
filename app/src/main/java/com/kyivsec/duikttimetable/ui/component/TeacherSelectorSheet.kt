package com.kyivsec.duikttimetable.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kyivsec.duikttimetable.R
import com.kyivsec.duikttimetable.model.ChairInfo
import com.kyivsec.duikttimetable.model.TeacherInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherSelectorSheet(
    chairs: List<ChairInfo>, teachers: List<TeacherInfo>, draftChairId: Long?, draftTeacher: TeacherInfo?,
    isLoading: Boolean, onChairSelected: (Long) -> Unit, onTeacherSelected: (TeacherInfo) -> Unit,
    onApply: (TeacherInfo) -> Unit, onDismiss: () -> Unit,
) {
    val chair = chairs.firstOrNull { it.id == draftChairId }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.select_teacher), style = MaterialTheme.typography.headlineSmall)
            DropdownSelector(stringResource(R.string.chair), chair, chairs, { it.name }, { onChairSelected(it.id) })
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) { DropdownSelector(stringResource(R.string.teacher), draftTeacher, teachers, { it.name }, onTeacherSelected) }
                if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onDismiss, Modifier.weight(1f)) { Text(stringResource(R.string.cancel)) }
                Button({ draftTeacher?.let(onApply) }, Modifier.weight(1f), enabled = draftTeacher != null && !isLoading) { Text(stringResource(R.string.apply)) }
            }
        }
    }
}
