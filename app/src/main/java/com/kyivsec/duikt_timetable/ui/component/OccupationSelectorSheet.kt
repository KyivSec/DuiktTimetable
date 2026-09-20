package com.kyivsec.duikt_timetable.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kyivsec.duikt_timetable.R
import com.kyivsec.duikt_timetable.model.Occupation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OccupationSelectorSheet(
    selectedOccupation: Occupation? = null,
    onSelected: (Occupation) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.choose_occupation), style = MaterialTheme.typography.headlineSmall)
            OccupationButton(stringResource(R.string.group), Occupation.GROUP, selectedOccupation, onSelected)
            OccupationButton(stringResource(R.string.student), Occupation.STUDENT, selectedOccupation, onSelected)
            OccupationButton(stringResource(R.string.teacher), Occupation.TEACHER, selectedOccupation, onSelected)
        }
    }
}

@Composable
private fun OccupationButton(
    label: String,
    occupation: Occupation,
    selectedOccupation: Occupation?,
    onSelected: (Occupation) -> Unit,
) {
    val selected = occupation == selectedOccupation
    TextButton(
        onClick = { onSelected(occupation) },
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        colors = ButtonDefaults.textButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = if (selected) 1f else 0f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        contentPadding = PaddingValues(16.dp),
    ) {
        Text(label)
        Spacer(Modifier.weight(1f))
        if (selected) {
            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}
