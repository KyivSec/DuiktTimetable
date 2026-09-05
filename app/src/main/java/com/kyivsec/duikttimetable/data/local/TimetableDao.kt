package com.kyivsec.duikttimetable.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TimetableDao {
    @Query("SELECT fetchedAt FROM directory_syncs WHERE branch = :branch")
    suspend fun directoryFetchedAt(branch: String): Long?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDirectorySync(value: DirectorySyncEntity)

    @Query("DELETE FROM faculties WHERE source = :source")
    suspend fun deleteFaculties(source: String = "GROUP")
    @Query("DELETE FROM courses WHERE source = :source AND facultyId = :facultyId")
    suspend fun deleteCourses(facultyId: Long, source: String = "GROUP")
    @Query("DELETE FROM groups WHERE source = :source AND facultyId = :facultyId AND course = :course")
    suspend fun deleteGroups(facultyId: Long, course: Int, source: String = "GROUP")
    @Query("DELETE FROM chairs")
    suspend fun deleteChairs()
    @Query("DELETE FROM teachers WHERE chairId = :chairId")
    suspend fun deleteTeachers(chairId: Long)
    @Query("DELETE FROM students WHERE groupId = :groupId")
    suspend fun deleteStudents(groupId: Long)

    @Query("SELECT * FROM semester_syncs WHERE ownerType = :ownerType AND ownerId = :ownerId")
    suspend fun semesterSync(ownerType: String, ownerId: Long): SemesterSyncEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSemesterSync(value: SemesterSyncEntity)
    @Query("DELETE FROM semester_syncs WHERE startDate < :date")
    suspend fun invalidatePrunedSemesters(date: String)

    @Query("SELECT * FROM faculties WHERE source = :source ORDER BY name")
    fun observeFaculties(source: String = "GROUP"): Flow<List<FacultyEntity>>
    @Query("SELECT * FROM courses WHERE source = :source AND facultyId = :facultyId ORDER BY course")
    fun observeCourses(facultyId: Long, source: String = "GROUP"): Flow<List<CourseEntity>>
    @Query("SELECT * FROM groups WHERE source = :source AND facultyId = :facultyId AND course = :course ORDER BY name")
    fun observeGroups(facultyId: Long, course: Int, source: String = "GROUP"): Flow<List<GroupEntity>>
    @Query("SELECT * FROM groups WHERE source = :source AND id = :id LIMIT 1")
    suspend fun group(id: Long, source: String = "GROUP"): GroupEntity?
    @Query("SELECT * FROM faculties WHERE source = :source AND id = :id LIMIT 1")
    suspend fun faculty(id: Long, source: String = "GROUP"): FacultyEntity?
    @Query("SELECT MAX(fetchedAt) FROM faculties WHERE source = :source")
    suspend fun facultiesFetchedAt(source: String = "GROUP"): Long?
    @Query("SELECT MAX(fetchedAt) FROM courses WHERE source = :source AND facultyId = :facultyId")
    suspend fun coursesFetchedAt(facultyId: Long, source: String = "GROUP"): Long?
    @Query("SELECT MAX(fetchedAt) FROM groups WHERE source = :source AND facultyId = :facultyId AND course = :course")
    suspend fun groupsFetchedAt(facultyId: Long, course: Int, source: String = "GROUP"): Long?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFaculties(values: List<FacultyEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(values: List<CourseEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(values: List<GroupEntity>)

    @Query("SELECT * FROM chairs ORDER BY name")
    fun observeChairs(): Flow<List<ChairEntity>>
    @Query("SELECT * FROM teachers WHERE chairId = :chairId ORDER BY name")
    fun observeTeachers(chairId: Long): Flow<List<TeacherEntity>>
    @Query("SELECT MAX(fetchedAt) FROM chairs")
    suspend fun chairsFetchedAt(): Long?
    @Query("SELECT MAX(fetchedAt) FROM teachers WHERE chairId = :chairId")
    suspend fun teachersFetchedAt(chairId: Long): Long?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChairs(values: List<ChairEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeachers(values: List<TeacherEntity>)

    @Query("SELECT * FROM students WHERE groupId = :groupId ORDER BY name")
    fun observeStudents(groupId: Long): Flow<List<StudentEntity>>
    @Query("SELECT MAX(fetchedAt) FROM students WHERE groupId = :groupId")
    suspend fun studentsFetchedAt(groupId: Long): Long?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(values: List<StudentEntity>)

    @Query("SELECT * FROM lessons WHERE ownerType = :ownerType AND ownerId = :ownerId AND date BETWEEN :start AND :end ORDER BY date, startMinute, endMinute")
    fun observeLessons(ownerType: String, ownerId: Long, start: String, end: String): Flow<List<LessonEntity>>
    @Query("SELECT * FROM cached_schedule_days WHERE ownerType = :ownerType AND ownerId = :ownerId AND date BETWEEN :start AND :end")
    fun observeCachedDays(ownerType: String, ownerId: Long, start: String, end: String): Flow<List<CachedScheduleDayEntity>>
    @Query("SELECT * FROM cached_schedule_days WHERE ownerType = :ownerType AND ownerId = :ownerId AND date BETWEEN :start AND :end")
    suspend fun cachedDays(ownerType: String, ownerId: Long, start: String, end: String): List<CachedScheduleDayEntity>
    @Query("DELETE FROM lessons WHERE ownerType = :ownerType AND ownerId = :ownerId AND date BETWEEN :start AND :end")
    suspend fun deleteLessons(ownerType: String, ownerId: Long, start: String, end: String)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLessons(values: List<LessonEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedDays(values: List<CachedScheduleDayEntity>)
    @Query("DELETE FROM lessons WHERE date < :date")
    suspend fun pruneLessons(date: String)
    @Query("DELETE FROM cached_schedule_days WHERE date < :date")
    suspend fun pruneCachedDays(date: String)
}
