package com.kyivsec.duikt_timetable.notification

import com.kyivsec.duikt_timetable.model.Lesson
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class ScheduledLesson(
    val date: LocalDate,
    val lesson: Lesson,
)

sealed interface LiveNotificationState {
    val occurrence: ScheduledLesson
    val countdownTarget: Instant

    data class Current(
        override val occurrence: ScheduledLesson,
        override val countdownTarget: Instant,
    ) : LiveNotificationState

    data class Next(
        override val occurrence: ScheduledLesson,
        override val countdownTarget: Instant,
    ) : LiveNotificationState
}

data class NotificationPlan(
    val reminder: ScheduledLesson? = null,
    val live: LiveNotificationState? = null,
    val nextEvaluationAt: Instant? = null,
)

class NotificationPlanner(
    private val zoneId: ZoneId,
) {
    fun plan(
        now: Instant,
        lessons: List<ScheduledLesson>,
        remindersEnabled: Boolean,
        liveEnabled: Boolean,
    ): NotificationPlan {
        val ordered = lessons.sortedWith(compareBy({ it.date }, { it.lesson.startTime }, { it.lesson.endTime }))
        val reminder = if (remindersEnabled) dueReminder(now, ordered) else null
        val live = if (liveEnabled) liveState(now, ordered) else null
        val evaluations = buildList {
            if (remindersEnabled) nextReminderAt(now, ordered)?.let(::add)
            if (liveEnabled) nextLiveEvaluationAt(now, ordered, live)?.let(::add)
        }
        return NotificationPlan(reminder, live, evaluations.minOrNull())
    }

    private fun dueReminder(now: Instant, lessons: List<ScheduledLesson>): ScheduledLesson? =
        lessons.firstOrNull { occurrence ->
            val reminderAt = occurrence.startInstant().minus(REMINDER_LEAD_TIME)
            !now.isBefore(reminderAt) && now.isBefore(reminderAt.plus(REMINDER_DELIVERY_WINDOW))
        }

    private fun nextReminderAt(now: Instant, lessons: List<ScheduledLesson>): Instant? = lessons
        .asSequence()
        .map { it.startInstant().minus(REMINDER_LEAD_TIME) }
        .filter { it.isAfter(now) }
        .minOrNull()

    private fun liveState(now: Instant, lessons: List<ScheduledLesson>): LiveNotificationState? {
        val today = now.atZone(zoneId).toLocalDate()
        val todayLessons = lessons.filter { it.date == today }
        val current = todayLessons.firstOrNull { occurrence ->
            !now.isBefore(occurrence.startInstant()) && now.isBefore(occurrence.endInstant())
        }
        if (current != null) {
            return if (Duration.between(now, current.endInstant()) <= MAX_LIVE_COUNTDOWN) {
                LiveNotificationState.Current(current, current.endInstant())
            } else null
        }

        val next = todayLessons.firstOrNull { it.startInstant().isAfter(now) } ?: return null
        val remaining = Duration.between(now, next.startInstant())
        return if (remaining <= MAX_LIVE_COUNTDOWN) {
            LiveNotificationState.Next(next, next.startInstant())
        } else null
    }

    private fun nextLiveEvaluationAt(
        now: Instant,
        lessons: List<ScheduledLesson>,
        live: LiveNotificationState?,
    ): Instant? {
        if (live != null) return live.countdownTarget.takeIf { it.isAfter(now) }

        val today = now.atZone(zoneId).toLocalDate()
        val current = lessons.firstOrNull { occurrence ->
            occurrence.date == today && !now.isBefore(occurrence.startInstant()) && now.isBefore(occurrence.endInstant())
        }
        if (current != null) return current.endInstant().minus(MAX_LIVE_COUNTDOWN).takeIf { it.isAfter(now) }

        val next = lessons.firstOrNull { it.startInstant().isAfter(now) } ?: return null
        val start = next.startInstant()
        val activation = start.minus(MAX_LIVE_COUNTDOWN)
        val startOfLessonDay = next.date.atStartOfDay(zoneId).toInstant()
        return maxOf(activation, startOfLessonDay).takeIf { it.isAfter(now) }
            ?: if (next.date == today) start else startOfLessonDay
    }

    private fun ScheduledLesson.startInstant(): Instant = date.atTime(lesson.startTime).atZone(zoneId).toInstant()
    private fun ScheduledLesson.endInstant(): Instant = date.atTime(lesson.endTime).atZone(zoneId).toInstant()

    companion object {
        val REMINDER_LEAD_TIME: Duration = Duration.ofMinutes(5)
        val REMINDER_DELIVERY_WINDOW: Duration = Duration.ofMinutes(1)
        val MAX_LIVE_COUNTDOWN: Duration = Duration.ofMinutes(99).plusSeconds(59)
    }
}
