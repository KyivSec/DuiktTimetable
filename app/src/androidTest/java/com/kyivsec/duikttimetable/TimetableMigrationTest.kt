package com.kyivsec.duikttimetable

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kyivsec.duikttimetable.data.local.TimetableDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TimetableMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TimetableDatabase::class.java,
    )

    @Test fun v1GroupRowsSurviveAndOwnerCachesAreIsolated() {
        helper.createDatabase(DATABASE, 1).apply {
            execSQL("INSERT INTO cached_schedule_days(groupId,date,fetchedAt) VALUES(17,'2026-09-04',123)")
            execSQL("INSERT INTO lessons(id,groupId,date,subject,lessonType,startMinute,endMinute,isNonstandardTime) VALUES('old',17,'2026-09-04','Algorithms','LECTURE',570,650,0)")
            close()
        }

        val db = helper.runMigrationsAndValidate(
            DATABASE, 5, true, TimetableDatabase.MIGRATION_1_2, TimetableDatabase.MIGRATION_2_3, TimetableDatabase.MIGRATION_3_4, TimetableDatabase.MIGRATION_4_5,
        )
        assertEquals(1, db.count("SELECT COUNT(*) FROM lessons WHERE ownerType='GROUP' AND ownerId=17 AND id='old'"))
        assertEquals(1, db.count("SELECT COUNT(*) FROM cached_schedule_days WHERE ownerType='GROUP' AND ownerId=17"))
        db.execSQL("INSERT INTO cached_schedule_days(ownerType,ownerId,date,fetchedAt) VALUES('TEACHER',17,'2026-09-04',456)")
        assertEquals(2, db.count("SELECT COUNT(*) FROM cached_schedule_days WHERE ownerId=17 AND date='2026-09-04'"))
        db.execSQL("INSERT INTO students(id,groupId,groupName,facultyId,course,name,fetchedAt) VALUES(9,17,'ПД-31',1,3,'Іваненко Іван Іванович',456)")
        assertEquals(1, db.count("SELECT COUNT(*) FROM students WHERE groupId=17"))
        assertEquals(0, db.count("SELECT COUNT(*) FROM semester_syncs"))
        db.close()
    }

    private fun SupportSQLiteDatabase.count(sql: String): Int = query(sql).use { cursor ->
        cursor.moveToFirst()
        cursor.getInt(0)
    }

    @Test fun v4DirectoriesSurviveInBothSourcesWithoutInventingFreshness() {
        helper.createDatabase(DATABASE, 4).apply {
            execSQL("INSERT INTO faculties VALUES(1, 'ІТ', 123)")
            execSQL("INSERT INTO courses VALUES(1, 3, 123)")
            execSQL("INSERT INTO groups VALUES(17, 1, 3, 'ПД-31', 123)")
            execSQL("INSERT INTO semester_syncs VALUES('GROUP', 17, '2026-09-01', '2026-12-31', 123)")
            close()
        }
        val db = helper.runMigrationsAndValidate(DATABASE, 5, true, TimetableDatabase.MIGRATION_4_5)
        for (source in listOf("GROUP", "STUDENT")) {
            assertEquals(1, db.count("SELECT COUNT(*) FROM faculties WHERE source='$source' AND id=1"))
            assertEquals(1, db.count("SELECT COUNT(*) FROM courses WHERE source='$source' AND facultyId=1 AND course=3"))
            assertEquals(1, db.count("SELECT COUNT(*) FROM groups WHERE source='$source' AND id=17"))
        }
        assertEquals(0, db.count("SELECT COUNT(*) FROM directory_syncs"))
        assertEquals(1, db.count("SELECT COUNT(*) FROM semester_syncs"))
        db.close()
    }

    private companion object { const val DATABASE = "migration-test" }
}
