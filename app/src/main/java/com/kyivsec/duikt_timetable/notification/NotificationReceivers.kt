package com.kyivsec.duikt_timetable.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kyivsec.duikt_timetable.DuiktTimetableApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = refresh(context)
}

class NotificationRecoveryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in SUPPORTED_ACTIONS) return
        refresh(context)
    }

    companion object {
        private val SUPPORTED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
        )
    }
}

private fun BroadcastReceiver.refresh(context: Context) {
    val result = goAsync()
    val application = context.applicationContext as DuiktTimetableApplication
    CoroutineScope(Dispatchers.Default).launch {
        try {
            application.container.notificationCoordinator.refreshNow()
        } finally {
            result.finish()
        }
    }
}
