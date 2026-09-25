package com.kyivsec.duikt_timetable

import android.os.SystemClock
import android.view.View
import android.widget.Chronometer
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kyivsec.duikt_timetable.model.Lesson
import com.kyivsec.duikt_timetable.model.LessonType
import com.kyivsec.duikt_timetable.model.ThemeMode
import com.kyivsec.duikt_timetable.notification.LiveNotificationState
import com.kyivsec.duikt_timetable.notification.ScheduledLesson
import com.kyivsec.duikt_timetable.widget.ScheduleWidgetPublisher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class ScheduleWidgetPublisherTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val publisher = ScheduleWidgetPublisher(context)

    @Test fun currentClassRendersWithLiveCountdown() {
        val target = Instant.now().plusSeconds(20 * 60L)
        val state = LiveNotificationState.Current(
            ScheduledLesson(
                LocalDate.of(2026, 9, 25),
                Lesson(
                    id = "lesson",
                    subject = "Algorithms and Data Structures",
                    type = LessonType.LECTURE,
                    startTime = LocalTime.of(9, 30),
                    endTime = LocalTime.of(10, 50),
                    room = "301",
                ),
            ),
            target,
        )

        val inflated = publisher.buildRemoteViews(state, ThemeMode.LIGHT, true)
            .apply(context, FrameLayout(context))
        val countdown = inflated.findViewById<Chronometer>(R.id.widget_countdown)

        assertEquals(View.VISIBLE, inflated.findViewById<View>(R.id.widget_content).visibility)
        assertEquals(View.GONE, inflated.findViewById<View>(R.id.widget_empty).visibility)
        assertTrue(inflated.findViewById<TextView>(R.id.widget_subject).text.contains("Algorithms"))
        assertTrue(inflated.findViewById<TextView>(R.id.widget_location).text.contains("301"))
        assertTrue(countdown.isCountDown)
        val expectedBase = SystemClock.elapsedRealtime() + target.toEpochMilli() - System.currentTimeMillis()
        assertTrue(kotlin.math.abs(countdown.base - expectedBase) < 1_000L)
    }

    @Test fun emptyStatePromptsForOwner() {
        val inflated = publisher.buildRemoteViews(null, ThemeMode.DARK, false)
            .apply(context, FrameLayout(context))

        assertEquals(View.GONE, inflated.findViewById<View>(R.id.widget_content).visibility)
        assertEquals(View.VISIBLE, inflated.findViewById<View>(R.id.widget_empty).visibility)
        assertTrue(inflated.findViewById<TextView>(R.id.widget_empty).text.isNotBlank())
    }
}
