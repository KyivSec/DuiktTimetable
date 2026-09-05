package com.kyivsec.duikttimetable

import android.app.Application
import androidx.room.Room
import com.kyivsec.duikttimetable.data.GroupDirectoryRepository
import com.kyivsec.duikttimetable.data.PreferencesRepository
import com.kyivsec.duikttimetable.data.RoomGroupDirectoryRepository
import com.kyivsec.duikttimetable.data.RoomScheduleRepository
import com.kyivsec.duikttimetable.data.ScheduleRepository
import com.kyivsec.duikttimetable.data.SettingsRepository
import com.kyivsec.duikttimetable.data.TeacherDirectoryRepository
import com.kyivsec.duikttimetable.data.RoomTeacherDirectoryRepository
import com.kyivsec.duikttimetable.data.StudentDirectoryRepository
import com.kyivsec.duikttimetable.data.RoomStudentDirectoryRepository
import com.kyivsec.duikttimetable.data.local.TimetableDatabase
import com.kyivsec.duikttimetable.data.remote.DuiktScheduleClient

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
