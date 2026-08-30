package com.kyivsec.duikttimetable.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kyivsec.duikttimetable.model.ScheduleSettings
import com.kyivsec.duikttimetable.model.ThemeMode
import com.kyivsec.duikttimetable.model.GroupInfo
import kotlinx.coroutines.flow.first

private val Context.scheduleDataStore by preferencesDataStore(name = "schedule_preferences")

data class StoredPreferences(
    val settings: ScheduleSettings = ScheduleSettings(),
    val selectedGroupId: Long? = null,
    val selectedInstituteId: Long? = null,
    val selectedCourse: Int? = null,
    val selectedGroupName: String? = null,
    val lastFullRefreshEpochMillis: Long? = null,
)

interface SettingsRepository {
    suspend fun load(): StoredPreferences
    suspend fun saveSettings(settings: ScheduleSettings)
    suspend fun saveSelectedGroup(group: GroupInfo)
    suspend fun saveLastFullRefresh(epochMillis: Long)
}

class PreferencesRepository(private val context: Context) : SettingsRepository {
    override suspend fun load(): StoredPreferences {
        val values = context.scheduleDataStore.data.first()
        return StoredPreferences(
            settings = ScheduleSettings(
                themeMode = values[Keys.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
                previousDaysToKeep = values[Keys.previousDays] ?: 14,
                previousWeeksToKeep = values[Keys.previousWeeks] ?: 4,
            ),
            selectedGroupId = values[Keys.groupId],
            selectedInstituteId = values[Keys.instituteId],
            selectedCourse = values[Keys.course],
            selectedGroupName = values[Keys.groupName],
            lastFullRefreshEpochMillis = values[Keys.lastRefresh],
        )
    }

    override suspend fun saveSettings(settings: ScheduleSettings) { context.scheduleDataStore.edit {
        it[Keys.theme] = settings.themeMode.name
        it[Keys.previousDays] = settings.previousDaysToKeep
        it[Keys.previousWeeks] = settings.previousWeeksToKeep
    } }

    override suspend fun saveSelectedGroup(group: GroupInfo) { context.scheduleDataStore.edit {
        it[Keys.groupId] = group.id
        it[Keys.instituteId] = group.instituteId
        it[Keys.course] = group.course
        it[Keys.groupName] = group.name
    } }
    override suspend fun saveLastFullRefresh(epochMillis: Long) { context.scheduleDataStore.edit { it[Keys.lastRefresh] = epochMillis } }

    private object Keys {
        val theme = stringPreferencesKey("theme")
        val previousDays = intPreferencesKey("previous_days")
        val previousWeeks = intPreferencesKey("previous_weeks")
        val groupId = longPreferencesKey("group_id")
        val instituteId = longPreferencesKey("institute_id")
        val course = intPreferencesKey("selected_course")
        val groupName = stringPreferencesKey("group_name")
        val lastRefresh = longPreferencesKey("last_refresh")
    }
}
