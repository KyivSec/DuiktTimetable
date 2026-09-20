package com.kyivsec.duikt_timetable.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyivsec.duikt_timetable.R

@Composable
fun TopScheduleBar(
    groupName: String,
    isRefreshing: Boolean,
    onMenuClick: () -> Unit,
    onGroupClick: () -> Unit,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val controlColors = IconButtonDefaults.iconButtonColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        disabledContentColor = MaterialTheme.colorScheme.onSurface,
    )
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onMenuClick, modifier = Modifier.size(48.dp), colors = controlColors) {
            Icon(Icons.Rounded.Menu, contentDescription = stringResource(R.string.open_settings))
        }
        Spacer(Modifier.width(8.dp))
        Row(
            modifier = Modifier.weight(1f).heightIn(min = 48.dp).clickable(onClick = onGroupClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                groupName,
                modifier = Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(Icons.Rounded.KeyboardArrowDown, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        IconButton(
            onClick = onRefreshClick,
            modifier = Modifier.size(48.dp),
            enabled = !isRefreshing,
            colors = controlColors,
        ) {
            if (isRefreshing) SpinningTopRefreshIcon() else Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.refresh))
        }
    }
}

@Composable
private fun SpinningTopRefreshIcon() {
    val transition = rememberInfiniteTransition(label = "refresh")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(650, easing = LinearEasing), RepeatMode.Restart),
        label = "refreshRotation",
    )
    Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.refreshing), modifier = Modifier.rotate(rotation))
}
