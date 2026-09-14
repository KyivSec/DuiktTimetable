package com.kyivsec.duikttimetable

import com.kyivsec.duikttimetable.model.Lesson
import com.kyivsec.duikttimetable.model.LessonType
import com.kyivsec.duikttimetable.ui.component.classNumber
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class LessonCardTest {
    @Test fun `class number changes to six at 15 40`() {
        assertEquals(5, lessonAt(15, 39).classNumber())
        assertEquals(6, lessonAt(15, 40).classNumber())
        assertEquals(6, lessonAt(17, 0).classNumber())
    }

    private fun lessonAt(hour: Int, minute: Int) = Lesson(
        id = "$hour:$minute",
        subject = "Test",
        type = LessonType.OTHER,
        startTime = LocalTime.of(hour, minute),
        endTime = LocalTime.of(hour, minute).plusMinutes(80),
    )
}
