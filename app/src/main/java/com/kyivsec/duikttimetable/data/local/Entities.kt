package com.kyivsec.duikttimetable.data.local

import androidx.room.Entity
import androidx.room.Index

@Entity(tableName = "faculties")
data class FacultyEntity(@androidx.room.PrimaryKey val id: Long, val name: String, val fetchedAt: Long)

@Entity(tableName = "courses", primaryKeys = ["facultyId", "course"], indices = [Index("facultyId")])
data class CourseEntity(val facultyId: Long, val course: Int, val fetchedAt: Long)

@Entity(
    tableName = "groups",
    indices = [Index(value = ["facultyId", "course"]), Index("name")],
)
data class GroupEntity(
    @androidx.room.PrimaryKey val id: Long,
    val facultyId: Long,
    val course: Int,
    val name: String,
    val fetchedAt: Long,
)

@Entity(tableName = "chairs", indices = [Index("name")])
data class ChairEntity(@androidx.room.PrimaryKey val id: Long, val name: String, val fetchedAt: Long)

@Entity(tableName = "teachers", indices = [Index("chairId"), Index("name")])
data class TeacherEntity(
    @androidx.room.PrimaryKey val id: Long,
    val chairId: Long,
    val name: String,
    val fetchedAt: Long,
)

@Entity(tableName = "students", indices = [Index("groupId"), Index("name")])
data class StudentEntity(
    @androidx.room.PrimaryKey val id: Long,
    val groupId: Long,
    val groupName: String,
    val facultyId: Long,
    val course: Int,
    val name: String,
    val fetchedAt: Long,
)

@Entity(
    tableName = "lessons",
    indices = [Index(value = ["ownerType", "ownerId", "date", "startMinute"])],
)
data class LessonEntity(
    @androidx.room.PrimaryKey val id: String,
    val ownerType: String,
    val ownerId: Long,
    val date: String,
    val subject: String,
    val shortSubject: String?,
    val lessonType: String,
    val rawType: String?,
    val startMinute: Int,
    val endMinute: Int,
    val room: String?,
    val building: String?,
    val teacher: String?,
    val subgroup: String?,
    val notes: String?,
    val onlineUrl: String?,
    val sourceOccurrenceId: Long?,
    val sourceDisciplineId: Long?,
    val sourceTeacherId: Long?,
    val sourceGroups: String?,
    val chairName: String?,
    val sourceUpdatedAt: Long?,
    val isNonstandardTime: Boolean,
    val notice: String?,
)

@Entity(tableName = "cached_schedule_days", primaryKeys = ["ownerType", "ownerId", "date"], indices = [Index("date")])
data class CachedScheduleDayEntity(val ownerType: String, val ownerId: Long, val date: String, val fetchedAt: Long)

@Entity(tableName = "semester_syncs", primaryKeys = ["ownerType", "ownerId"])
data class SemesterSyncEntity(
    val ownerType: String,
    val ownerId: Long,
    val startDate: String,
    val endDate: String,
    val fetchedAt: Long,
)
