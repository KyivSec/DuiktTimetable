package com.kyivsec.duikttimetable.data

import android.content.Context
import com.kyivsec.duikttimetable.model.CourseGroups
import com.kyivsec.duikttimetable.model.GroupInfo
import com.kyivsec.duikttimetable.model.Institute
import com.kyivsec.duikttimetable.model.Lesson
import com.kyivsec.duikttimetable.model.LessonType
import com.kyivsec.duikttimetable.model.ScheduleDay
import com.kyivsec.duikttimetable.model.ScheduleWeek
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

class MockDataLoader(
    private val context: Context,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun loadSchedule(): List<ScheduleWeek> {
        val root = json.decodeFromString<ScheduleRootDto>(readAsset("mock_schedule.json"))
        val currentMonday = LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val referenceMonday = LocalDate.parse(root.referenceWeekStart)
        val shiftDays = currentMonday.toEpochDay() - referenceMonday.toEpochDay()
        return root.weeks.map { week ->
            val days = week.days.map { day ->
                ScheduleDay(
                    date = LocalDate.parse(day.date).plusDays(shiftDays),
                    lessons = day.lessons.map { lesson ->
                        Lesson(
                            id = lesson.id,
                            subject = lesson.subject,
                            type = runCatching { LessonType.valueOf(lesson.type) }.getOrDefault(LessonType.OTHER),
                            startTime = LocalTime.parse(lesson.startTime),
                            endTime = LocalTime.parse(lesson.endTime),
                            room = lesson.room,
                            building = lesson.building,
                            teacher = lesson.teacher,
                            subgroup = lesson.subgroup,
                            notes = lesson.notes,
                            onlineUrl = lesson.onlineUrl,
                        )
                    }.sortedBy(Lesson::startTime),
                )
            }
            ScheduleWeek(
                weekNumber = week.weekNumber,
                startDate = LocalDate.parse(week.startDate).plusDays(shiftDays),
                endDate = LocalDate.parse(week.endDate).plusDays(shiftDays),
                days = days,
            )
        }
    }

    fun loadGroups(): List<Institute> = json.decodeFromString<GroupsRootDto>(readAsset("mock_groups.json"))
        .institutes.map { institute ->
            Institute(
                id = institute.id,
                name = institute.name,
                courses = institute.courses.map { course ->
                    CourseGroups(
                        course = course.course,
                        groups = course.groups.map { group ->
                            GroupInfo(group.id, group.name, institute.id, institute.name, course.course)
                        },
                    )
                },
            )
        }

    private fun readAsset(name: String): String = context.assets.open(name).bufferedReader().use { it.readText() }
}

@Serializable private data class ScheduleRootDto(val referenceWeekStart: String, val weeks: List<WeekDto>)
@Serializable private data class WeekDto(val weekNumber: Int, val startDate: String, val endDate: String, val days: List<DayDto>)
@Serializable private data class DayDto(val date: String, val lessons: List<LessonDto>)
@Serializable private data class LessonDto(
    val id: String,
    val subject: String,
    val type: String,
    val startTime: String,
    val endTime: String,
    val room: String? = null,
    val building: String? = null,
    val teacher: String? = null,
    val subgroup: String? = null,
    val notes: String? = null,
    val onlineUrl: String? = null,
)
@Serializable private data class GroupsRootDto(val institutes: List<InstituteDto>)
@Serializable private data class InstituteDto(val id: Long, val name: String, val courses: List<CourseDto>)
@Serializable private data class CourseDto(val course: Int, val groups: List<GroupDto>)
@Serializable private data class GroupDto(val id: Long, val name: String)
