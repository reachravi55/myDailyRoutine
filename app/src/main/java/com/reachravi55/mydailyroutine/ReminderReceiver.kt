package com.reachravi55.mydailyroutine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        val channelId = "routine_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Routine Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = intent.getStringExtra("title") ?: "Routine Reminder"

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText("Tap to open checklist")
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context).notify(
            System.currentTimeMillis().toInt(),
            notification
        )

        // --- Re-schedule for the next day (for real reminders, not test) ---
        val reminderType = intent.getStringExtra("reminderType")
        val hour = intent.getIntExtra("hour", -1)
        val minute = intent.getIntExtra("minute", -1)

        if (reminderType != null && hour >= 0 && minute >= 0) {
            val requestCode = when (reminderType) {
                "morning" -> 101
                "afternoon" -> 102
                "evening" -> 103
                else -> 999
            }

            if (requestCode != 999) {
                // Reuse the same scheduling helper from MainActivity.kt
                scheduleExactReminder(
                    context = context,
                    hour = hour,
                    minute = minute,
                    requestCode = requestCode,
                    title = title,
                    reminderType = reminderType
                )
            }
        }
    }
}
