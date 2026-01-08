package com.example.mydailyroutine.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra("reminderId") ?: return
        val taskTitle = intent.getStringExtra("taskTitle") ?: "Reminder"

        val channelId = NotificationHelper.ensureChannel(context)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Checklist reminder")
            .setContentText("Time to update: $taskTitle")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(reminderId.hashCode(), notification)

        // TODO: load reminder from DB and schedule next alarm if recurrence is enabled
    }
}