package com.reachravi55.mydailyroutine.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationHelper {
    private const val CHANNEL_ID = "mydailyroutine_reminders"
    fun ensureChannel(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)
            val ch = NotificationChannel(CHANNEL_ID, "Reminders", NotificationManager.IMPORTANCE_HIGH)
            ch.description = "Notifications for checklist reminders"
            nm?.createNotificationChannel(ch)
        }
        return CHANNEL_ID
    }
}