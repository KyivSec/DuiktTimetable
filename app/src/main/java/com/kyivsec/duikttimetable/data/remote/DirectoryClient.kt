package com.kyivsec.duikttimetable.data.remote

interface DirectoryClient {
    suspend fun fetchInstitutes(): List<RemoteOption>
    suspend fun fetchCourses(facultyId: Long): List<Int>
    suspend fun fetchGroups(facultyId: Long, course: Int): List<RemoteOption>
    suspend fun fetchChairs(): List<RemoteOption>
    suspend fun fetchTeachers(chairId: Long): List<RemoteOption>
    suspend fun fetchStudentInstitutes(): List<RemoteOption>
    suspend fun fetchStudentCourses(facultyId: Long): List<Int>
    suspend fun fetchStudentGroups(facultyId: Long, course: Int): List<RemoteOption>
    suspend fun fetchStudents(facultyId: Long, course: Int, groupId: Long): List<RemoteOption>
}
