package com.kyivsec.duikt_timetable.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kyivsec.duikt_timetable.R
import com.kyivsec.duikt_timetable.model.ScheduleMode

@Composable
fun ScheduleModeSelector(selectedMode: ScheduleMode, onModeSelected: (ScheduleMode) -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth().height(54.dp)) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(27.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {}
        val indicatorOffset by animateDpAsState(
            targetValue = if (selectedMode == ScheduleMode.DAY) 6.dp else maxWidth / 2,
            animationSpec = tween(220),
            label = "modeIndicatorOffset",
        )
        Surface(
            modifier = Modifier.width(maxWidth / 2 - 6.dp).height(42.dp).align(Alignment.CenterStart)
                .offset(x = indicatorOffset),
            shape = RoundedCornerShape(21.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {}
        Row(Modifier.fillMaxSize().selectableGroup()) {
            ModeOption(stringResource(R.string.day), ScheduleMode.DAY, selectedMode, onModeSelected, Modifier.weight(1f))
            ModeOption(stringResource(R.string.week), ScheduleMode.WEEK, selectedMode, onModeSelected, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ModeOption(label: String, mode: ScheduleMode, selectedMode: ScheduleMode, onClick: (ScheduleMode) -> Unit, modifier: Modifier) {
    val selected = mode == selectedMode
    val interactionSource = remember { MutableInteractionSource() }
    val content by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(200), label = "modeContent",
    )
    Box(
        modifier = modifier.testTag("scheduleMode:${mode.name}").fillMaxHeight()
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = null,
                role = Role.RadioButton,
                onClick = { onClick(mode) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            textAlign = TextAlign.Center,
            color = content,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
