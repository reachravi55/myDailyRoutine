package com.reachravi55.mydailyroutine.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reachravi55.mydailyroutine.proto.RoutineStore

@Composable
fun SettingsScreen(store: RoutineStore, contentPadding: PaddingValues) {
    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))
        ListCard(title = "Local storage", subtitle = "This version stores everything on-device (DataStore Proto).") {}
        Spacer(Modifier.height(8.dp))
        ListCard(title = "Reminders", subtitle = "Reminders use system alarms + notifications.") {}
        Spacer(Modifier.height(8.dp))
        ListCard(title = "Next", subtitle = "We can add backup/export, widgets, and cloud sync later.") {}
    }
}
