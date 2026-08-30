package com.kyivsec.duikttimetable.data.remote

import com.kyivsec.duikttimetable.model.Lesson
import com.kyivsec.duikttimetable.model.LessonType
import org.jsoup.Jsoup
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class DuiktLessonMapper(private val zoneId: ZoneId = ZoneId.systemDefault()) {
    fun map(groupId: Long, dto: DuiktLessonDto): Pair<LocalDate, Lesson> {
        val date = LocalDate.parse(dto.date, SOURCE_DATE_FORMAT)
        val start = LocalTime.parse(dto.timeStart)
        val end = LocalTime.parse(dto.timeEnd)
        val info = sanitizeInfo(dto.info)
        val rawType = dto.typeStr?.trim().orEmpty()
        val room = dto.classroom?.removePrefix("ауд.")?.trim()?.takeIf { it.isNotBlank() }
        val idSeed = listOf(groupId, date, start, end, dto.r1, dto.teacherP1, room).joinToString("|")
        return date to Lesson(
            id = sha256(idSeed),
            subject = dto.disciplineFullName.trim().ifBlank { dto.disciplineShortName.orEmpty() },
            shortSubject = dto.disciplineShortName?.trim()?.takeIf { it.isNotBlank() },
            type = lessonType(rawType),
            rawType = rawType.takeIf { it.isNotBlank() },
            startTime = start,
            endTime = end,
            room = room,
            teacher = dto.teachersNameFull?.trim()?.takeIf { it.isNotBlank() } ?: dto.teachersName,
            notes = listOfNotNull(dto.notice.clean(), info.text).joinToString("\n").takeIf { it.isNotBlank() },
            onlineUrl = info.url,
            sourceOccurrenceId = dto.r1,
            sourceDisciplineId = dto.disciplineId,
            sourceTeacherId = dto.teacherP1,
            sourceGroups = dto.groups.clean(),
            chairName = dto.chairName.clean(),
            sourceUpdatedAt = dto.dateUpdated?.let { runCatching { LocalDateTime.parse(it, UPDATED_FORMAT).atZone(zoneId).toInstant() }.getOrNull() },
            isNonstandardTime = dto.nonstandardTime,
            notice = dto.notice.clean(),
        )
    }

    private fun lessonType(raw: String): LessonType = when {
        raw.startsWith("Лк", true) -> LessonType.LECTURE
        raw.startsWith("Пз", true) || raw.contains("практ", true) -> LessonType.PRACTICE
        raw.startsWith("Лб", true) || raw.contains("лабор", true) -> LessonType.LAB
        raw.contains("сем", true) -> LessonType.SEMINAR
        raw.contains("екз", true) || raw.contains("ісп", true) -> LessonType.EXAM
        else -> LessonType.OTHER
    }

    private data class SanitizedInfo(val text: String?, val url: String?)
    private fun sanitizeInfo(html: String?): SanitizedInfo {
        if (html.isNullOrBlank()) return SanitizedInfo(null, null)
        val document = Jsoup.parseBodyFragment(html)
        val text = document.text().trim().takeIf { it.isNotBlank() }
        val link = document.select("a[href]").firstNotNullOfOrNull { it.absUrl("href").ifBlank { it.attr("href") }.httpUrlOrNull() }
            ?: URL_PATTERN.find(text.orEmpty())?.value
        return SanitizedInfo(text, link)
    }

    private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotBlank() }
    private fun String.httpUrlOrNull(): String? = takeIf { startsWith("https://") || startsWith("http://") }
    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8)).joinToString("") { "%02x".format(it) }

    private companion object {
        val UPDATED_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val URL_PATTERN = Regex("https?://[^\\s<]+")
    }
}
