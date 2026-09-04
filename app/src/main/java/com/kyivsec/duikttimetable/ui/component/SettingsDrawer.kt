package com.kyivsec.duikttimetable.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.kyivsec.duikttimetable.R
import com.kyivsec.duikttimetable.model.ScheduleSettings
import com.kyivsec.duikttimetable.model.ScheduleMode
import com.kyivsec.duikttimetable.model.ThemeMode
import com.kyivsec.duikttimetable.util.AppLanguage
import com.kyivsec.duikttimetable.util.LanguageHandler
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SettingsDrawer(
    settings: ScheduleSettings, isFullReloading: Boolean, lastRefreshEpochMillis: Long?,
    selectedLanguage: AppLanguage, onLanguageChange: (AppLanguage) -> Unit,
    onThemeChange: (ThemeMode) -> Unit, onStartupModeChange: (ScheduleMode) -> Unit,
    onHideClassesInWeekViewChange: (Boolean) -> Unit,
    onPreviousDaysChange: (Int) -> Unit, onPreviousWeeksChange: (Int) -> Unit, onFullReload: () -> Unit,
    onChangeOccupation: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val locale = LocalConfiguration.current.locales[0]
    val languageOptions = LanguageHandler.availableLanguages.map { language ->
        language to when (language) {
            AppLanguage.SYSTEM -> stringResource(R.string.language_system)
            AppLanguage.UKRAINIAN -> stringResource(R.string.language_ukrainian)
            AppLanguage.ENGLISH -> stringResource(R.string.language_english)
        }
    }
    val selectedLanguageOption = languageOptions.first { it.first == selectedLanguage }
    ModalDrawerSheet(Modifier.fillMaxHeight().fillMaxWidth(0.88f)) {
        Column(Modifier.fillMaxHeight()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {
                Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(20.dp))
                SectionTitle(stringResource(R.string.appearance))
                Text(stringResource(R.string.theme), style = MaterialTheme.typography.bodyMedium)
                RadioChoice(stringResource(R.string.theme_system), settings.themeMode == ThemeMode.SYSTEM) { onThemeChange(ThemeMode.SYSTEM) }
                RadioChoice(stringResource(R.string.theme_light), settings.themeMode == ThemeMode.LIGHT) { onThemeChange(ThemeMode.LIGHT) }
                RadioChoice(stringResource(R.string.theme_dark), settings.themeMode == ThemeMode.DARK) { onThemeChange(ThemeMode.DARK) }
                Spacer(Modifier.height(10.dp))
                DropdownSelector(
                    label = stringResource(R.string.language),
                    selected = selectedLanguageOption,
                    options = languageOptions,
                    optionLabel = { it.second },
                    onSelected = { onLanguageChange(it.first) },
                )
                HorizontalDivider(Modifier.padding(vertical = 14.dp))
                SectionTitle(stringResource(R.string.schedule_and_navigation))
                Text(stringResource(R.string.startup_screen), style = MaterialTheme.typography.bodyMedium)
                RadioChoice(stringResource(R.string.day), settings.startupMode == ScheduleMode.DAY) { onStartupModeChange(ScheduleMode.DAY) }
                RadioChoice(stringResource(R.string.week), settings.startupMode == ScheduleMode.WEEK) { onStartupModeChange(ScheduleMode.WEEK) }
                CheckboxChoice(stringResource(R.string.hide_classes_in_week_view), settings.hideClassesInWeekView, onHideClassesInWeekViewChange)
                Stepper(stringResource(R.string.previous_days), settings.previousDaysToKeep, 0..30, onPreviousDaysChange)
                Stepper(stringResource(R.string.previous_weeks), settings.previousWeeksToKeep, 0..12, onPreviousWeeksChange)
                HorizontalDivider(Modifier.padding(vertical = 14.dp))
                SectionTitle(stringResource(R.string.data))
                Button(onClick = onFullReload, enabled = !isFullReloading, modifier = Modifier.fillMaxWidth()) {
                    ReloadIcon(isFullReloading)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isFullReloading) stringResource(R.string.reload_in_progress) else stringResource(R.string.reload_all))
                }
                Text(
                    lastRefreshEpochMillis?.let {
                        val value = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())
                        stringResource(R.string.last_full_refresh, value.format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", locale)))
                    } ?: stringResource(R.string.never_fully_refreshed),
                    Modifier.padding(top = 10.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                androidx.compose.material3.TextButton(onClick = onChangeOccupation, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.change_occupation))
                }
            }
            HorizontalDivider()
            Row(
                Modifier.fillMaxWidth().clickable { uriHandler.openUri(RepositoryUrl) }.padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.ic_github), null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(12.dp))
                Text("GitHub", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun ReloadIcon(spinning: Boolean) {
    if (!spinning) {
        Icon(Icons.Rounded.Refresh, null, Modifier.size(20.dp))
        return
    }
    val transition = rememberInfiniteTransition(label = "fullReload")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(650, easing = LinearEasing), RepeatMode.Restart),
        label = "fullReloadRotation",
    )
    Icon(Icons.Rounded.Refresh, null, Modifier.size(20.dp).rotate(rotation))
}

private const val RepositoryUrl = "https://github.com/KyivSec/DuiktTimetable"

@Composable private fun SectionTitle(text: String) = Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))

@Composable private fun RadioChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected, onClick)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable private fun CheckboxChoice(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable private fun Stepper(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        IconButton(onClick = { onChange(value - 1) }, enabled = value > range.first) { Icon(Icons.Rounded.Remove, stringResource(R.string.decrease)) }
        Text(value.toString(), style = MaterialTheme.typography.titleSmall)
        IconButton(onClick = { onChange(value + 1) }, enabled = value < range.last) { Icon(Icons.Rounded.Add, stringResource(R.string.increase)) }
    }
}
