package com.reachravi55.mydailyroutine.data

import com.reachravi55.mydailyroutine.proto.RepeatRule
import com.reachravi55.mydailyroutine.proto.Task
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object RepeatEngine {

    fun occursOn(task: Task, date: LocalDate): Boolean {
        if (task.archived) return false
        if (task.startDate.isBlank()) return false

        val start = runCatching { LocalDate.parse(task.startDate) }.getOrNull() ?: return false
        if (date.isBefore(start)) return false

        val rule = task.repeatRule
        val until = if (rule.untilDate.isNullOrBlank()) null else runCatching { LocalDate.parse(rule.untilDate) }.getOrNull()
        if (until != null && date.isAfter(until)) return false

        val interval = if (rule.interval <= 0) 1 else rule.interval

        return when (rule.frequency) {
            RepeatRule.Frequency.NONE -> date == start
            RepeatRule.Frequency.DAILY -> {
                val days = ChronoUnit.DAYS.between(start, date)
                days % interval == 0L
            }
            RepeatRule.Frequency.WEEKLY -> {
                val weeks = ChronoUnit.WEEKS.between(start, date)
                if (weeks % interval != 0L) return false
                val by = rule.byWeekdayList
                val dow = date.dayOfWeek.value // 1..7
                if (by.isEmpty()) dow == start.dayOfWeek.value else by.contains(dow)
            }
            RepeatRule.Frequency.MONTHLY -> {
                // Simple monthly: same day-of-month (or rule.day_of_month)
                val dom = if (rule.dayOfMonth in 1..31) rule.dayOfMonth else start.dayOfMonth
                val months = ChronoUnit.MONTHS.between(start.withDayOfMonth(1), date.withDayOfMonth(1))
                if (months % interval != 0L) return false
                date.dayOfMonth == dom
            }
            RepeatRule.Frequency.YEARLY -> {
                val years = ChronoUnit.YEARS.between(start.withDayOfYear(1), date.withDayOfYear(1))
                if (years % interval != 0L) return false
                date.month == start.month && date.dayOfMonth == start.dayOfMonth
            }
            else -> false
        }
    }

    fun occurrences(task: Task, from: LocalDate, to: LocalDate): List<LocalDate> {
        val out = mutableListOf<LocalDate>()
        var d = from
        while (!d.isAfter(to)) {
            if (occursOn(task, d)) out.add(d)
            d = d.plusDays(1)
        }
        return out
    }
}
