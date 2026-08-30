package com.kyivsec.duikttimetable.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [FacultyEntity::class, CourseEntity::class, GroupEntity::class, LessonEntity::class, CachedScheduleDayEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class TimetableDatabase : RoomDatabase() {
    abstract fun timetableDao(): TimetableDao
}
