package com.kyivsec.duikt_timetable.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import com.kyivsec.duikt_timetable.DuiktTimetableApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

open class ScheduleAppWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) = refresh(context)

    override fun onEnabled(context: Context) = refresh(context)

    override fun onDisabled(context: Context) = refresh(context)

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) = refresh(context)

    private fun refresh(context: Context) {
        val result = goAsync()
        val application = context.applicationContext as DuiktTimetableApplication
        val coordinator = application.container.notificationCoordinator
        coordinator.start()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                coordinator.refreshNow()
            } finally {
                result.finish()
            }
        }
    }
}
