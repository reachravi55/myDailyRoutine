package com.reachravi55.mydailyroutine.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reachravi55.mydailyroutine.data.RoutineRepository
import com.reachravi55.mydailyroutine.proto.RoutineStore

@Composable
fun SettingsScreen(
    store: RoutineStore,
    repo: RoutineRepository,
    contentPadding: PaddingValues,
) {
    val settings = store.settings

    Scaffold(
        modifier = Modifier.padding(contentPadding),
        topBar = {
            TopAppBar(title = { Text("Settings") })
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Notifications")
                    Text(
                        if (settings.notificationsEnabled) "Enabled" else "Disabled",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = settings.notificationsEnabled,
                    onCheckedChange = { enabled -> repo.toggleNotifications(enabled) }
                )
            }

            HorizontalDivider()

            Text(
                "Theme",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "This project currently uses the system theme. (You can add Light/Dark toggles later.)",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
