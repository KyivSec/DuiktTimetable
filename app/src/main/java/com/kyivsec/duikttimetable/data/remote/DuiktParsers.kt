package com.kyivsec.duikttimetable.data.remote

import kotlinx.serialization.json.Json
import org.jsoup.Jsoup
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DuiktFilterPageParser {
    fun csrf(html: String): String {
        val document = Jsoup.parse(html)
        requireFilter(document.selectFirst("#filter-form") != null)
        return document.selectFirst("input[name=_csrf-frontend]")?.attr("value")
            ?.takeIf { it.isNotBlank() } ?: throw SourceFormatException("Missing CSRF field")
    }

    fun faculties(html: String): List<RemoteOption> = options(html, "#timetableform-facultyid")
    fun courses(html: String): List<Int> = options(html, "#timetableform-course").mapNotNull { it.id.toInt().takeIf { n -> n > 0 } }
    fun groups(html: String): List<RemoteOption> = options(html, "#timetableform-groupid")
    fun chairs(html: String): List<RemoteOption> = options(html, "#timetableform-chairid")
    fun teachers(html: String): List<RemoteOption> = options(html, "#timetableform-teacherid")
    fun students(html: String): List<RemoteOption> = options(html, "#timetableform-studentid")

    fun semesterRange(html: String): ClosedRange<LocalDate>? {
        val match = SEMESTER_PATTERN.find(html) ?: return null
        return runCatching {
            LocalDate.parse(match.groupValues[1])..LocalDate.parse(match.groupValues[2])
        }.getOrNull()
    }

    private fun options(html: String, selector: String): List<RemoteOption> {
        val document = Jsoup.parse(html)
        requireFilter(document.selectFirst("#filter-form") != null)
        val select = document.selectFirst(selector) ?: throw SourceFormatException("Missing $selector")
        return select.select("option[value]").mapNotNull { option ->
            val value = option.attr("value").trim()
            if (value.isEmpty()) return@mapNotNull null // The unselected placeholder.
            val id = value.toLongOrNull() ?: throw SourceFormatException("Invalid directory ID in $selector")
            val name = option.text().trim()
            if (name.isBlank()) throw SourceFormatException("Missing directory name in $selector")
            RemoteOption(id, name)
        }
    }

    private fun requireFilter(value: Boolean) {
        if (!value) throw SourceFormatException("Response does not contain the timetable form")
    }

    private companion object {
        val SEMESTER_PATTERN = Regex("Семестр[^\\n]+moment\\(\\\"(\\d{4}-\\d{2}-\\d{2})[^\\n]+moment\\(\\\"(\\d{4}-\\d{2}-\\d{2})")
    }
}

class EmbeddedEventsJsonExtractor(
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true },
) {
    fun extract(html: String): List<DuiktLessonDto> {
        if (!FILTER_FORM_PATTERN.containsMatchIn(html)) throw SourceFormatException("Response does not contain the timetable form")
        val marker = html.indexOf(EVENTS_MARKER)
        if (marker < 0) throw SourceFormatException("Missing embedded events data")
        val objectStart = html.indexOf('{', marker).takeIf { it >= 0 } ?: Int.MAX_VALUE
        val arrayStart = html.indexOf('[', marker).takeIf { it >= 0 } ?: Int.MAX_VALUE
        val start = minOf(objectStart, arrayStart)
        if (start == Int.MAX_VALUE) throw SourceFormatException("Events JSON has no collection")
        val end = balancedCollectionEnd(html, start)
        val collectionText = html.substring(start, end + 1)
        return try {
            when (html[start]) {
                '{' -> json.decodeFromString<Map<String, DuiktLessonDto>>(collectionText).values.toList()
                '[' -> json.decodeFromString<List<DuiktLessonDto>>(collectionText)
                else -> throw SourceFormatException("Events JSON is not a collection")
            }
        } catch (error: Exception) {
            throw SourceFormatException("Invalid events JSON: ${error.message}")
        }
    }

    private fun balancedCollectionEnd(text: String, start: Int): Int {
        val open = text[start]
        val close = if (open == '{') '}' else ']'
        var depth = 0
        var quoted = false
        var escaped = false
        for (index in start until text.length) {
            val char = text[index]
            if (quoted) {
                when {
                    escaped -> escaped = false
                    char == '\\' -> escaped = true
                    char == '"' -> quoted = false
                }
            } else {
                when (char) {
                    '"' -> quoted = true
                    open -> depth++
                    close -> if (--depth == 0) return index
                }
            }
        }
        throw SourceFormatException("Unterminated events JSON")
    }

    private companion object {
        const val EVENTS_MARKER = "var events ="
        val FILTER_FORM_PATTERN = Regex("id\\s*=\\s*['\"]filter-form['\"]", RegexOption.IGNORE_CASE)
    }
}

internal val SOURCE_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
