package com.kyivsec.duikt_timetable

import com.kyivsec.duikt_timetable.model.Lesson
import com.kyivsec.duikt_timetable.model.LessonType
import com.kyivsec.duikt_timetable.notification.LiveNotificationState
import com.kyivsec.duikt_timetable.notification.NotificationPlanner
import com.kyivsec.duikt_timetable.notification.ScheduledLesson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

class NotificationPlannerTest {
    private val planner = NotificationPlanner(ZoneOffset.UTC)
    private val date = LocalDate.of(2026, 9, 21)

    @Test fun reminderIsDueFiveMinutesBeforeClass() {
        val lesson = occurrence("Math", 10, 0, 11, 20)

        val plan = planner.plan(Instant.parse("2026-09-21T09:55:00Z"), listOf(lesson), true, false)

        assertEquals(lesson, plan.reminder)
    }

    @Test fun liveNotificationShowsNextClassOnlyInsideOneHour() {
        val lesson = occurrence("Math", 10, 0, 11, 20)

        val early = planner.plan(Instant.parse("2026-09-21T09:00:00Z"), listOf(lesson), false, true)
        val visible = planner.plan(Instant.parse("2026-09-21T09:00:01Z"), listOf(lesson), false, true)

        assertNull(early.live)
        assertEquals(Instant.parse("2026-09-21T09:00:01Z"), early.nextEvaluationAt)
        assertTrue(visible.live is LiveNotificationState.Next)
        assertEquals(Instant.parse("2026-09-21T10:00:00Z"), visible.live?.countdownTarget)
    }

    @Test fun liveNotificationSwitchesToCurrentAndEndsAfterFinalClass() {
        val lesson = occurrence("Math", 10, 0, 10, 50)

        val current = planner.plan(Instant.parse("2026-09-21T10:00:00Z"), listOf(lesson), false, true)
        val finished = planner.plan(Instant.parse("2026-09-21T10:50:00Z"), listOf(lesson), false, true)

        assertTrue(current.live is LiveNotificationState.Current)
        assertEquals(Instant.parse("2026-09-21T10:50:00Z"), current.nextEvaluationAt)
        assertNull(finished.live)
        assertNull(finished.nextEvaluationAt)
    }

    @Test fun persistentNotificationDoesNotRemainOvernight() {
        val tomorrow = occurrence("Physics", 9, 0, 10, 20, date.plusDays(1))

        val plan = planner.plan(Instant.parse("2026-09-21T18:00:00Z"), listOf(tomorrow), false, true)

        assertNull(plan.live)
        assertEquals(Instant.parse("2026-09-22T08:00:01Z"), plan.nextEvaluationAt)
    }

    @Test fun gapLongerThanOneHourTemporarilyRemovesLiveNotification() {
        val first = occurrence("Math", 9, 0, 10, 0)
        val second = occurrence("Physics", 12, 0, 13, 0)

        val plan = planner.plan(Instant.parse("2026-09-21T10:00:00Z"), listOf(first, second), false, true)

        assertNull(plan.live)
        assertEquals(Instant.parse("2026-09-21T11:00:01Z"), plan.nextEvaluationAt)
    }

    @Test fun longCurrentClassAppearsOnlyWhenLessThanOneHourRemains() {
        val lesson = occurrence("Math", 10, 0, 11, 20)

        val early = planner.plan(Instant.parse("2026-09-21T10:00:00Z"), listOf(lesson), false, true)
        val visible = planner.plan(Instant.parse("2026-09-21T10:20:01Z"), listOf(lesson), false, true)

        assertNull(early.live)
        assertEquals(Instant.parse("2026-09-21T10:20:01Z"), early.nextEvaluationAt)
        assertTrue(visible.live is LiveNotificationState.Current)
    }

    private fun occurrence(
        subject: String,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        lessonDate: LocalDate = date,
    ) = ScheduledLesson(
        lessonDate,
        Lesson(
            id = "$subject-$lessonDate-$startHour-$startMinute",
            subject = subject,
            type = LessonType.LECTURE,
            startTime = LocalTime.of(startHour, startMinute),
            endTime = LocalTime.of(endHour, endMinute),
            room = "301",
        ),
    )
}
