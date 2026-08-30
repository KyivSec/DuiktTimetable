package com.kyivsec.duikttimetable.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TimetableDao {
    @Query("SELECT * FROM faculties ORDER BY name")
    fun observeFaculties(): Flow<List<FacultyEntity>>
    @Query("SELECT * FROM courses WHERE facultyId = :facultyId ORDER BY course")
    fun observeCourses(facultyId: Long): Flow<List<CourseEntity>>
    @Query("SELECT * FROM groups WHERE facultyId = :facultyId AND course = :course ORDER BY name")
    fun observeGroups(facultyId: Long, course: Int): Flow<List<GroupEntity>>
    @Query("SELECT * FROM groups WHERE id = :id LIMIT 1")
    suspend fun group(id: Long): GroupEntity?
    @Query("SELECT * FROM faculties WHERE id = :id LIMIT 1")
    suspend fun faculty(id: Long): FacultyEntity?
    @Query("SELECT MAX(fetchedAt) FROM faculties")
    suspend fun facultiesFetchedAt(): Long?
    @Query("SELECT MAX(fetchedAt) FROM courses WHERE facultyId = :facultyId")
    suspend fun coursesFetchedAt(facultyId: Long): Long?
    @Query("SELECT MAX(fetchedAt) FROM groups WHERE facultyId = :facultyId AND course = :course")
    suspend fun groupsFetchedAt(facultyId: Long, course: Int): Long?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFaculties(values: List<FacultyEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(values: List<CourseEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(values: List<GroupEntity>)

    @Query("SELECT * FROM lessons WHERE groupId = :groupId AND date BETWEEN :start AND :end ORDER BY date, startMinute, endMinute")
    fun observeLessons(groupId: Long, start: String, end: String): Flow<List<LessonEntity>>
    @Query("SELECT * FROM cached_schedule_days WHERE groupId = :groupId AND date BETWEEN :start AND :end")
    fun observeCachedDays(groupId: Long, start: String, end: String): Flow<List<CachedScheduleDayEntity>>
    @Query("SELECT * FROM cached_schedule_days WHERE groupId = :groupId AND date BETWEEN :start AND :end")
    suspend fun cachedDays(groupId: Long, start: String, end: String): List<CachedScheduleDayEntity>
    @Query("DELETE FROM lessons WHERE groupId = :groupId AND date BETWEEN :start AND :end")
    suspend fun deleteLessons(groupId: Long, start: String, end: String)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLessons(values: List<LessonEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedDays(values: List<CachedScheduleDayEntity>)
    @Query("DELETE FROM lessons WHERE date < :date")
    suspend fun pruneLessons(date: String)
    @Query("DELETE FROM cached_schedule_days WHERE date < :date")
    suspend fun pruneCachedDays(date: String)
}
