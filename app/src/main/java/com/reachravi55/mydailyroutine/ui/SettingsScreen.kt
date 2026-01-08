package com.reachravi55.mydailyroutine.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.reachravi55.mydailyroutine.data.RoutineRepository
import com.reachravi55.mydailyroutine.proto.RoutineStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    store: RoutineStore,
    contentPadding: PaddingValues,
) {
    val ctx = LocalContext.current
    val repo = RoutineRepository.get(ctx)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { inner ->
        Column(
            Modifier
                .padding(contentPadding)
                .padding(inner)
                .fillMaxSize()
        ) {
            SectionHeader("Notifications")

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Enable reminders", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(2.dp))
                    Text("Allows scheduled routine alerts", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = store.settings.notificationsEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch { repo.toggleNotifications(enabled) }
                    }
                )
            }

            SettingsRow(
                title = "Send test notification",
                subtitle = "Verify notifications work on this device"
            ) {
                sendTestNotification(ctx)
                scope.launch { snackbar.showSnackbar("Test notification sent") }
            }

            Divider()

            SectionHeader("Data")
            SettingsRow("Export (coming soon)", "Export your lists & tasks") {
                scope.launch { snackbar.showSnackbar("Export will be added next") }
            }
            SettingsRow("Import (coming soon)", "Import from a backup") {
                scope.launch { snackbar.showSnackbar("Import will be added next") }
            }

            Divider()

            SectionHeader("About")
            SettingsRow("App version", "2.0.0") { }
            SettingsRow("Made for daily routines", "Fast, private, offline-first") { }
        }
    }
}

private fun sendTestNotification(context: Context) {
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

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_popup_reminder)
        .setContentTitle("Test notification")
        .setContentText("If you see this, notifications work.")
        .setAutoCancel(true)
        .build()

    NotificationManagerCompat.from(context).notify(
        (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
        notification
    )
}
