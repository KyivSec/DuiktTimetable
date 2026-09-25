package com.kyivsec.duikt_timetable.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.SystemClock
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.kyivsec.duikt_timetable.MainActivity
import com.kyivsec.duikt_timetable.R
import com.kyivsec.duikt_timetable.model.LessonType
import com.kyivsec.duikt_timetable.model.ThemeMode
import com.kyivsec.duikt_timetable.notification.LiveNotificationState
import com.kyivsec.duikt_timetable.util.LanguageHandler
import kotlin.math.min

class ScheduleWidgetPublisher(private val applicationContext: Context) {
    private val manager = AppWidgetManager.getInstance(applicationContext)

    fun hasWidgets(): Boolean = widgetIds().isNotEmpty()

    fun publish(state: LiveNotificationState?, themeMode: ThemeMode, ownerSelected: Boolean) {
        val ids = widgetIds()
        if (ids.isEmpty()) return

        ids.forEach { id ->
            val views = buildRemoteViews(state, themeMode, ownerSelected)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val options = manager.getAppWidgetOptions(id)
                val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
                val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
                val size = min(width, height)
                if (size > 0) {
                    views.setViewLayoutWidth(R.id.widget_square, size.toFloat(), TypedValue.COMPLEX_UNIT_DIP)
                    views.setViewLayoutHeight(R.id.widget_square, size.toFloat(), TypedValue.COMPLEX_UNIT_DIP)
                }
            }
            manager.updateAppWidget(id, views)
        }
    }

    internal fun buildRemoteViews(
        state: LiveNotificationState?,
        themeMode: ThemeMode,
        ownerSelected: Boolean,
    ): RemoteViews {
        val context = LanguageHandler.wrapContext(applicationContext)
        val palette = widgetPalette(themeMode)
        return RemoteViews(context.packageName, R.layout.widget_schedule).apply {
            setInt(R.id.widget_background, "setColorFilter", palette.background)
            setTextColor(R.id.widget_empty, palette.primaryText)
            setOnClickPendingIntent(R.id.widget_root, contentIntent())

            if (state == null) {
                setViewVisibility(R.id.widget_content, View.GONE)
                setViewVisibility(R.id.widget_empty, View.VISIBLE)
                setTextViewText(
                    R.id.widget_empty,
                    context.getString(if (ownerSelected) R.string.widget_no_class else R.string.widget_select_owner),
                )
                return@apply
            }

            val lesson = state.occurrence.lesson
            val location = lesson.room?.takeIf(String::isNotBlank)?.let { context.getString(R.string.room_short, it) }
                ?: context.getString(
                    if (lesson.onlineUrl != null) R.string.notification_online
                    else R.string.notification_room_unavailable,
                )
            setViewVisibility(R.id.widget_content, View.VISIBLE)
            setViewVisibility(R.id.widget_empty, View.GONE)
            setInt(R.id.widget_timer_background, "setColorFilter", palette.timerBackground)
            setTextColor(R.id.widget_status, lesson.type.widgetForegroundAccent(palette.light))
            setTextColor(R.id.widget_subject, palette.primaryText)
            setTextColor(R.id.widget_location, palette.secondaryText)
            setTextColor(R.id.widget_countdown, palette.primaryText)
            setTextViewText(
                R.id.widget_status,
                context.getString(
                    if (state is LiveNotificationState.Current) R.string.widget_current_class
                    else R.string.widget_next_class,
                ),
            )
            setTextViewText(R.id.widget_subject, lesson.subject)
            setTextViewText(R.id.widget_location, location)
            setChronometer(
                R.id.widget_countdown,
                SystemClock.elapsedRealtime() + state.countdownTarget.toEpochMilli() - System.currentTimeMillis(),
                null,
                true,
            )
            setChronometerCountDown(R.id.widget_countdown, true)
        }
    }

    private fun widgetIds(): IntArray = manager.getAppWidgetIds(
        ComponentName(applicationContext, ScheduleAppWidgetProvider::class.java),
    )

    private fun contentIntent(): PendingIntent = PendingIntent.getActivity(
        applicationContext,
        2,
        Intent(applicationContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun widgetPalette(themeMode: ThemeMode): WidgetPalette {
        val systemDark = applicationContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES
        return when (themeMode) {
            ThemeMode.SYSTEM -> if (systemDark) DARK_PALETTE else LIGHT_PALETTE
            ThemeMode.LIGHT -> LIGHT_PALETTE
            ThemeMode.DARK -> DARK_PALETTE
            ThemeMode.HIGH_CONTRAST_LIGHT -> HIGH_CONTRAST_LIGHT_PALETTE
            ThemeMode.HIGH_CONTRAST_DARK -> HIGH_CONTRAST_DARK_PALETTE
            ThemeMode.OLED -> OLED_PALETTE
        }
    }

    private data class WidgetPalette(
        val background: Int,
        val primaryText: Int,
        val secondaryText: Int,
        val timerBackground: Int,
        val light: Boolean,
    )

    private fun LessonType.widgetAccent(): Int = when (this) {
        LessonType.LECTURE -> 0xFF4F83E7.toInt()
        LessonType.PRACTICE -> 0xFF4FB178.toInt()
        LessonType.LAB -> 0xFFAB6ED3.toInt()
        LessonType.SEMINAR -> 0xFFE1B629.toInt()
        LessonType.EXAM -> 0xFFE06C75.toInt()
        LessonType.OTHER -> 0xFF78909C.toInt()
    }

    private fun LessonType.widgetForegroundAccent(light: Boolean): Int {
        if (!light) return widgetAccent()
        return when (this) {
            LessonType.LECTURE -> 0xFF356AC8.toInt()
            LessonType.PRACTICE -> 0xFF287A4D.toInt()
            LessonType.LAB -> 0xFF7D45A5.toInt()
            LessonType.SEMINAR -> 0xFF806500.toInt()
            LessonType.EXAM -> 0xFFB5404B.toInt()
            LessonType.OTHER -> 0xFF526A75.toInt()
        }
    }

    private companion object {
        val LIGHT_PALETTE = WidgetPalette(0xFFECECEC.toInt(), 0xFF171717.toInt(), 0xFF646464.toInt(), 0xFFE0E0E0.toInt(), true)
        val DARK_PALETTE = WidgetPalette(0xFF303030.toInt(), 0xFFECECEC.toInt(), 0xFFB4B4B4.toInt(), 0xFF3A3A3A.toInt(), false)
        val OLED_PALETTE = WidgetPalette(0xFF0D0D0D.toInt(), 0xFFE6E6E6.toInt(), 0xFFBDBDBD.toInt(), 0xFF121212.toInt(), false)
        val HIGH_CONTRAST_LIGHT_PALETTE = WidgetPalette(0xFFF2F2F2.toInt(), 0xFF000000.toInt(), 0xFF333333.toInt(), 0xFFE8E8E8.toInt(), true)
        val HIGH_CONTRAST_DARK_PALETTE = WidgetPalette(0xFF121212.toInt(), 0xFFFFFFFF.toInt(), 0xFFE0E0E0.toInt(), 0xFF1F1F1F.toInt(), false)
    }
}
