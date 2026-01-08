package com.example.mydailyroutine.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Launch a coroutine to read DB and reschedule active reminders
            CoroutineScope(Dispatchers.IO).launch {
                // TODO: initialize DB, query active reminders and schedule next alarm for each
            }
        }
    }
}