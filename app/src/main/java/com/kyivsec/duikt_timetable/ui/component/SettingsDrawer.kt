package com.kyivsec.duikt_timetable.ui.component

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
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.kyivsec.duikt_timetable.R
import com.kyivsec.duikt_timetable.model.ScheduleSettings
import com.kyivsec.duikt_timetable.model.ScheduleMode
import com.kyivsec.duikt_timetable.model.ThemeMode
import com.kyivsec.duikt_timetable.util.AppLanguage
import com.kyivsec.duikt_timetable.util.LanguageHandler
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun SettingsDrawer(
    settings: ScheduleSettings, isFullReloading: Boolean, lastRefreshEpochMillis: Long?,
    selectedLanguage: AppLanguage, onLanguageChange: (AppLanguage) -> Unit,
    onThemeChange: (ThemeMode) -> Unit, onStartupModeChange: (ScheduleMode) -> Unit,
    onHideClassesInWeekViewChange: (Boolean) -> Unit,
    onImmediateNotificationsChange: (Boolean) -> Unit,
    onPersistentNotificationChange: (Boolean) -> Unit,
    onFastUpdateChange: (Boolean) -> Unit,
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
    val themeOptions = ThemeMode.entries.map { themeMode ->
        themeMode to when (themeMode) {
            ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
            ThemeMode.LIGHT -> stringResource(R.string.theme_light)
            ThemeMode.DARK -> stringResource(R.string.theme_dark)
            ThemeMode.HIGH_CONTRAST_LIGHT -> stringResource(R.string.theme_high_contrast_light)
            ThemeMode.HIGH_CONTRAST_DARK -> stringResource(R.string.theme_high_contrast_dark)
            ThemeMode.OLED -> stringResource(R.string.theme_oled)
        }
    }
    val selectedThemeOption = themeOptions.first { it.first == settings.themeMode }
    ModalDrawerSheet(
        modifier = Modifier.fillMaxHeight().fillMaxWidth(0.88f),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.fillMaxHeight()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {
                Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(20.dp))
                SectionTitle(stringResource(R.string.appearance))
                DropdownSelector(
                    label = stringResource(R.string.theme),
                    selected = selectedThemeOption,
                    options = themeOptions,
                    optionLabel = { it.second },
                    onSelected = { onThemeChange(it.first) },
                )
                Spacer(Modifier.height(10.dp))
                DropdownSelector(
                    label = stringResource(R.string.language),
                    selected = selectedLanguageOption,
                    options = languageOptions,
                    optionLabel = { it.second },
                    onSelected = { onLanguageChange(it.first) },
                )
                Spacer(Modifier.height(28.dp))
                SectionTitle(stringResource(R.string.schedule_and_navigation))
                Text(stringResource(R.string.startup_screen), style = MaterialTheme.typography.bodyMedium)
                RadioChoice(stringResource(R.string.day), settings.startupMode == ScheduleMode.DAY) { onStartupModeChange(ScheduleMode.DAY) }
                RadioChoice(stringResource(R.string.week), settings.startupMode == ScheduleMode.WEEK) { onStartupModeChange(ScheduleMode.WEEK) }
                CheckboxChoice(stringResource(R.string.hide_classes_in_week_view), settings.hideClassesInWeekView, onHideClassesInWeekViewChange)
                IntegerSlider(stringResource(R.string.previous_days), settings.previousDaysToKeep, 0..30, onPreviousDaysChange)
                IntegerSlider(stringResource(R.string.previous_weeks), settings.previousWeeksToKeep, 0..12, onPreviousWeeksChange)
                Spacer(Modifier.height(28.dp))
                SectionTitle(stringResource(R.string.notifications))
                CheckboxChoice(
                    stringResource(R.string.immediate_notifications),
                    settings.immediateNotificationsEnabled,
                    onImmediateNotificationsChange,
                )
                Text(
                    stringResource(R.string.immediate_notifications_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 48.dp, bottom = 8.dp),
                )
                CheckboxChoice(
                    stringResource(R.string.persistent_notification),
                    settings.persistentNotificationEnabled,
                    onPersistentNotificationChange,
                )
                Text(
                    stringResource(R.string.persistent_notification_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 48.dp),
                )
                Spacer(Modifier.height(28.dp))
                SectionTitle(stringResource(R.string.data))
                CheckboxChoice(stringResource(R.string.fast_update), settings.fastUpdate, onFastUpdateChange)
                Button(
                    onClick = onFullReload,
                    enabled = !isFullReloading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = MaterialTheme.colorScheme.primary,
                        disabledContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
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
                Spacer(Modifier.height(24.dp))
                Row(
                    Modifier.fillMaxWidth().clickable { uriHandler.openUri(PrivacyPolicyUrl) }.padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.PrivacyTip, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.privacy_policy), style = MaterialTheme.typography.bodyLarge)
                }
                Row(
                    Modifier.fillMaxWidth().clickable { uriHandler.openUri(RepositoryUrl) }.padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(painterResource(R.drawable.ic_github), null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.width(12.dp))
                    Text("GitHub", style = MaterialTheme.typography.bodyLarge)
                }
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
private const val PrivacyPolicyUrl = "https://raw.githubusercontent.com/KyivSec/DuiktTimetable/play-store-privacy/PRIVACY_POLICY.md"

@Composable private fun SectionTitle(text: String) = Text(
    text,
    style = MaterialTheme.typography.titleMedium,
    color = MaterialTheme.colorScheme.onSurface,
    modifier = Modifier.padding(bottom = 10.dp),
)

@Composable private fun RadioChoice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(
            selected = selected,
            onClick = onClick,
        )
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

@Composable private fun IntegerSlider(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    var sliderValue by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text(sliderValue.roundToInt().toString(), style = MaterialTheme.typography.titleSmall)
        }
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = range.last - range.first - 1,
            onValueChangeFinished = { onChange(sliderValue.roundToInt()) },
        )
    }
}
