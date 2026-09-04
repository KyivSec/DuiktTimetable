package com.kyivsec.duikttimetable.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kyivsec.duikttimetable.R
import com.kyivsec.duikttimetable.model.Occupation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OccupationSelectorSheet(onSelected: (Occupation) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.choose_occupation))
            Button({ onSelected(Occupation.GROUP) }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.group)) }
            Button({ onSelected(Occupation.STUDENT) }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.student)) }
            Button({ onSelected(Occupation.TEACHER) }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.teacher)) }
        }
    }
}
