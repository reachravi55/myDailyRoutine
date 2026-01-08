package com.reachravi55.mydailyroutine.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.reachravi55.mydailyroutine.data.RepeatEngine
import com.reachravi55.mydailyroutine.proto.Task
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object AlarmScheduler {

    private const val EXTRA_TASK_ID = "task_id"
    private const val EXTRA_TITLE = "title"
    private const val EXTRA_DATE = "date"
    private const val EXTRA_HOUR = "hour"
    private const val EXTRA_MIN = "min"

    fun rescheduleTask(context: Context, task: Task, daysAhead: Int = 30) {
        if (task.remindersCount == 0) return
        if (task.startDate.isBlank()) return

        val now = LocalDate.now()
        val end = now.plusDays(daysAhead.toLong())

        val occDates = RepeatEngine.occurrences(task, now, end)
        for (d in occDates) {
            for (r in task.remindersList) {
                if (!r.enabled) continue
                cancelAlarm(context, task.id, d, r.hour, r.minute)
                scheduleAlarm(context, task.id, task.title, d, r.hour, r.minute)
            }
        }
    }

    private fun scheduleAlarm(
        context: Context,
        taskId: String,
        title: String,
        date: LocalDate,
        hour: Int,
        minute: Int
    ) {
        val trigger = LocalDateTime.of(date.year, date.month, date.dayOfMonth, hour, minute)
        val zone = ZoneId.systemDefault()
        val millis = trigger.atZone(zone).toInstant().toEpochMilli()
        if (millis <= System.currentTimeMillis()) return

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = createPendingIntent(context, taskId, date, hour, minute, title)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, millis, pi)
            }
        } catch (se: SecurityException) {
            am.set(AlarmManager.RTC_WAKEUP, millis, pi)
        }
    }

    private fun cancelAlarm(context: Context, taskId: String, date: LocalDate, hour: Int, minute: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = findPendingIntent(context, taskId, date, hour, minute)
        if (pi != null) am.cancel(pi)
    }

    private fun createPendingIntent(
        context: Context,
        taskId: String,
        date: LocalDate,
        hour: Int,
        minute: Int,
        title: String
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_DATE, date.toString())
            putExtra(EXTRA_HOUR, hour)
            putExtra(EXTRA_MIN, minute)
        }
        val requestCode = stableRequestCode(taskId, date.toString(), hour, minute)

        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun findPendingIntent(
        context: Context,
        taskId: String,
        date: LocalDate,
        hour: Int,
        minute: Int
    ): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_DATE, date.toString())
            putExtra(EXTRA_HOUR, hour)
            putExtra(EXTRA_MIN, minute)
        }
        val requestCode = stableRequestCode(taskId, date.toString(), hour, minute)

        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun stableRequestCode(taskId: String, date: String, hour: Int, minute: Int): Int {
        val s = "$taskId|$date|$hour|$minute"
        return s.hashCode()
    }
}
