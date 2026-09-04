package com.kyivsec.duikttimetable

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyivsec.duikttimetable.ui.screen.ScheduleScreen
import com.kyivsec.duikttimetable.ui.theme.DuiktTimetableTheme
import com.kyivsec.duikttimetable.util.LanguageHandler
import com.kyivsec.duikttimetable.viewmodel.ScheduleViewModel

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageHandler.wrapContext(newBase))
    }

    private val scheduleViewModel: ScheduleViewModel by viewModels {
        val container = (application as DuiktTimetableApplication).container
        ScheduleViewModel.Factory(
            scheduleRepository = container.scheduleRepository,
            groupDirectoryRepository = container.groupDirectoryRepository,
            preferencesRepository = container.settingsRepository,
            teacherDirectoryRepository = container.teacherDirectoryRepository,
            studentDirectoryRepository = container.studentDirectoryRepository,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by scheduleViewModel.uiState.collectAsStateWithLifecycle()
            DuiktTimetableTheme(themeMode = state.settings.themeMode) {
                ScheduleScreen(state = state, viewModel = scheduleViewModel)
            }
        }
    }
}
