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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.kyivsec.duikttimetable.R
import com.kyivsec.duikttimetable.model.GroupInfo
import com.kyivsec.duikttimetable.model.Institute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSelectorSheet(
    institutes: List<Institute>,
    courses: List<Int>,
    groups: List<GroupInfo>,
    draftInstituteId: Long?,
    draftCourse: Int?,
    draftGroup: GroupInfo?,
    isCoursesLoading: Boolean,
    isGroupsLoading: Boolean,
    onInstituteSelected: (Long) -> Unit,
    onCourseSelected: (Int) -> Unit,
    onGroupSelected: (GroupInfo) -> Unit,
    onApply: (GroupInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    val institute = institutes.firstOrNull { it.id == draftInstituteId }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.select_group), style = MaterialTheme.typography.headlineSmall)
            DropdownSelector(stringResource(R.string.institute), institute, institutes, { it.name }, { onInstituteSelected(it.id) })
            LoadingDropdownRow(isCoursesLoading) {
                DropdownSelector(stringResource(R.string.course), draftCourse, courses, Int::toString, onCourseSelected)
            }
            LoadingDropdownRow(isGroupsLoading) {
                DropdownSelector(stringResource(R.string.group), draftGroup, groups, { it.name }, onGroupSelected)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.cancel)) }
                Button(onClick = { draftGroup?.let(onApply) }, enabled = draftGroup != null && !isGroupsLoading, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.apply)) }
            }
        }
    }
}

@Composable
private fun LoadingDropdownRow(loading: Boolean, content: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f)) { content() }
        if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
    }
}
