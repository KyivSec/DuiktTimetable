package com.kyivsec.duikttimetable.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

val UkrainianLocale: Locale = Locale.forLanguageTag("uk-UA")

fun weekStart(date: LocalDate, firstDay: DayOfWeek): LocalDate =
    date.with(TemporalAdjusters.previousOrSame(firstDay))

fun LocalDate.shortDayName(locale: Locale = UkrainianLocale): String = dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
    .removeSuffix(".")
    .replaceFirstChar { it.uppercase(locale) }

fun LocalDate.fullDayName(locale: Locale = UkrainianLocale): String = dayOfWeek.getDisplayName(TextStyle.FULL, locale)
    .replaceFirstChar { it.uppercase(locale) }

fun LocalDate.dayMonth(locale: Locale = UkrainianLocale): String = format(DateTimeFormatter.ofPattern("d MMMM", locale))

fun weekRangeText(start: LocalDate, end: LocalDate, locale: Locale = UkrainianLocale): String {
    val startPattern = if (start.month == end.month) "d" else "d MMM"
    val startText = start.format(DateTimeFormatter.ofPattern(startPattern, locale))
    val endText = end.format(DateTimeFormatter.ofPattern("d MMM yyyy", locale))
    return "$startText – $endText"
}
