package com.reachravi55.mydailyroutine.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.reachravi55.mydailyroutine.data.DateUtils
import com.reachravi55.mydailyroutine.proto.RoutineStore
import java.time.LocalDate

@Composable
fun TodayScreen(
    store: RoutineStore,
    contentPadding: PaddingValues,
    onOpenDay: (LocalDate) -> Unit,
    onNewTask: (listId: String, dateKey: String) -> Unit
) {
    val ctx = LocalContext.current
    val today = remember { LocalDate.now() }
    val todayKey = remember { DateUtils.formatKey(today) }

    val listsById = remember(store) { store.listsList.associateBy { it.id } }

    // "Active today" view (simple for now):
    // - tasks that have started on/before today
    // - not archived
    // NOTE: Repeat rules can be enhanced later; this keeps Today working + useful.
    val tasksToday = remember(store, todayKey) {
        store.tasksList
            .asSequence()
            .filter { !it.archived }
            .filter { it.startDate.isBlank() || it.startDate <= todayKey }
            .sortedBy { it.sortOrder }
            .toList()
    }

    fun shareToday() {
        val sb = StringBuilder()
        sb.append("My Daily Routine — ").append(todayKey).append("\n\n")

        if (tasksToday.isEmpty()) {
            sb.append("No tasks scheduled for today.")
        } else {
            tasksToday.forEach { t ->
                val listName = listsById[t.listId]?.name ?: "List"
                sb.append("• ").append(t.title).append(" (").append(listName).append(")\n")
                if (t.description.isNotBlank()) {
                    sb.append("  - ").append(t.description.trim()).append("\n")
                }
                sb.append("\n")
            }
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, sb.toString().trim())
        }
        val chooser = Intent.createChooser(sendIntent, "Share Today")
        ContextCompat.startActivity(ctx, chooser, null)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Today") },
                actions = {
                    IconButton(onClick = { onOpenDay(today) }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Open Today")
                    }
                    IconButton(onClick = { shareToday() }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    val listId = store.activeListId.ifBlank { store.listsList.firstOrNull()?.id ?: "" }
                    onNewTask(listId, todayKey)
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New task") }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(contentPadding)   // <- space for bottom nav
                .padding(inner)            // <- space for top bar/fab
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Today",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        todayKey,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { onOpenDay(today) }) {
                    Text("Open day")
                }
            }

            Divider()

            if (tasksToday.isEmpty()) {
                EmptyTodayState(
                    dateKey = todayKey,
                    onCreate = {
                        val listId = store.activeListId.ifBlank { store.listsList.firstOrNull()?.id ?: "" }
                        onNewTask(listId, todayKey)
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "Tasks",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Tap “Open day” to check off items and add notes for today.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    items(tasksToday, key = { it.id }) { task ->
                        TaskCard(
                            title = task.title,
                            subtitle = listsById[task.listId]?.name ?: "List",
                            description = task.description
                        )
                    }

                    item { Spacer(Modifier.height(90.dp)) }
                }
            }
        }
    }
}

@Composable
private fun EmptyTodayState(
    dateKey: String,
    onCreate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Nothing scheduled for today",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Create a task for $dateKey or assign a repeating checklist.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onCreate) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Create task")
        }
    }
}

@Composable
private fun TaskCard(
    title: String,
    subtitle: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (description.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
