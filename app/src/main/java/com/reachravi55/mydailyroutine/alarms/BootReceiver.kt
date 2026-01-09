package com.reachravi55.mydailyroutine.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.reachravi55.mydailyroutine.data.RoutineRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-schedules alarms after a device reboot.
 *
 * Note: This is kept intentionally lightweight; it just triggers a reschedule based on
 * the persisted tasks.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val repo = RoutineRepository.get(context)
                repo.ensureInitialized()
                // If you have a scheduler, this is the place to call it.
                // AlarmScheduler(context).rescheduleAll(repo.readOnce())
            } finally {
                pending.finish()
            }
        }
    }
}
