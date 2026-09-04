package com.kyivsec.duikttimetable.data.remote

import com.kyivsec.duikttimetable.data.DateRange
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class DuiktScheduleClient(
    private val baseUrl: String = "https://e-rozklad.duikt.edu.ua",
    private val client: OkHttpClient = defaultClient(),
    private val filterParser: DuiktFilterPageParser = DuiktFilterPageParser(),
    private val eventsExtractor: EmbeddedEventsJsonExtractor = EmbeddedEventsJsonExtractor(),
) {
    private val sessionMutex = Mutex()
    private var csrf: String? = null

    suspend fun fetchInstitutes(): List<RemoteOption> = sessionMutex.withLock {
        val html = getInitialPage(force = true)
        filterParser.faculties(html)
    }

    suspend fun fetchCourses(facultyId: Long): List<Int> = sessionMutex.withLock {
        val html = postForm(facultyId, null, null, null, type = 0)
        filterParser.courses(html)
    }

    suspend fun fetchGroups(facultyId: Long, course: Int): List<RemoteOption> = sessionMutex.withLock {
        val html = postForm(facultyId, course, null, null, type = 0)
        filterParser.groups(html)
    }

    suspend fun fetchSchedule(facultyId: Long, course: Int, groupId: Long, range: DateRange): RemoteSchedule = sessionMutex.withLock {
        val html = postForm(facultyId, course, groupId, range, type = 1)
        RemoteSchedule(eventsExtractor.extract(html), filterParser.semesterRange(html))
    }

    suspend fun fetchChairs(): List<RemoteOption> = sessionMutex.withLock {
        filterParser.chairs(getTeacherInitialPage(force = true))
    }

    suspend fun fetchTeachers(chairId: Long): List<RemoteOption> = sessionMutex.withLock {
        filterParser.teachers(postTeacherForm(chairId, null, null, type = 0))
    }

    suspend fun fetchTeacherSchedule(chairId: Long, teacherId: Long, range: DateRange): RemoteSchedule = sessionMutex.withLock {
        val html = postTeacherForm(chairId, teacherId, range, type = 1)
        RemoteSchedule(eventsExtractor.extract(html), filterParser.semesterRange(html))
    }

    suspend fun fetchStudents(facultyId: Long, course: Int, groupId: Long): List<RemoteOption> = sessionMutex.withLock {
        filterParser.students(postStudentForm(facultyId, course, groupId, null, null, type = 0))
    }

    suspend fun fetchStudentInstitutes(): List<RemoteOption> = sessionMutex.withLock {
        filterParser.faculties(getStudentInitialPage(force = true))
    }

    suspend fun fetchStudentCourses(facultyId: Long): List<Int> = sessionMutex.withLock {
        filterParser.courses(postStudentForm(facultyId, null, null, null, null, type = 0))
    }

    suspend fun fetchStudentGroups(facultyId: Long, course: Int): List<RemoteOption> = sessionMutex.withLock {
        filterParser.groups(postStudentForm(facultyId, course, null, null, null, type = 0))
    }

    suspend fun fetchStudentSchedule(
        facultyId: Long,
        course: Int,
        groupId: Long,
        studentId: Long,
        range: DateRange,
    ): RemoteSchedule = sessionMutex.withLock {
        val html = postStudentForm(facultyId, course, groupId, studentId, range, type = 1)
        RemoteSchedule(eventsExtractor.extract(html), filterParser.semesterRange(html))
    }

    private suspend fun getInitialPage(force: Boolean = false): String {
        if (!force && csrf != null) return ""
        val html = execute(Request.Builder().url("$baseUrl/time-table/group").get().build())
        csrf = filterParser.csrf(html)
        return html
    }

    private suspend fun getTeacherInitialPage(force: Boolean = false): String {
        if (!force && csrf != null) return ""
        val html = execute(Request.Builder().url("$baseUrl/time-table/teacher").get().build())
        csrf = filterParser.csrf(html)
        return html
    }

    private suspend fun getStudentInitialPage(force: Boolean = false): String {
        if (!force && csrf != null) return ""
        val html = execute(Request.Builder().url("$baseUrl/time-table/student?type=0").get().build())
        csrf = filterParser.csrf(html)
        return html
    }

    private suspend fun postStudentForm(
        facultyId: Long,
        course: Int?,
        groupId: Long?,
        studentId: Long?,
        range: DateRange?,
        type: Int,
        replayed: Boolean = false,
    ): String {
        if (csrf == null) getStudentInitialPage(force = true)
        val body = FormBody.Builder()
            .add("_csrf-frontend", csrf.orEmpty())
            .add("TimeTableForm[facultyId]", facultyId.toString())
            .add("TimeTableForm[course]", course?.toString().orEmpty())
            .add("TimeTableForm[groupId]", groupId?.toString().orEmpty())
            .add("TimeTableForm[studentId]", studentId?.toString().orEmpty())
            .apply {
                if (range != null) {
                    add("TimeTableForm[dateStart]", range.startInclusive.format(SOURCE_DATE_FORMAT))
                    add("TimeTableForm[dateEnd]", range.endInclusive.format(SOURCE_DATE_FORMAT))
                    add("TimeTableForm[indicationDays]", "5")
                    add("time-table-type", "1")
                }
            }.build()
        val request = Request.Builder().url("$baseUrl/time-table/student?type=$type").post(body).build()
        return try {
            execute(request).also { csrf = filterParser.csrf(it) }
        } catch (error: HttpStatusException) {
            if (error.statusCode == 403 && !replayed) {
                csrf = null
                getStudentInitialPage(force = true)
                postStudentForm(facultyId, course, groupId, studentId, range, type, replayed = true)
            } else if (error.statusCode == 403) throw RejectedSessionException() else throw error
        }
    }

    private suspend fun postTeacherForm(
        chairId: Long,
        teacherId: Long?,
        range: DateRange?,
        type: Int,
        replayed: Boolean = false,
    ): String {
        if (csrf == null) getTeacherInitialPage(force = true)
        val body = FormBody.Builder()
            .add("_csrf-frontend", csrf.orEmpty())
            .add("TimeTableForm[chairId]", chairId.toString())
            .add("TimeTableForm[teacherId]", teacherId?.toString().orEmpty())
            .apply {
                if (range != null) {
                    add("TimeTableForm[dateStart]", range.startInclusive.format(SOURCE_DATE_FORMAT))
                    add("TimeTableForm[dateEnd]", range.endInclusive.format(SOURCE_DATE_FORMAT))
                    add("TimeTableForm[indicationDays]", "5")
                    add("time-table-type", "1")
                }
            }.build()
        val request = Request.Builder().url("$baseUrl/time-table/teacher?type=$type").post(body).build()
        return try {
            execute(request).also { csrf = filterParser.csrf(it) }
        } catch (error: HttpStatusException) {
            if (error.statusCode == 403 && !replayed) {
                csrf = null
                getTeacherInitialPage(force = true)
                postTeacherForm(chairId, teacherId, range, type, replayed = true)
            } else if (error.statusCode == 403) throw RejectedSessionException() else throw error
        }
    }

    private suspend fun postForm(
        facultyId: Long,
        course: Int?,
        groupId: Long?,
        range: DateRange?,
        type: Int,
        replayed: Boolean = false,
    ): String {
        if (csrf == null) getInitialPage(force = true)
        val body = FormBody.Builder()
            .add("_csrf-frontend", csrf.orEmpty())
            .add("TimeTableForm[facultyId]", facultyId.toString())
            .add("TimeTableForm[course]", course?.toString().orEmpty())
            .add("TimeTableForm[groupId]", groupId?.toString().orEmpty())
            .apply {
                if (range != null) {
                    add("TimeTableForm[dateStart]", range.startInclusive.format(SOURCE_DATE_FORMAT))
                    add("TimeTableForm[dateEnd]", range.endInclusive.format(SOURCE_DATE_FORMAT))
                    add("TimeTableForm[indicationDays]", "5")
                    add("time-table-type", "1")
                }
            }.build()
        val request = Request.Builder().url("$baseUrl/time-table/group?type=$type").post(body).build()
        return try {
            execute(request).also { csrf = filterParser.csrf(it) }
        } catch (error: HttpStatusException) {
            if (error.statusCode == 403 && !replayed) {
                csrf = null
                getInitialPage(force = true)
                postForm(facultyId, course, groupId, range, type, replayed = true)
            } else if (error.statusCode == 403) throw RejectedSessionException() else throw error
        }
    }

    private suspend fun execute(request: Request): String {
        var lastError: Exception? = null
        repeat(3) { attempt ->
            try {
                val response = executeOnce(request)
                if (response.code in 200..299) return response.body
                if (response.code !in RETRYABLE_CODES || attempt == 2) throw HttpStatusException(response.code)
                lastError = HttpStatusException(response.code)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (error !is IOException || attempt == 2) throw error
                lastError = error
            }
            delay(250L shl attempt)
        }
        throw lastError ?: IOException("Request failed")
    }

    private suspend fun executeOnce(request: Request): NetworkResponse = suspendCancellableCoroutine { continuation ->
        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val value = runCatching { NetworkResponse(it.code, it.body.string()) }
                    if (continuation.isActive) value.fold(continuation::resume, continuation::resumeWithException)
                }
            }
        })
    }

    private data class NetworkResponse(val code: Int, val body: String)

    companion object {
        private val RETRYABLE_CODES = setOf(408, 429, 500, 502, 503, 504)
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .cookieJar(MemoryCookieJar())
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder()
                    .header("Accept-Language", "uk-UA,uk;q=0.9")
                    .header("User-Agent", "DuiktTimetable/1.0 Android")
                    .build())
            }.build()
    }
}

private class MemoryCookieJar : CookieJar {
    private val cookies = mutableListOf<Cookie>()
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        synchronized(this.cookies) {
            this.cookies.removeAll { old -> cookies.any { it.name == old.name && it.domain == old.domain && it.path == old.path } }
            this.cookies.addAll(cookies)
        }
    }
    override fun loadForRequest(url: HttpUrl): List<Cookie> = synchronized(cookies) {
        cookies.removeAll { it.expiresAt < System.currentTimeMillis() }
        cookies.filter { it.matches(url) }
    }
}
