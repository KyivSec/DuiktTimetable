package com.kyivsec.duikttimetable.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.kyivsec.duikttimetable.R

@Composable
fun TopScheduleBar(
    groupName: String,
    isRefreshing: Boolean,
    onMenuClick: () -> Unit,
    onGroupClick: () -> Unit,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onMenuClick) { Icon(Icons.Rounded.Menu, contentDescription = stringResource(R.string.open_settings)) }
        Spacer(Modifier.width(4.dp))
        Column(
            modifier = Modifier.weight(1f).clickable(onClick = onGroupClick).padding(vertical = 2.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(stringResource(R.string.schedule), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(groupName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(Icons.Rounded.KeyboardArrowDown, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        IconButton(onClick = onRefreshClick, enabled = !isRefreshing) {
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
