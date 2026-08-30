package com.kyivsec.duikttimetable.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import java.time.LocalDate

data class RemoteOption(val id: Long, val name: String)
data class RemoteSchedule(val lessons: List<DuiktLessonDto>, val semesterRange: ClosedRange<LocalDate>?)

@Serializable
data class DuiktLessonDto(
    val r1: Long? = null,
    val disciplineId: Long? = null,
    val educationDisciplineId: Long? = null,
    val disciplineFullName: String = "",
    val disciplineShortName: String? = null,
    val classroom: String? = null,
    val timeStart: String = "",
    val timeEnd: String = "",
    val teachersName: String? = null,
    val teachersNameFull: String? = null,
    val teacherP1: Long? = null,
    val type: String? = null,
    val typeStr: String? = null,
    val dateUpdated: String? = null,
    val nonstandardTime: Boolean = false,
    val groups: String? = null,
    val chairName: String? = null,
    val extraText: JsonElement? = null,
    val lessonYear: Int? = null,
    val semester: Int? = null,
    val notice: String? = null,
    val info: String? = null,
    val date: String = "",
)

class SourceFormatException(message: String) : Exception(message)
class HttpStatusException(val statusCode: Int) : Exception("HTTP $statusCode")
class RejectedSessionException : Exception("DUІКТ session was rejected")
