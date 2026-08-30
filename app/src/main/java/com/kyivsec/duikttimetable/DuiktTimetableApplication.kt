package com.kyivsec.duikttimetable

import android.app.Application
import androidx.room.Room
import com.kyivsec.duikttimetable.data.GroupDirectoryRepository
import com.kyivsec.duikttimetable.data.PreferencesRepository
import com.kyivsec.duikttimetable.data.RoomGroupDirectoryRepository
import com.kyivsec.duikttimetable.data.RoomScheduleRepository
import com.kyivsec.duikttimetable.data.ScheduleRepository
import com.kyivsec.duikttimetable.data.SettingsRepository
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
    private val database = Room.databaseBuilder(application, TimetableDatabase::class.java, "duikt_timetable.db").build()
    private val client = DuiktScheduleClient()
    val scheduleRepository: ScheduleRepository = RoomScheduleRepository(database, client)
    val groupDirectoryRepository: GroupDirectoryRepository = RoomGroupDirectoryRepository(database, client)
    val settingsRepository: SettingsRepository = PreferencesRepository(application)
}
