package com.kyivsec.duikttimetable

import com.kyivsec.duikttimetable.data.remote.DuiktLessonMapper
import com.kyivsec.duikttimetable.data.remote.EmbeddedEventsJsonExtractor
import com.kyivsec.duikttimetable.model.GroupInfo
import com.kyivsec.duikttimetable.model.StudentInfo
import com.kyivsec.duikttimetable.model.TeacherInfo
import com.kyivsec.duikttimetable.model.TimetableOwner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset
import kotlin.system.measureNanoTime

class ParsingPerformanceTest {
    @Test fun `measure group day response parsing`() = benchmark(
        name = "group-day",
        owner = TimetableOwner.Group(GroupInfo(1001, "ПД-31", 1, "ННІ ІТ", 3)),
        lessonCount = 16,
        iterations = 30,
        collection = CollectionShape.OBJECT,
    )

    @Test fun `measure teacher week response parsing`() = benchmark(
        name = "teacher-week",
        owner = TimetableOwner.Teacher(TeacherInfo(860, "Ярошевський Олександр Вікторович", 12, "ІПЗ")),
        lessonCount = 80,
        iterations = 15,
        collection = CollectionShape.ARRAY,
    )

    @Test fun `measure individual student semester response parsing`() = benchmark(
        name = "student-semester",
        owner = TimetableOwner.Student(StudentInfo(47822, "Іваненко Іван Андрійович", 1700, "ШІД-11", 1, "ННІ ІТ", 1)),
        lessonCount = 500,
        iterations = 5,
        collection = CollectionShape.OBJECT,
    )

    private fun benchmark(
        name: String,
        owner: TimetableOwner,
        lessonCount: Int,
        iterations: Int,
        collection: CollectionShape,
    ) {
        val html = response(lessonCount, collection)
        val extractor = EmbeddedEventsJsonExtractor()
        val mapper = DuiktLessonMapper(ZoneOffset.UTC)
        repeat(3) { extractor.extract(html).forEach { mapper.map(owner, it) } }

        var parsed = emptyList<com.kyivsec.duikttimetable.data.remote.DuiktLessonDto>()
        val extractionNanos = measureNanoTime {
            repeat(iterations) { parsed = extractor.extract(html) }
        }
        var mappedCount = 0
        val mappingNanos = measureNanoTime {
            repeat(iterations) {
                mappedCount += parsed.count { mapper.map(owner, it).second.subject.isNotEmpty() }
            }
        }
        val extractionMs = extractionNanos / 1_000_000.0 / iterations
        val mappingMs = mappingNanos / 1_000_000.0 / iterations
        println(
            "PARSING_BENCHMARK name=$name lessons=$lessonCount bytes=${html.length} " +
                "extract_avg_ms=${"%.3f".format(extractionMs)} map_avg_ms=${"%.3f".format(mappingMs)} " +
                "total_avg_ms=${"%.3f".format(extractionMs + mappingMs)}",
        )

        assertEquals(lessonCount, parsed.size)
        assertEquals(lessonCount * iterations, mappedCount)
        assertTrue("$name parsing exceeded the generous regression ceiling", extractionMs + mappingMs < 5_000.0)
    }

    private fun response(count: Int, shape: CollectionShape): String {
        val events = (0 until count).joinToString(",") { index ->
            val lesson = """{"r1":$index,"disciplineId":${1000 + index},"disciplineFullName":"Алгоритми та структури даних $index","disciplineShortName":"АСД","classroom":"ауд. ${100 + index % 30}","timeStart":"09:30","timeEnd":"10:50","teachersNameFull":"Викладач Тестовий $index","teacherP1":${800 + index % 20},"typeStr":"Лб","dateUpdated":"2026-09-01 12:00:00","groups":"ПД-31","chairName":"ІПЗ","notice":"Підготуватися","info":"<p>Матеріали: https://example.com/$index</p>","date":"${"%02d".format(index % 28 + 1)}.09.2026"}"""
            if (shape == CollectionShape.OBJECT) "\"$index\":$lesson" else lesson
        }
        val collection = if (shape == CollectionShape.OBJECT) "{$events}" else "[$events]"
        return """
            <!doctype html><html><body><form id="filter-form"></form>
            <script>var events = $collection;</script>
            <script>ranges:{"Семестр":[moment("2026-09-01 00:00:00"), moment("2026-12-30 00:00:00")]}</script>
            </body></html>
        """.trimIndent()
    }

    private enum class CollectionShape { OBJECT, ARRAY }
}
