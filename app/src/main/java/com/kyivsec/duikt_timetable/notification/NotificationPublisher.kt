package com.kyivsec.duikt_timetable.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kyivsec.duikt_timetable.MainActivity
import com.kyivsec.duikt_timetable.R
import com.kyivsec.duikt_timetable.util.LanguageHandler
import java.time.Duration
import java.time.Instant

class NotificationPublisher(private val applicationContext: Context) {
    fun createChannels() {
        val context = LanguageHandler.wrapContext(applicationContext)
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(listOf(
            NotificationChannel(
                REMINDER_CHANNEL_ID,
                context.getString(R.string.notification_channel_reminders),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = context.getString(R.string.notification_channel_reminders_description) },
            NotificationChannel(
                LIVE_CHANNEL_ID,
                context.getString(R.string.notification_channel_live),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.notification_channel_live_description)
                setSound(null, null)
                enableVibration(false)
            },
        ))
    }

    fun publishReminder(occurrence: ScheduledLesson, now: Instant) {
        val context = LanguageHandler.wrapContext(applicationContext)
        val lesson = occurrence.lesson
        val text = lesson.room?.takeIf(String::isNotBlank)?.let { room ->
            context.getString(R.string.notification_reminder_room, lesson.subject, room)
        } ?: context.getString(R.string.notification_reminder, lesson.subject)
        val expiresIn = Duration.between(now, occurrence.date.atTime(lesson.startTime)
            .atZone(java.time.ZoneId.systemDefault()).toInstant()).toMillis().coerceAtLeast(1L)
        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_reminder_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent())
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setTimeoutAfter(expiresIn)
            .setOnlyAlertOnce(true)
            .build()
        notify(occurrence.lesson.id.hashCode(), notification)
    }

    fun publishLive(state: LiveNotificationState) {
        notify(LIVE_NOTIFICATION_ID, buildLiveNotification(state))
    }

    internal fun buildLiveNotification(state: LiveNotificationState): android.app.Notification {
        val context = LanguageHandler.wrapContext(applicationContext)
        val lesson = state.occurrence.lesson
        val title = when (state) {
            is LiveNotificationState.Current -> context.getString(R.string.notification_current_class, lesson.subject)
            is LiveNotificationState.Next -> context.getString(R.string.notification_next_class, lesson.subject)
        }
        val location = lesson.room?.takeIf(String::isNotBlank)?.let { context.getString(R.string.room_short, it) }
            ?: context.getString(if (lesson.onlineUrl != null) R.string.notification_online else R.string.notification_room_unavailable)
        val contentView = RemoteViews(context.packageName, R.layout.notification_live).apply {
            setTextViewText(R.id.notification_lesson, title)
            setTextViewText(R.id.notification_location, location)
            setChronometer(
                R.id.notification_countdown,
                SystemClock.elapsedRealtime() + state.countdownTarget.toEpochMilli() - System.currentTimeMillis(),
                null,
                true,
            )
            setChronometerCountDown(R.id.notification_countdown, true)
        }
        return NotificationCompat.Builder(context, LIVE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(location)
            .setShowWhen(false)
            .setCustomContentView(contentView)
            .setCustomBigContentView(contentView)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setContentIntent(contentIntent())
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .build()
    }

    fun cancelLive() {
        NotificationManagerCompat.from(applicationContext).cancel(LIVE_NOTIFICATION_ID)
    }

    fun cancelAll() {
        NotificationManagerCompat.from(applicationContext).cancelAll()
    }

    private fun contentIntent(): PendingIntent = PendingIntent.getActivity(
        applicationContext,
        0,
        Intent(applicationContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun notify(id: Int, notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return
        NotificationManagerCompat.from(applicationContext).notify(id, notification)
    }

    companion object {
        const val REMINDER_CHANNEL_ID = "class_reminders"
        const val LIVE_CHANNEL_ID = "live_timetable"
        private const val LIVE_NOTIFICATION_ID = 10_001
    }
}
