package com.kyivsec.duikttimetable

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kyivsec.duikttimetable.data.MockDataLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class MockDataLoaderTest {
    @Test fun assetsParseAndRebaseAroundCurrentWeek() {
        val clock = Clock.fixed(Instant.parse("2026-08-28T10:00:00Z"), ZoneOffset.UTC)
        val loader = MockDataLoader(ApplicationProvider.getApplicationContext(), clock)
        val weeks = loader.loadSchedule()
        assertEquals(3, weeks.size)
        assertTrue(weeks.flatMap { it.days }.any { it.date == LocalDate.of(2026, 8, 28) })
        assertNotNull(weeks.flatMap { it.days }.flatMap { it.lessons }.firstOrNull { it.onlineUrl != null })
        assertTrue(loader.loadGroups().flatMap { it.courses }.flatMap { it.groups }.any { it.name == "ПД-31" })
    }
}
