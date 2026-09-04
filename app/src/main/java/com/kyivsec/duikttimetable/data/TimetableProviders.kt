package com.kyivsec.duikttimetable.data

import com.kyivsec.duikttimetable.data.remote.DuiktScheduleClient
import com.kyivsec.duikttimetable.data.remote.RemoteSchedule
import com.kyivsec.duikttimetable.model.Occupation
import com.kyivsec.duikttimetable.model.TimetableOwner

interface TimetableProvider {
    val occupation: Occupation
    suspend fun fetch(owner: TimetableOwner, range: DateRange): RemoteSchedule
}

class GroupTimetableProvider(private val client: DuiktScheduleClient) : TimetableProvider {
    override val occupation = Occupation.GROUP
    override suspend fun fetch(owner: TimetableOwner, range: DateRange): RemoteSchedule {
        require(owner is TimetableOwner.Group)
        val group = owner.group
        return client.fetchSchedule(group.instituteId, group.course, group.id, range)
    }
}

class TeacherTimetableProvider(private val client: DuiktScheduleClient) : TimetableProvider {
    override val occupation = Occupation.TEACHER
    override suspend fun fetch(owner: TimetableOwner, range: DateRange): RemoteSchedule {
        require(owner is TimetableOwner.Teacher)
        return client.fetchTeacherSchedule(owner.teacher.chairId, owner.teacher.id, range)
    }
}

class StudentTimetableProvider(private val client: DuiktScheduleClient) : TimetableProvider {
    override val occupation = Occupation.STUDENT
    override suspend fun fetch(owner: TimetableOwner, range: DateRange): RemoteSchedule {
        require(owner is TimetableOwner.Student)
        val student = owner.student
        return client.fetchStudentSchedule(
            student.instituteId, student.course, student.groupId, student.id, range,
        )
    }
}

class TimetableProviderRegistry(providers: List<TimetableProvider>) {
    private val providers = providers.associateBy(TimetableProvider::occupation)
    fun providerFor(owner: TimetableOwner): TimetableProvider =
        providers[owner.type] ?: error("No timetable provider for ${owner.type}")
}
