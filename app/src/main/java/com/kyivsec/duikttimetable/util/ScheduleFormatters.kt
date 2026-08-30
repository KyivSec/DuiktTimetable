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

fun pairCountText(count: Int): String {
    val lastTwo = count % 100
    val last = count % 10
    val noun = when {
        lastTwo in 11..14 -> "пар"
        last == 1 -> "пара"
        last in 2..4 -> "пари"
        else -> "пар"
    }
    return "$count $noun"
}

fun LocalDate.shortDayName(): String = dayOfWeek.getDisplayName(TextStyle.SHORT, UkrainianLocale)
    .removeSuffix(".")
    .replaceFirstChar { it.uppercase(UkrainianLocale) }

fun LocalDate.fullDayName(): String = dayOfWeek.getDisplayName(TextStyle.FULL, UkrainianLocale)
    .replaceFirstChar { it.uppercase(UkrainianLocale) }

fun DayOfWeek.ukrainianName(): String = getDisplayName(TextStyle.FULL, UkrainianLocale)
    .replaceFirstChar { it.uppercase(UkrainianLocale) }

fun LocalDate.dayMonth(): String = format(DateTimeFormatter.ofPattern("d MMMM", UkrainianLocale))

fun weekRangeText(start: LocalDate, end: LocalDate): String {
    val startPattern = if (start.month == end.month) "d" else "d MMM"
    val startText = start.format(DateTimeFormatter.ofPattern(startPattern, UkrainianLocale))
    val endText = end.format(DateTimeFormatter.ofPattern("d MMM yyyy", UkrainianLocale))
    return "$startText – $endText"
}
