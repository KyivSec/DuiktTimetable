package com.kyivsec.duikt_timetable.notification

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.kyivsec.duikt_timetable.data.SettingsRepository
import com.kyivsec.duikt_timetable.data.StoredPreferences
import com.kyivsec.duikt_timetable.data.local.LessonEntity
import com.kyivsec.duikt_timetable.data.local.TimetableDao
import com.kyivsec.duikt_timetable.data.local.toDomain
import com.kyivsec.duikt_timetable.widget.ScheduleWidgetPublisher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ScheduleNotificationCoordinator(
    private val context: Context,
    private val dao: TimetableDao,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val publisher = NotificationPublisher(context)
    private val widgetPublisher = ScheduleWidgetPublisher(context)
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val updateMutex = Mutex()
    private var observationJob: Job? = null

    fun start() {
        publisher.createChannels()
        observationJob?.cancel()
        observationJob = scope.launch {
            settingsRepository.observe().collectLatest { preferences ->
                val owner = preferences.notificationOwner()
                if ((!preferences.settings.notificationsEnabled && !widgetPublisher.hasWidgets()) || owner == null) {
                    apply(preferences, emptyList())
                    return@collectLatest
                }
                dao.observeNotificationLessons(owner.type, owner.id, LocalDate.now(clock).toString())
                    .collect { rows -> apply(preferences, rows) }
            }
        }
    }

    suspend fun refreshNow() {
        val preferences = settingsRepository.load()
        val owner = preferences.notificationOwner()
        val rows = if (owner != null && (preferences.settings.notificationsEnabled || widgetPublisher.hasWidgets())) {
            dao.notificationLessons(owner.type, owner.id, LocalDate.now(clock).toString())
        } else emptyList()
        apply(preferences, rows)
    }

    private suspend fun apply(preferences: StoredPreferences, rows: List<LessonEntity>) = updateMutex.withLock {
        cancelAlarm()
        val settings = preferences.settings
        val canPost = canPostNotifications()
        val widgetEnabled = widgetPublisher.hasWidgets()
        if (!settings.notificationsEnabled || !canPost) {
            publisher.cancelAll()
        }
        val now = Instant.now(clock)
        val lessons = rows.map { ScheduledLesson(LocalDate.parse(it.date), it.toDomain()) }
        val liveNotificationEnabled = settings.persistentNotificationEnabled && canPost
        val plan = NotificationPlanner(ZoneId.systemDefault()).plan(
            now = now,
            lessons = lessons,
            remindersEnabled = settings.immediateNotificationsEnabled && canPost,
            liveEnabled = liveNotificationEnabled || widgetEnabled,
        )
        if (canPost) plan.reminder?.let { publisher.publishReminder(it, now) }
        if (liveNotificationEnabled) plan.live?.let(publisher::publishLive) ?: publisher.cancelLive()
        else publisher.cancelLive()
        widgetPublisher.publish(
            state = plan.live,
            themeMode = settings.themeMode,
            ownerSelected = preferences.notificationOwner() != null,
        )
        plan.nextEvaluationAt?.let(::scheduleAlarm)
    }

    private fun scheduleAlarm(at: Instant) {
        if (!at.isAfter(Instant.now(clock))) return
        val scheduledExactly = canScheduleExactAlarms() && runCatching {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), alarmIntent(PRIMARY_ALARM))
        }.isSuccess
        if (!scheduledExactly) {
            runCatching {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), alarmIntent(PRIMARY_ALARM))
            }
        }
        val backupAt = at.plus(BACKUP_ALARM_DELAY)
        runCatching {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                backupAt.toEpochMilli(),
                alarmIntent(BACKUP_ALARM),
            )
        }
    }

    private fun cancelAlarm() {
        alarmManager.cancel(alarmIntent(PRIMARY_ALARM))
        alarmManager.cancel(alarmIntent(BACKUP_ALARM))
    }

    private fun alarmIntent(requestCode: Int): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        Intent(context, NotificationAlarmReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun canPostNotifications(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun canScheduleExactAlarms(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private data class OwnerKey(val type: String, val id: Long)

    private fun StoredPreferences.notificationOwner(): OwnerKey? = when (occupation) {
        com.kyivsec.duikt_timetable.model.Occupation.GROUP -> selectedGroupId?.let { OwnerKey(occupation.name, it) }
        com.kyivsec.duikt_timetable.model.Occupation.STUDENT -> selectedStudentId?.let { OwnerKey(occupation.name, it) }
        com.kyivsec.duikt_timetable.model.Occupation.TEACHER -> selectedTeacherId?.let { OwnerKey(occupation.name, it) }
        null -> selectedGroupId?.let { OwnerKey(com.kyivsec.duikt_timetable.model.Occupation.GROUP.name, it) }
    }

    private val com.kyivsec.duikt_timetable.model.ScheduleSettings.notificationsEnabled: Boolean
        get() = immediateNotificationsEnabled || persistentNotificationEnabled

    private companion object {
        const val PRIMARY_ALARM = 0
        const val BACKUP_ALARM = 1
        val BACKUP_ALARM_DELAY: Duration = Duration.ofMinutes(1)
    }
}
