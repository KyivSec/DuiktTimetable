package com.kyivsec.duikttimetable.model

import androidx.compose.runtime.Immutable

enum class Occupation { GROUP, STUDENT, TEACHER }

@Immutable
data class ChairInfo(val id: Long, val name: String)

@Immutable
data class TeacherInfo(val id: Long, val name: String, val chairId: Long, val chairName: String)

@Immutable
data class StudentInfo(
    val id: Long,
    val name: String,
    val groupId: Long,
    val groupName: String,
    val instituteId: Long,
    val instituteName: String,
    val course: Int,
)

@Immutable
sealed interface TimetableOwner {
    val type: Occupation
    val id: Long
    val displayName: String

    @Immutable
    data class Group(val group: GroupInfo) : TimetableOwner {
        override val type = Occupation.GROUP
        override val id get() = group.id
        override val displayName get() = group.name
    }

    @Immutable
    data class Student(val student: StudentInfo) : TimetableOwner {
        override val type = Occupation.STUDENT
        override val id get() = student.id
        override val displayName get() = student.name
    }

    @Immutable
    data class Teacher(val teacher: TeacherInfo) : TimetableOwner {
        override val type = Occupation.TEACHER
        override val id get() = teacher.id
        override val displayName get() = teacher.name
    }
}
