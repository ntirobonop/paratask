package io.github.ntirobonop.paratask.feature.upcoming

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

fun LocalDate.isoWeekStart(): LocalDate =
    with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

fun weekDates(weekStart: LocalDate): List<LocalDate> {
    require(weekStart.dayOfWeek == DayOfWeek.MONDAY) { "Week must start on Monday" }
    return List(DAYS_IN_WEEK) { offset -> weekStart.plusDays(offset.toLong()) }
}

private const val DAYS_IN_WEEK = 7
