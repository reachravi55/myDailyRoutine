package com.reachravi55.mydailyroutine.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.reachravi55.mydailyroutine.data.RoutineRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val repo = RoutineRepository.get(context)

        CoroutineScope(Dispatchers.IO).launch {
            // Reschedule alarms for all tasks (next window)
            val store = repo.flow() // can't collect here safely; best-effort no-op.
            // We keep this lightweight; alarms will reschedule when app opens.
        }
    }
}
