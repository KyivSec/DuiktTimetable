package com.kyivsec.duikttimetable

import com.kyivsec.duikttimetable.data.remote.DuiktScheduleClient
import com.kyivsec.duikttimetable.data.DateRange
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class DuiktScheduleClientTest {
    private lateinit var server: MockWebServer

    @Before fun start() { server = MockWebServer(); server.start() }
    @After fun stop() { server.shutdown() }

    @Test fun `session carries cookie csrf and exact dependent form fields`() = runTest {
        server.enqueue(MockResponse().setHeader("Set-Cookie", "PHPSESSID=abc; Path=/").setBody(initialPage("first")))
        server.enqueue(MockResponse().setBody(coursePage("second")))
        val client = DuiktScheduleClient(server.url("/").toString().removeSuffix("/"))

        assertEquals(listOf(1, 2, 3), client.fetchCourses(1))

        val initial = server.takeRequest()
        val post = server.takeRequest()
        assertEquals("/time-table/group", initial.path)
        assertEquals("PHPSESSID=abc", post.getHeader("Cookie"))
        val body = post.body.readUtf8()
        assertTrue(body.contains("_csrf-frontend=first"))
        assertTrue(body.contains("TimeTableForm%5BfacultyId%5D=1"))
        assertEquals("/time-table/group?type=0", post.path)
    }

    @Test fun `teacher directory uses chair and teacher endpoint fields`() = runTest {
        server.enqueue(MockResponse().setBody(teacherInitialPage("teacher-csrf")))
        server.enqueue(MockResponse().setBody(teacherPage("next")))
        val client = DuiktScheduleClient(server.url("/").toString().removeSuffix("/"))

        assertEquals(listOf("Іваненко Іван Іванович"), client.fetchTeachers(42).map { it.name })

        assertEquals("/time-table/teacher", server.takeRequest().path)
        val post = server.takeRequest()
        assertEquals("/time-table/teacher?type=0", post.path)
        val body = post.body.readUtf8()
        assertTrue(body.contains("TimeTableForm%5BchairId%5D=42"))
        assertTrue(body.contains("TimeTableForm%5BteacherId%5D="))
    }

    @Test fun `student directory posts the full group chain and student field`() = runTest {
        server.enqueue(MockResponse().setBody(studentInitialPage("student-csrf")))
        server.enqueue(MockResponse().setBody(studentPage("next")))
        val client = DuiktScheduleClient(server.url("/").toString().removeSuffix("/"))

        assertEquals(listOf("Петренко Петро Петрович"), client.fetchStudents(1, 3, 567).map { it.name })

        assertEquals("/time-table/student?type=0", server.takeRequest().path)
        val post = server.takeRequest()
        assertEquals("/time-table/student?type=0", post.path)
        val body = post.body.readUtf8()
        assertTrue(body.contains("TimeTableForm%5BfacultyId%5D=1"))
        assertTrue(body.contains("TimeTableForm%5Bcourse%5D=3"))
        assertTrue(body.contains("TimeTableForm%5BgroupId%5D=567"))
        assertTrue(body.contains("TimeTableForm%5BstudentId%5D="))
    }

    @Test fun `student schedule uses type one endpoint and adds identity and date range`() = runTest {
        server.enqueue(MockResponse().setBody(studentInitialPage("student-csrf")))
        server.enqueue(MockResponse().setBody(studentSchedulePage("next")))
        val client = DuiktScheduleClient(server.url("/").toString().removeSuffix("/"))
        val range = DateRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 7))

        assertTrue(client.fetchStudentSchedule(1, 3, 567, 9, range).lessons.isEmpty())
        server.takeRequest()
        val post = server.takeRequest()
        assertEquals("/time-table/student?type=1", post.path)
        val body = post.body.readUtf8()
        assertTrue(body.contains("TimeTableForm%5BstudentId%5D=9"))
        assertTrue(body.contains("TimeTableForm%5BdateStart%5D=01.09.2026"))
        assertTrue(body.contains("TimeTableForm%5BdateEnd%5D=07.09.2026"))
    }

    private fun initialPage(token: String) = """
        <form id="filter-form"><input name="_csrf-frontend" value="$token">
        <select id="timetableform-facultyid"><option value="1">ННІ ІТ</option></select></form>
    """.trimIndent()

    private fun coursePage(token: String) = """
        <form id="filter-form"><input name="_csrf-frontend" value="$token">
        <select id="timetableform-course"><option value="1">1</option><option value="2">2</option><option value="3">3</option></select></form>
    """.trimIndent()

    private fun teacherInitialPage(token: String) = """
        <form id="filter-form"><input name="_csrf-frontend" value="$token">
        <select id="timetableform-chairid"><option value="42">Кафедра ІТ</option></select></form>
    """.trimIndent()

    private fun teacherPage(token: String) = """
        <form id="filter-form"><input name="_csrf-frontend" value="$token">
        <select id="timetableform-teacherid"><option value="7">Іваненко Іван Іванович</option></select></form>
    """.trimIndent()

    private fun studentInitialPage(token: String) = """
        <form id="filter-form"><input name="_csrf-frontend" value="$token">
        <select id="timetableform-facultyid"><option value="1">ННІ ІТ</option></select></form>
    """.trimIndent()

    private fun studentPage(token: String) = """
        <form id="filter-form"><input name="_csrf-frontend" value="$token">
        <select id="timetableform-studentid"><option value="9">Петренко Петро Петрович</option></select></form>
    """.trimIndent()

    private fun studentSchedulePage(token: String) = """
        <form id="filter-form"><input name="_csrf-frontend" value="$token"></form>
        <script>var events = {};</script>
    """.trimIndent()
}
