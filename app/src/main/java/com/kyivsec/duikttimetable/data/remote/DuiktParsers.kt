package com.kyivsec.duikttimetable.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
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

    fun semesterRange(html: String): ClosedRange<LocalDate>? {
        val script = Jsoup.parse(html).select("script").asSequence()
            .map { it.data() }.firstOrNull { it.contains("Семестр") && it.contains("moment(") } ?: return null
        val match = SEMESTER_PATTERN.find(script) ?: return null
        return runCatching {
            LocalDate.parse(match.groupValues[1])..LocalDate.parse(match.groupValues[2])
        }.getOrNull()
    }

    private fun options(html: String, selector: String): List<RemoteOption> {
        val document = Jsoup.parse(html)
        requireFilter(document.selectFirst("#filter-form") != null)
        val select = document.selectFirst(selector) ?: throw SourceFormatException("Missing $selector")
        return select.select("option[value]").mapNotNull { option ->
            val id = option.attr("value").toLongOrNull() ?: return@mapNotNull null
            option.text().trim().takeIf { it.isNotBlank() }?.let { RemoteOption(id, it) }
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
        val document = Jsoup.parse(html)
        if (document.selectFirst("#filter-form") == null) throw SourceFormatException("Response does not contain the timetable form")
        val script = document.select("script").asSequence().map { it.data() }
            .firstOrNull { it.contains("var events =") }
            ?: throw SourceFormatException("Missing embedded events data")
        val marker = script.indexOf("var events =")
        val objectStart = script.indexOf('{', marker).takeIf { it >= 0 } ?: Int.MAX_VALUE
        val arrayStart = script.indexOf('[', marker).takeIf { it >= 0 } ?: Int.MAX_VALUE
        val start = minOf(objectStart, arrayStart)
        if (start == Int.MAX_VALUE) throw SourceFormatException("Events JSON has no collection")
        val end = balancedCollectionEnd(script, start)
        val collectionText = script.substring(start, end + 1)
        return try {
            when (val root = json.parseToJsonElement(collectionText)) {
                is JsonObject -> root.values.map { json.decodeFromJsonElement<DuiktLessonDto>(it) }
                is JsonArray -> root.map { json.decodeFromJsonElement<DuiktLessonDto>(it) }
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
}

internal val SOURCE_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
