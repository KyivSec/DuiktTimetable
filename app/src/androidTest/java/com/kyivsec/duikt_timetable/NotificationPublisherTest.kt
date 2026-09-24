package com.kyivsec.duikt_timetable

import android.app.Notification
import android.os.SystemClock
import android.widget.Chronometer
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kyivsec.duikt_timetable.model.Lesson
import com.kyivsec.duikt_timetable.model.LessonType
import com.kyivsec.duikt_timetable.notification.LiveNotificationState
import com.kyivsec.duikt_timetable.notification.NotificationPublisher
import com.kyivsec.duikt_timetable.notification.ScheduledLesson
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class NotificationPublisherTest {
    @Test fun liveNotificationUsesCustomCountdownWithVisibleLessonDetails() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val target = Instant.now().plusSeconds(20 * 60L)
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

        val contentView = notification.contentView
        val bigContentView = notification.bigContentView
        assertNotNull(contentView)
        assertNotNull(bigContentView)
        assertNull(notification.largeIcon)
        assertNull(notification.extras.getString(Notification.EXTRA_TEMPLATE))
        val inflated = contentView.apply(context, FrameLayout(context))
        val inflatedBig = bigContentView.apply(context, FrameLayout(context))
        val countdown = inflated.findViewById<Chronometer>(R.id.notification_countdown)

        assertTrue(inflated.findViewById<TextView>(R.id.notification_lesson).maxLines == 1)
        assertTrue(inflatedBig.findViewById<TextView>(R.id.notification_lesson).maxLines == 2)
        assertTrue(inflated.findViewById<TextView>(R.id.notification_lesson).text.toString().contains("Algorithms"))
        assertTrue(inflated.findViewById<TextView>(R.id.notification_location).text.toString().contains("301"))
        assertTrue(countdown.isCountDown)
        val expectedBase = SystemClock.elapsedRealtime() + target.toEpochMilli() - System.currentTimeMillis()
        assertTrue(kotlin.math.abs(countdown.base - expectedBase) < 1_000L)
        assertTrue(notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString().contains("Algorithms"))
        assertTrue(notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString().contains("301"))
        assertFalse(notification.extras.getBoolean(Notification.EXTRA_SHOW_WHEN))
    }
}
