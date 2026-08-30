package com.kyivsec.duikttimetable.model

import androidx.compose.runtime.Immutable

@Immutable
data class GroupInfo(
    val id: Long,
    val name: String,
    val instituteId: Long,
    val instituteName: String,
    val course: Int,
)

@Immutable
data class Institute(
    val id: Long,
    val name: String,
    val courses: List<CourseGroups>,
)

@Immutable
data class CourseGroups(
    val course: Int,
    val groups: List<GroupInfo>,
)
