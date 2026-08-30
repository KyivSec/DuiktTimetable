package com.kyivsec.duikttimetable.model

import androidx.compose.runtime.Immutable
import java.time.LocalTime
import java.time.Instant

enum class LessonType { LECTURE, PRACTICE, LAB, SEMINAR, EXAM, OTHER }

@Immutable
data class Lesson(
    val id: String,
    val subject: String,
    val type: LessonType,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val room: String? = null,
    val building: String? = null,
    val teacher: String? = null,
    val subgroup: String? = null,
    val notes: String? = null,
    val onlineUrl: String? = null,
    val shortSubject: String? = null,
    val rawType: String? = null,
    val sourceOccurrenceId: Long? = null,
    val sourceDisciplineId: Long? = null,
    val sourceTeacherId: Long? = null,
    val sourceGroups: String? = null,
    val chairName: String? = null,
    val sourceUpdatedAt: Instant? = null,
    val isNonstandardTime: Boolean = false,
    val notice: String? = null,
)
