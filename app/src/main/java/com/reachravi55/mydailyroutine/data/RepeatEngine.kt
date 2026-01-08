package com.reachravi55.mydailyroutine.data

import com.reachravi55.mydailyroutine.proto.RepeatRule
import com.reachravi55.mydailyroutine.proto.Task
import java.time.DayOfWeek
import java.time.LocalDate

object RepeatEngine {

    /**
     * Generate occurrence dates for [task] between [start] and [end] inclusive.
     * Notes:
     * - If repeat=NONE: only start_date is returned (if in range).
     * - WEEKLY: uses weekdays list if present; otherwise repeats on start_date's weekday.
     * - MONTHLY: uses day_of_month if set; otherwise uses start_date day-of-month.
     * - YEARLY: repeats on start_date month/day with interval.
     */
    fun occurrences(task: Task, start: LocalDate, end: LocalDate): List<LocalDate> {
        if (task.startDate.isBlank()) return emptyList()
        val startDate = parseDateKey(task.startDate)
        val until = if (task.repeat.untilDate.isBlank()) null else parseDateKey(task.repeat.untilDate)

        fun inWindow(d: LocalDate): Boolean {
            if (d.isBefore(start) || d.isAfter(end)) return false
            if (d.isBefore(startDate)) return false
            if (until != null && d.isAfter(until)) return false
            return true
        }

        val freq = task.repeat.frequency
        val interval = (if (task.repeat.interval <= 0) 1 else task.repeat.interval)

        return when (freq) {
            RepeatRule.Frequency.NONE -> {
                listOfNotNull(startDate.takeIf { inWindow(it) })
            }
            RepeatRule.Frequency.DAILY -> {
                val out = mutableListOf<LocalDate>()
                // Find first date >= start that aligns with interval
                var d = startDate
                if (d.isBefore(start)) {
                    val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(d, start).toInt()
                    val skip = (daysBetween / interval) * interval
                    d = d.plusDays(skip.toLong())
                    while (d.isBefore(start)) d = d.plusDays(interval.toLong())
                }
                while (!d.isAfter(end)) {
                    if (inWindow(d)) out.add(d)
                    d = d.plusDays(interval.toLong())
                }
                out
            }
            RepeatRule.Frequency.WEEKLY -> {
                val weekdays = if (task.repeat.weekdaysList.isNotEmpty()) {
                    task.repeat.weekdaysList.mapNotNull { runCatching { DayOfWeek.of(it) }.getOrNull() }.toSet()
                } else {
                    setOf(startDate.dayOfWeek)
                }
                val out = mutableListOf<LocalDate>()

                // Align weeks to startDate as week 0
                var cursor = start
                if (cursor.isBefore(startDate)) cursor = startDate

                // Move cursor to Monday of its week for consistent stepping
                val weekStart = cursor.minusDays((cursor.dayOfWeek.value - 1).toLong())

                var w = weekStart
                while (!w.isAfter(end)) {
                    val weeksFromStart = java.time.temporal.ChronoUnit.WEEKS.between(
                        startDate.minusDays((startDate.dayOfWeek.value - 1).toLong()),
                        w
                    ).toInt()
                    if (weeksFromStart >= 0 && weeksFromStart % interval == 0) {
                        for (dow in weekdays) {
                            val d = w.plusDays((dow.value - 1).toLong())
                            if (inWindow(d)) out.add(d)
                        }
                    }
                    w = w.plusWeeks(1)
                }
                out.sorted()
            }
            RepeatRule.Frequency.MONTHLY -> {
                val out = mutableListOf<LocalDate>()
                val dom = if (task.repeat.dayOfMonth <= 0) startDate.dayOfMonth else task.repeat.dayOfMonth
                var d = LocalDate.of(startDate.year, startDate.month, 1)
                // step month by month
                val startMonth = LocalDate.of(start.year, start.month, 1)
                if (d.isBefore(startMonth)) d = startMonth
                // align to interval based on startDate month
                while (!d.isAfter(LocalDate.of(end.year, end.month, 1))) {
                    val monthsFromStart = java.time.temporal.ChronoUnit.MONTHS.between(
                        LocalDate.of(startDate.year, startDate.month, 1),
                        d
                    ).toInt()
                    if (monthsFromStart >= 0 && monthsFromStart % interval == 0) {
                        val day = dom.coerceAtMost(d.lengthOfMonth())
                        val occ = LocalDate.of(d.year, d.month, day)
                        if (inWindow(occ)) out.add(occ)
                    }
                    d = d.plusMonths(1)
                }
                out
            }
            RepeatRule.Frequency.YEARLY -> {
                val out = mutableListOf<LocalDate>()
                var year = start.year
                val md = startDate.month
                val dom = startDate.dayOfMonth
                while (year <= end.year) {
                    val yearsFromStart = year - startDate.year
                    if (yearsFromStart >= 0 && yearsFromStart % interval == 0) {
                        val day = dom.coerceAtMost(LocalDate.of(year, md, 1).lengthOfMonth())
                        val occ = LocalDate.of(year, md, day)
                        if (inWindow(occ)) out.add(occ)
                    }
                    year++
                }
                out
            }
            else -> emptyList()
        }
    }
}
