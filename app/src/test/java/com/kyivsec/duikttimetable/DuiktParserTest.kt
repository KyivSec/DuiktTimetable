package com.kyivsec.duikttimetable

import com.kyivsec.duikttimetable.data.remote.DuiktFilterPageParser
import com.kyivsec.duikttimetable.data.remote.DuiktLessonMapper
import com.kyivsec.duikttimetable.data.remote.EmbeddedEventsJsonExtractor
import com.kyivsec.duikttimetable.model.LessonType
import com.kyivsec.duikttimetable.model.GroupInfo
import com.kyivsec.duikttimetable.model.TimetableOwner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class DuiktParserTest {
    @Test fun `parses chair and full teacher directories`() {
        val parser = DuiktFilterPageParser()
        val html = """
            <form id="filter-form">
              <select id="timetableform-chairid"><option value="12">Кафедра систем</option></select>
              <select id="timetableform-teacherid"><option value="34">Петренко Петро Петрович</option></select>
              <select id="timetableform-studentid"><option value="56">Іваненко Іван Іванович</option></select>
            </form>
        """.trimIndent()
        assertEquals("Кафедра систем", parser.chairs(html).single().name)
        assertEquals("Петренко Петро Петрович", parser.teachers(html).single().name)
        assertEquals("Іваненко Іван Іванович", parser.students(html).single().name)
    }
    @Test fun `filter parser reads csrf options and semester`() {
        val html = """
            <form id="filter-form"><input name="_csrf-frontend" value="token">
            <select id="timetableform-facultyid"><option value="">--</option><option value="1">ННІ ІТ</option></select></form>
            <script>ranges:{"Семестр":[moment("2026-09-01 00:00:00"), moment("2026-12-30 00:00:00")]}</script>
        """.trimIndent()
        val parser = DuiktFilterPageParser()
        assertEquals("token", parser.csrf(html))
        assertEquals("ННІ ІТ", parser.faculties(html).single().name)
        assertEquals(LocalDate.of(2026, 9, 1), parser.semesterRange(html)?.start)
    }

    @Test fun `events JSON and metadata map without scraping timetable cells`() {
        val html = """
            <form id="filter-form"><input name="_csrf-frontend" value="token"></form>
            <script>var events = {"1":{"r1":370653,"disciplineId":1125,"disciplineFullName":"Об'єктно-орієнтоване програмування C#","disciplineShortName":"ООП","classroom":"ауд. 325","timeStart":"09:30","timeEnd":"10:50","teachersNameFull":"Ярошевський Олександр Вікторович","teacherP1":860,"typeStr":"Лб т.2","dateUpdated":"2026-02-03 14:34:30","nonstandardTime":false,"groups":"ПД-31","chairName":"ІПЗ","notice":"Взяти ноутбук","info":"<p>Адреса: https://meet.google.com/abc-defg-hij</p>","date":"13.05.2026","unknownFutureField":42}};</script>
        """.trimIndent()
        val dto = EmbeddedEventsJsonExtractor().extract(html).single()
        val owner = TimetableOwner.Group(GroupInfo(1247, "ПД-31", 1, "ННІ ІТ", 3))
        val (date, lesson) = DuiktLessonMapper(ZoneOffset.UTC).map(owner, dto)
        assertEquals(LocalDate.of(2026, 5, 13), date)
        assertEquals(LessonType.LAB, lesson.type)
        assertEquals("325", lesson.room)
        assertEquals("https://meet.google.com/abc-defg-hij", lesson.onlineUrl)
        assertTrue(lesson.notes.orEmpty().contains("Взяти ноутбук"))
    }

    @Test fun `empty events object is a valid empty schedule`() {
        val html = "<form id=\"filter-form\"></form><script>var events = {};</script>"
        assertTrue(EmbeddedEventsJsonExtractor().extract(html).isEmpty())
        val arrayHtml = "<form id=\"filter-form\"></form><script>var events = [];</script>"
        assertTrue(EmbeddedEventsJsonExtractor().extract(arrayHtml).isEmpty())
    }
}
