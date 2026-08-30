package com.kyivsec.duikttimetable

import com.kyivsec.duikttimetable.data.remote.DuiktScheduleClient
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

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

    private fun initialPage(token: String) = """
        <form id="filter-form"><input name="_csrf-frontend" value="$token">
        <select id="timetableform-facultyid"><option value="1">ННІ ІТ</option></select></form>
    """.trimIndent()

    private fun coursePage(token: String) = """
        <form id="filter-form"><input name="_csrf-frontend" value="$token">
        <select id="timetableform-course"><option value="1">1</option><option value="2">2</option><option value="3">3</option></select></form>
    """.trimIndent()
}
