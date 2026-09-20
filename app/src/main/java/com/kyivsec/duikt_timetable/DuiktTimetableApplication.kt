package com.kyivsec.duikt_timetable

import android.app.Application
import androidx.room.Room
import com.kyivsec.duikt_timetable.data.GroupDirectoryRepository
import com.kyivsec.duikt_timetable.data.PreferencesRepository
import com.kyivsec.duikt_timetable.data.RoomGroupDirectoryRepository
import com.kyivsec.duikt_timetable.data.RoomScheduleRepository
import com.kyivsec.duikt_timetable.data.ScheduleRepository
import com.kyivsec.duikt_timetable.data.SettingsRepository
import com.kyivsec.duikt_timetable.data.TeacherDirectoryRepository
import com.kyivsec.duikt_timetable.data.RoomTeacherDirectoryRepository
import com.kyivsec.duikt_timetable.data.StudentDirectoryRepository
import com.kyivsec.duikt_timetable.data.RoomStudentDirectoryRepository
import com.kyivsec.duikt_timetable.data.local.TimetableDatabase
import com.kyivsec.duikt_timetable.data.remote.DuiktScheduleClient

class DuiktTimetableApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(application: Application) {
    private val database = Room.databaseBuilder(application, TimetableDatabase::class.java, "duikt_timetable.db")
        .addMigrations(TimetableDatabase.MIGRATION_1_2, TimetableDatabase.MIGRATION_2_3, TimetableDatabase.MIGRATION_3_4, TimetableDatabase.MIGRATION_4_5)
        .build()
    private val client = DuiktScheduleClient()
    val scheduleRepository: ScheduleRepository = RoomScheduleRepository(database, client)
    val groupDirectoryRepository: GroupDirectoryRepository = RoomGroupDirectoryRepository(database, client)
    val teacherDirectoryRepository: TeacherDirectoryRepository = RoomTeacherDirectoryRepository(database, client)
    val studentDirectoryRepository: StudentDirectoryRepository = RoomStudentDirectoryRepository(database, client)
    val settingsRepository: SettingsRepository = PreferencesRepository(application)
}
