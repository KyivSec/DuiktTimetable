package com.kyivsec.duikt_timetable.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kyivsec.duikt_timetable.DuiktTimetableApplication
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

/** Independent recovery for delayed/missed alarms or a removed live notification. */
class NotificationScheduleCheckWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = try {
        // Reads only DataStore and Room; no schedule download or activity launch.
        val application = applicationContext as DuiktTimetableApplication
        application.container.notificationCoordinator.refreshNow()
        Result.success()
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        Result.retry()
    }

    companion object {
        private const val WORK_NAME = "persistent_notification_schedule_check"

        internal fun setEnabled(context: Context, enabled: Boolean) {
            val manager = WorkManager.getInstance(context)
            if (enabled) {
                // KEEP avoids postponing the check on every Room emission/boundary alarm.
                // Fifteen minutes is WorkManager's minimum; Doze/OEM policies may delay it.
                manager.enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    PeriodicWorkRequestBuilder<NotificationScheduleCheckWorker>(15, TimeUnit.MINUTES)
                        .build(),
                )
            } else {
                manager.cancelUniqueWork(WORK_NAME)
            }
        }
    }
}
