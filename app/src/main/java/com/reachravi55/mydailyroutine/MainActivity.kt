package com.reachravi55.mydailyroutine

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.util.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        scheduleReminders()

        setContent {
            ChecklistUI()
        }
    }

    private fun scheduleReminders() {
        schedule(7, 0, 101, "Morning Routine")
        schedule(12, 0, 102, "Afternoon Routine")
        schedule(19, 0, 103, "Evening Routine")
    }

    private fun schedule(h: Int, m: Int, code: Int, title: String) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val i = Intent(this, ReminderReceiver::class.java).apply {
            putExtra("title", title)
        }

        val pending = PendingIntent.getBroadcast(
            this, code, i,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val c = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            c.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pending
        )
    }
}

@Composable
fun ChecklistUI() {
    var done by remember { mutableStateOf(false) }

    Column(Modifier.padding(20.dp)) {
        Text("Daily Routine", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(20.dp))
        Row {
            Checkbox(done, { done = it })
            Text("Mark routine complete")
        }
    }
}