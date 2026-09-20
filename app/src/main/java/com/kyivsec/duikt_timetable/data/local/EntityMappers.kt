package com.kyivsec.duikt_timetable.data.local

import com.kyivsec.duikt_timetable.model.GroupInfo
import com.kyivsec.duikt_timetable.model.Lesson
import com.kyivsec.duikt_timetable.model.LessonType
import com.kyivsec.duikt_timetable.model.TimetableOwner
import com.kyivsec.duikt_timetable.model.TeacherInfo
import com.kyivsec.duikt_timetable.model.StudentInfo
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

fun LessonEntity.toDomain(): Lesson = Lesson(
    id = id,
    subject = subject,
    type = runCatching { LessonType.valueOf(lessonType) }.getOrDefault(LessonType.OTHER),
    startTime = LocalTime.of(startMinute / 60, startMinute % 60),
    endTime = LocalTime.of(endMinute / 60, endMinute % 60),
    room = room,
    building = building,
    teacher = teacher,
    subgroup = subgroup,
    notes = notes,
    onlineUrl = onlineUrl,
    shortSubject = shortSubject,
    rawType = rawType,
    sourceOccurrenceId = sourceOccurrenceId,
    sourceDisciplineId = sourceDisciplineId,
    sourceTeacherId = sourceTeacherId,
    sourceGroups = sourceGroups,
    chairName = chairName,
    sourceUpdatedAt = sourceUpdatedAt?.let(Instant::ofEpochMilli),
    isNonstandardTime = isNonstandardTime,
    notice = notice,
)

fun Lesson.toEntity(owner: TimetableOwner, date: LocalDate): LessonEntity = LessonEntity(
    id, owner.type.name, owner.id, date.toString(), subject, shortSubject, type.name, rawType,
    startTime.hour * 60 + startTime.minute, endTime.hour * 60 + endTime.minute,
    room, building, teacher, subgroup, notes, onlineUrl, sourceOccurrenceId, sourceDisciplineId,
    sourceTeacherId, sourceGroups, chairName, sourceUpdatedAt?.toEpochMilli(), isNonstandardTime, notice,
)

fun GroupEntity.toDomain(facultyName: String): GroupInfo = GroupInfo(id, name, facultyId, facultyName, course)

fun TeacherEntity.toDomain(chairName: String): TeacherInfo = TeacherInfo(id, name, chairId, chairName)

fun StudentEntity.toDomain(facultyName: String): StudentInfo = StudentInfo(
    id, name, groupId, groupName, facultyId, facultyName, course,
)
