package com.kyivsec.duikt_timetable

import android.app.Notification
import androidx.core.app.NotificationCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kyivsec.duikt_timetable.model.Lesson
import com.kyivsec.duikt_timetable.model.LessonType
import com.kyivsec.duikt_timetable.notification.LiveNotificationState
import com.kyivsec.duikt_timetable.notification.NotificationPublisher
import com.kyivsec.duikt_timetable.notification.ScheduledLesson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class NotificationPublisherTest {
    @Test fun liveNotificationUsesSystemTemplateWithVisibleLessonDetails() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val target = Instant.parse("2026-09-21T10:50:00Z")
        val occurrence = ScheduledLesson(
            LocalDate.of(2026, 9, 21),
            Lesson(
                id = "lesson",
                subject = "Algorithms",
                type = LessonType.LECTURE,
                startTime = LocalTime.of(9, 30),
                endTime = LocalTime.of(10, 50),
                room = "301",
            ),
        )

        val notification = NotificationPublisher(context).buildLiveNotification(
            LiveNotificationState.Current(occurrence, target),
        )

        assertNull(notification.contentView)
        assertNull(notification.bigContentView)
        assertTrue(notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString().contains("Algorithms"))
        assertTrue(notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString().contains("301"))
        assertTrue(notification.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertTrue(notification.extras.getBoolean(NotificationCompat.EXTRA_CHRONOMETER_COUNT_DOWN))
        assertEquals(target.toEpochMilli(), notification.`when`)
    }
}
