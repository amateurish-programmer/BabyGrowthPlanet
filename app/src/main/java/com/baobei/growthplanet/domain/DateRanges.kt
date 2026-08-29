package com.baobei.growthplanet.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

data class DateRange(val start: LocalDate, val end: LocalDate) {
    val startText: String get() = start.toString()
    val endText: String get() = end.toString()
}

object DateRanges {
    fun week(date: LocalDate): DateRange {
        val start = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return DateRange(start, start.plusDays(6))
    }

    fun month(date: LocalDate): DateRange {
        val month = YearMonth.from(date)
        return DateRange(month.atDay(1), month.atEndOfMonth())
    }

    fun year(date: LocalDate): DateRange =
        DateRange(LocalDate.of(date.year, 1, 1), LocalDate.of(date.year, 12, 31))
}
