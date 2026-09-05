package com.kyivsec.duikttimetable.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FacultyEntity::class, CourseEntity::class, GroupEntity::class, ChairEntity::class, TeacherEntity::class, StudentEntity::class, LessonEntity::class, CachedScheduleDayEntity::class, SemesterSyncEntity::class, DirectorySyncEntity::class],
    version = 5,
    exportSchema = true,
)
abstract class TimetableDatabase : RoomDatabase() {
    abstract fun timetableDao(): TimetableDao
    internal val directorySyncMutex = kotlinx.coroutines.sync.Mutex()

    companion object {
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS directory_syncs (branch TEXT NOT NULL PRIMARY KEY, fetchedAt INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE faculties_new (id INTEGER NOT NULL, name TEXT NOT NULL, fetchedAt INTEGER NOT NULL, source TEXT NOT NULL, PRIMARY KEY(source, id))")
                db.execSQL("CREATE TABLE courses_new (facultyId INTEGER NOT NULL, course INTEGER NOT NULL, fetchedAt INTEGER NOT NULL, source TEXT NOT NULL, PRIMARY KEY(source, facultyId, course))")
                db.execSQL("CREATE TABLE groups_new (id INTEGER NOT NULL, facultyId INTEGER NOT NULL, course INTEGER NOT NULL, name TEXT NOT NULL, fetchedAt INTEGER NOT NULL, source TEXT NOT NULL, PRIMARY KEY(source, id))")
                // The old cache was shared. Preserve it for both selectors, but leave it
                // unmarked so each endpoint reconciles its own branch on the next refresh.
                for (source in listOf("GROUP", "STUDENT")) {
                    db.execSQL("INSERT INTO faculties_new SELECT id, name, fetchedAt, '$source' FROM faculties")
                    db.execSQL("INSERT INTO courses_new SELECT facultyId, course, fetchedAt, '$source' FROM courses")
                    db.execSQL("INSERT INTO groups_new SELECT id, facultyId, course, name, fetchedAt, '$source' FROM groups")
                }
                for (table in listOf("faculties", "courses", "groups")) {
                    db.execSQL("DROP TABLE $table")
                    db.execSQL("ALTER TABLE ${table}_new RENAME TO $table")
                }
                db.execSQL("CREATE INDEX index_courses_facultyId ON courses(facultyId)")
                db.execSQL("CREATE INDEX index_groups_facultyId_course ON groups(facultyId, course)")
                db.execSQL("CREATE INDEX index_groups_name ON groups(name)")
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Existing day markers cannot prove that a complete semester was fetched.
                db.execSQL("CREATE TABLE IF NOT EXISTS semester_syncs (ownerType TEXT NOT NULL, ownerId INTEGER NOT NULL, startDate TEXT NOT NULL, endDate TEXT NOT NULL, fetchedAt INTEGER NOT NULL, PRIMARY KEY(ownerType, ownerId))")
            }
        }
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS chairs (id INTEGER NOT NULL, name TEXT NOT NULL, fetchedAt INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chairs_name ON chairs(name)")
                db.execSQL("CREATE TABLE IF NOT EXISTS teachers (id INTEGER NOT NULL, chairId INTEGER NOT NULL, name TEXT NOT NULL, fetchedAt INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_teachers_chairId ON teachers(chairId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_teachers_name ON teachers(name)")
                db.execSQL("CREATE TABLE lessons_new (id TEXT NOT NULL, ownerType TEXT NOT NULL, ownerId INTEGER NOT NULL, date TEXT NOT NULL, subject TEXT NOT NULL, shortSubject TEXT, lessonType TEXT NOT NULL, rawType TEXT, startMinute INTEGER NOT NULL, endMinute INTEGER NOT NULL, room TEXT, building TEXT, teacher TEXT, subgroup TEXT, notes TEXT, onlineUrl TEXT, sourceOccurrenceId INTEGER, sourceDisciplineId INTEGER, sourceTeacherId INTEGER, sourceGroups TEXT, chairName TEXT, sourceUpdatedAt INTEGER, isNonstandardTime INTEGER NOT NULL, notice TEXT, PRIMARY KEY(id))")
                db.execSQL("INSERT INTO lessons_new SELECT id, 'GROUP', groupId, date, subject, shortSubject, lessonType, rawType, startMinute, endMinute, room, building, teacher, subgroup, notes, onlineUrl, sourceOccurrenceId, sourceDisciplineId, sourceTeacherId, sourceGroups, chairName, sourceUpdatedAt, isNonstandardTime, notice FROM lessons")
                db.execSQL("DROP TABLE lessons")
                db.execSQL("ALTER TABLE lessons_new RENAME TO lessons")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_lessons_ownerType_ownerId_date_startMinute ON lessons(ownerType, ownerId, date, startMinute)")
                db.execSQL("CREATE TABLE cached_schedule_days_new (ownerType TEXT NOT NULL, ownerId INTEGER NOT NULL, date TEXT NOT NULL, fetchedAt INTEGER NOT NULL, PRIMARY KEY(ownerType, ownerId, date))")
                db.execSQL("INSERT INTO cached_schedule_days_new(ownerType, ownerId, date, fetchedAt) SELECT 'GROUP', groupId, date, fetchedAt FROM cached_schedule_days")
                db.execSQL("DROP TABLE cached_schedule_days")
                db.execSQL("ALTER TABLE cached_schedule_days_new RENAME TO cached_schedule_days")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cached_schedule_days_date ON cached_schedule_days(date)")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS students (id INTEGER NOT NULL, groupId INTEGER NOT NULL, groupName TEXT NOT NULL, facultyId INTEGER NOT NULL, course INTEGER NOT NULL, name TEXT NOT NULL, fetchedAt INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_students_groupId ON students(groupId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_students_name ON students(name)")
            }
        }
    }
}
