package com.kyivsec.duikt_timetable

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.kyivsec.duikt_timetable.ui.theme.DarkBackground
import com.kyivsec.duikt_timetable.ui.theme.DarkOutline
import com.kyivsec.duikt_timetable.ui.theme.DarkSurfaceHigh
import com.kyivsec.duikt_timetable.ui.theme.ExamAccentLightForeground
import com.kyivsec.duikt_timetable.ui.theme.LabAccentLightForeground
import com.kyivsec.duikt_timetable.ui.theme.LectureAccentLightForeground
import com.kyivsec.duikt_timetable.ui.theme.LightBackground
import com.kyivsec.duikt_timetable.ui.theme.LightOutline
import com.kyivsec.duikt_timetable.ui.theme.LightSurfaceHigh
import com.kyivsec.duikt_timetable.ui.theme.OtherAccentLightForeground
import com.kyivsec.duikt_timetable.ui.theme.PracticeAccentLightForeground
import com.kyivsec.duikt_timetable.ui.theme.SeminarAccentLightForeground
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeContrastTest {
    @Test fun `light lesson foregrounds meet normal text contrast`() {
        lightLessonForegrounds.forEach { foreground ->
            assertContrastAtLeast(foreground, LightBackground, 4.5f)
        }
    }

    @Test fun `meaningful outlines meet non text contrast`() {
        assertContrastAtLeast(LightOutline, LightBackground, 3f)
        assertContrastAtLeast(LightOutline, LightSurfaceHigh, 3f)
        assertContrastAtLeast(DarkOutline, DarkBackground, 3f)
        assertContrastAtLeast(DarkOutline, DarkSurfaceHigh, 3f)
    }

    private fun assertContrastAtLeast(foreground: Color, background: Color, minimum: Float) {
        val lighter = maxOf(foreground.luminance(), background.luminance())
        val darker = minOf(foreground.luminance(), background.luminance())
        val contrast = (lighter + 0.05f) / (darker + 0.05f)
        assertTrue("Expected contrast >= $minimum but was $contrast", contrast >= minimum)
    }

    private val lightLessonForegrounds = listOf(
        LectureAccentLightForeground,
        PracticeAccentLightForeground,
        LabAccentLightForeground,
        SeminarAccentLightForeground,
        ExamAccentLightForeground,
        OtherAccentLightForeground,
    )
}
