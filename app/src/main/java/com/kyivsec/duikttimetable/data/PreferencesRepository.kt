package com.kyivsec.duikttimetable.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kyivsec.duikttimetable.model.ScheduleSettings
import com.kyivsec.duikttimetable.model.ScheduleMode
import com.kyivsec.duikttimetable.model.ThemeMode
import com.kyivsec.duikttimetable.model.GroupInfo
import com.kyivsec.duikttimetable.model.Occupation
import com.kyivsec.duikttimetable.model.TeacherInfo
import com.kyivsec.duikttimetable.model.TimetableOwner
import com.kyivsec.duikttimetable.model.StudentInfo
import kotlinx.coroutines.flow.first

private val Context.scheduleDataStore by preferencesDataStore(name = "schedule_preferences")

data class StoredPreferences(
    val settings: ScheduleSettings = ScheduleSettings(),
    val selectedGroupId: Long? = null,
    val selectedInstituteId: Long? = null,
    val selectedCourse: Int? = null,
    val selectedGroupName: String? = null,
    val occupation: Occupation? = null,
    val selectedTeacherId: Long? = null,
    val selectedChairId: Long? = null,
    val selectedTeacherName: String? = null,
    val selectedChairName: String? = null,
    val selectedStudentId: Long? = null,
    val selectedStudentName: String? = null,
    val selectedStudentGroupId: Long? = null,
    val selectedStudentGroupName: String? = null,
    val selectedStudentInstituteId: Long? = null,
    val selectedStudentInstituteName: String? = null,
    val selectedStudentCourse: Int? = null,
    val lastFullRefreshEpochMillis: Long? = null,
)

interface SettingsRepository {
    suspend fun load(): StoredPreferences
    suspend fun saveSettings(settings: ScheduleSettings)
    suspend fun saveSelectedGroup(group: GroupInfo)
    suspend fun saveSelectedTeacher(teacher: TeacherInfo) = Unit
    suspend fun saveSelectedStudent(student: StudentInfo) = Unit
    suspend fun saveOccupation(occupation: Occupation) = Unit
    suspend fun saveSelectedOwner(owner: TimetableOwner) {
        saveOccupation(owner.type)
        when (owner) {
            is TimetableOwner.Group -> saveSelectedGroup(owner.group)
            is TimetableOwner.Student -> saveSelectedStudent(owner.student)
            is TimetableOwner.Teacher -> saveSelectedTeacher(owner.teacher)
        }
    }
    suspend fun saveLastFullRefresh(epochMillis: Long)
}

class PreferencesRepository(private val context: Context) : SettingsRepository {
    override suspend fun load(): StoredPreferences {
        val values = context.scheduleDataStore.data.first()
        val storedOccupation = values[Keys.occupation]?.let { runCatching { Occupation.valueOf(it) }.getOrNull() }
            ?: if (values[Keys.groupId] != null) Occupation.GROUP else null
        return StoredPreferences(
            settings = ScheduleSettings(
                themeMode = values[Keys.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
                startupMode = values[Keys.startupMode]?.let { runCatching { ScheduleMode.valueOf(it) }.getOrNull() } ?: ScheduleMode.DAY,
                hideClassesInWeekView = values[Keys.hideClassesInWeekView] ?: false,
                fastUpdate = values[Keys.fastUpdate] ?: false,
                previousDaysToKeep = values[Keys.previousDays] ?: 14,
                previousWeeksToKeep = values[Keys.previousWeeks] ?: 4,
            ),
            selectedGroupId = values[Keys.groupId],
            selectedInstituteId = values[Keys.instituteId],
            selectedCourse = values[Keys.course],
            selectedGroupName = values[Keys.groupName],
            occupation = storedOccupation,
            selectedTeacherId = values[Keys.teacherId],
            selectedChairId = values[Keys.chairId],
            selectedTeacherName = values[Keys.teacherName],
            selectedChairName = values[Keys.chairName],
            selectedStudentId = values[Keys.studentId],
            selectedStudentName = values[Keys.studentName],
            selectedStudentGroupId = values[Keys.studentGroupId],
            selectedStudentGroupName = values[Keys.studentGroupName],
            selectedStudentInstituteId = values[Keys.studentInstituteId],
            selectedStudentInstituteName = values[Keys.studentInstituteName],
            selectedStudentCourse = values[Keys.studentCourse],
            lastFullRefreshEpochMillis = values[Keys.lastRefresh],
        )
    }

    override suspend fun saveSettings(settings: ScheduleSettings) { context.scheduleDataStore.edit {
        it[Keys.theme] = settings.themeMode.name
        it[Keys.startupMode] = settings.startupMode.name
        it[Keys.hideClassesInWeekView] = settings.hideClassesInWeekView
        it[Keys.fastUpdate] = settings.fastUpdate
        it[Keys.previousDays] = settings.previousDaysToKeep
        it[Keys.previousWeeks] = settings.previousWeeksToKeep
    } }

    override suspend fun saveSelectedGroup(group: GroupInfo) { context.scheduleDataStore.edit {
        it[Keys.groupId] = group.id
        it[Keys.instituteId] = group.instituteId
        it[Keys.course] = group.course
        it[Keys.groupName] = group.name
    } }
    override suspend fun saveSelectedTeacher(teacher: TeacherInfo) { context.scheduleDataStore.edit {
        it[Keys.teacherId] = teacher.id
        it[Keys.chairId] = teacher.chairId
        it[Keys.teacherName] = teacher.name
        it[Keys.chairName] = teacher.chairName
    } }
    override suspend fun saveSelectedStudent(student: StudentInfo) { context.scheduleDataStore.edit {
        it[Keys.studentId] = student.id
        it[Keys.studentName] = student.name
        it[Keys.studentGroupId] = student.groupId
        it[Keys.studentGroupName] = student.groupName
        it[Keys.studentInstituteId] = student.instituteId
        it[Keys.studentInstituteName] = student.instituteName
        it[Keys.studentCourse] = student.course
    } }
    override suspend fun saveOccupation(occupation: Occupation) { context.scheduleDataStore.edit { it[Keys.occupation] = occupation.name } }
    override suspend fun saveLastFullRefresh(epochMillis: Long) { context.scheduleDataStore.edit { it[Keys.lastRefresh] = epochMillis } }

    private object Keys {
        val theme = stringPreferencesKey("theme")
        val startupMode = stringPreferencesKey("startup_mode")
        val hideClassesInWeekView = booleanPreferencesKey("hide_classes_in_week_view")
        val fastUpdate = booleanPreferencesKey("fast_update")
        val previousDays = intPreferencesKey("previous_days")
        val previousWeeks = intPreferencesKey("previous_weeks")
        val groupId = longPreferencesKey("group_id")
        val instituteId = longPreferencesKey("institute_id")
        val course = intPreferencesKey("selected_course")
        val groupName = stringPreferencesKey("group_name")
        val occupation = stringPreferencesKey("occupation")
        val teacherId = longPreferencesKey("teacher_id")
        val chairId = longPreferencesKey("chair_id")
        val teacherName = stringPreferencesKey("teacher_name")
        val chairName = stringPreferencesKey("chair_name")
        val studentId = longPreferencesKey("student_id")
        val studentName = stringPreferencesKey("student_name")
        val studentGroupId = longPreferencesKey("student_group_id")
        val studentGroupName = stringPreferencesKey("student_group_name")
        val studentInstituteId = longPreferencesKey("student_institute_id")
        val studentInstituteName = stringPreferencesKey("student_institute_name")
        val studentCourse = intPreferencesKey("student_course")
        val lastRefresh = longPreferencesKey("last_refresh")
    }
}
