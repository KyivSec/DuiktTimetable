package com.kyivsec.duikttimetable.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import com.kyivsec.duikttimetable.R
import com.kyivsec.duikttimetable.model.ScheduleMode

@Composable
fun ScheduleModeSelector(selectedMode: ScheduleMode, onModeSelected: (ScheduleMode) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(Modifier.padding(3.dp)) {
            ModeOption(stringResource(R.string.day), ScheduleMode.DAY, selectedMode, onModeSelected, Modifier.weight(1f))
            ModeOption(stringResource(R.string.week), ScheduleMode.WEEK, selectedMode, onModeSelected, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ModeOption(label: String, mode: ScheduleMode, selectedMode: ScheduleMode, onClick: (ScheduleMode) -> Unit, modifier: Modifier) {
    val selected = mode == selectedMode
    val background by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        tween(200), label = "modeBackground",
    )
    val content by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(200), label = "modeContent",
    )
    Text(
        text = label,
        modifier = modifier.testTag("scheduleMode:${mode.name}").clip(RoundedCornerShape(20.dp))
            .background(background).clickable { onClick(mode) }.padding(vertical = 9.dp),
        textAlign = TextAlign.Center,
        color = content,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        style = MaterialTheme.typography.labelLarge,
    )
}
