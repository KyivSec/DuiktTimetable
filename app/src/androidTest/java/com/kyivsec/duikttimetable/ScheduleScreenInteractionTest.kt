package com.kyivsec.duikttimetable

import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.FileOutputStream

class ScheduleScreenInteractionTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    @Test fun weekModeAndLessonDetailsWork() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("ПД-31").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Тиждень").performClick()
        composeRule.waitForIdle()
        captureScreen("week-verification.png")
        composeRule.onNodeWithText("Проєктний практикум").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Практична робота").assertIsDisplayed()
    }

    @Test fun groupSelectorWorks() {
        composeRule.waitUntil(timeoutMillis = 5_000) { composeRule.onAllNodesWithText("ПД-31").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("ПД-31").performClick()
        composeRule.onNodeWithText("Оберіть групу").assertIsDisplayed()
        composeRule.onNodeWithText("Інститут").assertIsDisplayed()
        composeRule.onNodeWithText("ННІ Інформаційних технологій").performClick()
        composeRule.onNodeWithText("ННІ Кібербезпеки").performClick()
        composeRule.onNodeWithText("БКС-21").assertIsDisplayed()
        composeRule.onNodeWithText("Скасувати").performClick()
    }

    @Test fun settingsDrawerWorks() {
        composeRule.waitUntil(timeoutMillis = 5_000) { composeRule.onAllNodesWithText("ПД-31").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithContentDescription("Відкрити налаштування").performClick()
        composeRule.onNodeWithText("Налаштування").assertIsDisplayed()
        composeRule.onNodeWithText("Перший день тижня").performClick()
        composeRule.onNodeWithText("Середа").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Перший день тижня").performClick()
        composeRule.onNodeWithText("Понеділок").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Оновити весь розклад").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Оновлення…").assertIsDisplayed()
        captureScreen("settings-reload-verification.png")
    }

    private fun captureScreen(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val output = File(instrumentation.targetContext.getExternalFilesDir(null), name)
        FileOutputStream(output).use { stream ->
            instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
    }
}
