package com.reachravi55.mydailyroutine.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.reachravi55.mydailyroutine.data.DateUtils
import com.reachravi55.mydailyroutine.data.RoutineRepository
import com.reachravi55.mydailyroutine.proto.RoutineStore
import com.reachravi55.mydailyroutine.proto.Task
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    store: RoutineStore,
    onNewTask: (dateKey: String) -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenLists: () -> Unit,
    onShareToday: (String) -> Unit
) {
    val ctx = LocalContext.current
    val repo = RoutineRepository.get(ctx)
    val scope = rememberCoroutineScope()

    val todayKey = remember { DateUtils.todayKey() }

    // Tasks "active" for today:
    // - startDate <= today
    // - not archived
    // - and either one-time on today OR repeating (we include repeating tasks always once started)
    val todayTasks = remember(store, todayKey) {
        store.tasksList
            .asSequence()
            .filter { !it.archived }
            .filter { it.startDate <= todayKey }
            .sortedBy { it.sortOrder }
            .toList()
    }

    val listsById = remember(store) {
        store.listsList.associateBy { it.id }
    }

    fun buildShareText(): String {
        val sb = StringBuilder()
        sb.append("My Daily Routine — ").append(todayKey).append("\n\n")
        if (todayTasks.isEmpty()) {
            sb.append("No tasks scheduled for today.\n")
        } else {
            todayTasks.forEach { t ->
                val listName = listsById[t.listId]?.name ?: "List"
                sb.append("• ").append(t.title)
                sb.append("  (").append(listName).append(")")
                sb.append("\n")
                if (t.description.isNotBlank()) {
                    sb.append("  - ").append(t.description.trim()).append("\n")
                }
                sb.append("\n")
            }
        }
        return sb.toString().trim()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Today") },
                actions = {
                    IconButton(onClick = { onShareToday(buildShareText()) }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNewTask(todayKey) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New task") }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
        ) {

            // Quick actions row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenCalendar,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Calendar")
                }
                OutlinedButton(
                    onClick = onOpenLists,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ListAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Lists")
                }
            }

            Divider()

            if (todayTasks.isEmpty()) {
                EmptyStateToday(
                    dateKey = todayKey,
                    onCreate = { onNewTask(todayKey) }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "Scheduled tasks",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Tap a task to edit it from Calendar. Use Share to send today’s plan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    items(todayTasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            listName = listsById[task.listId]?.name ?: "List",
                            onToggleDone = {
                                // V2 store doesn’t yet have per-day completion state in proto.
                                // For now, we just show a “checked” UX signal via a snackbar.
                                scope.launch {
                                    repo.showSnackbar(ctx, "Marked “${task.title}” (completion tracking coming next).")
                                }
                            }
                        )
                    }

                    item { Spacer(Modifier.height(90.dp)) }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateToday(
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
    task: Task,
    listName: String,
    onToggleDone: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(
                        task.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        listName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onToggleDone) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Mark done")
                }
            }

            if (task.description.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
