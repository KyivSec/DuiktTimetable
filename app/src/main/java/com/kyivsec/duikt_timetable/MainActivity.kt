package com.kyivsec.duikt_timetable

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyivsec.duikt_timetable.ui.screen.ScheduleScreen
import com.kyivsec.duikt_timetable.ui.theme.DuiktTimetableTheme
import com.kyivsec.duikt_timetable.util.LanguageHandler
import com.kyivsec.duikt_timetable.viewmodel.ScheduleViewModel

class MainActivity : ComponentActivity() {
    private enum class NotificationSetting { IMMEDIATE, PERSISTENT }

    private var pendingNotificationSetting: NotificationSetting? = null

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) continueEnablingNotifications()
        else denyPendingNotificationSetting(R.string.notification_permission_required)
    }

    private val exactAlarmPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        if (canScheduleExactAlarms()) enablePendingNotificationSetting()
        else denyPendingNotificationSetting(R.string.exact_alarm_permission_required)
    }

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
                ScheduleScreen(
                    state = state,
                    viewModel = scheduleViewModel,
                    onImmediateNotificationsChange = { enabled ->
                        updateNotificationSetting(NotificationSetting.IMMEDIATE, enabled)
                    },
                    onPersistentNotificationChange = { enabled ->
                        updateNotificationSetting(NotificationSetting.PERSISTENT, enabled)
                    },
                )
            }
        }
    }

    private fun updateNotificationSetting(setting: NotificationSetting, enabled: Boolean) {
        if (!enabled) {
            when (setting) {
                NotificationSetting.IMMEDIATE -> scheduleViewModel.updateImmediateNotifications(false)
                NotificationSetting.PERSISTENT -> scheduleViewModel.updatePersistentNotification(false)
            }
            return
        }
        pendingNotificationSetting = setting
        continueEnablingNotifications()
    }

    private fun continueEnablingNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        if (!canScheduleExactAlarms()) {
            exactAlarmPermissionLauncher.launch(Intent(
                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.parse("package:$packageName"),
            ))
            return
        }
        enablePendingNotificationSetting()
    }

    private fun canScheduleExactAlarms(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    private fun enablePendingNotificationSetting() {
        when (pendingNotificationSetting) {
            NotificationSetting.IMMEDIATE -> scheduleViewModel.updateImmediateNotifications(true)
            NotificationSetting.PERSISTENT -> scheduleViewModel.updatePersistentNotification(true)
            null -> return
        }
        pendingNotificationSetting = null
    }

    private fun denyPendingNotificationSetting(message: Int) {
        pendingNotificationSetting = null
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
