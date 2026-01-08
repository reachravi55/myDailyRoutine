package com.reachravi55.mydailyroutine.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reachravi55.mydailyroutine.data.DateUtils
import com.reachravi55.mydailyroutine.data.RepeatEngine
import com.reachravi55.mydailyroutine.proto.OccurrenceOverride
import com.reachravi55.mydailyroutine.proto.RoutineList
import com.reachravi55.mydailyroutine.proto.RoutineStore
import com.reachravi55.mydailyroutine.proto.Task
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    store: RoutineStore,
    contentPadding: PaddingValues,
    onOpenDay: (LocalDate) -> Unit,
    onNewTask: (listId: String, dateKey: String) -> Unit
) {
    val today = LocalDate.now()
    val dateKey = DateUtils.formatKey(today)

    val lists = store.listsList.filterNot { it.archived }.sortedBy { it.sortOrder }
    var filterListId by rememberSaveable { mutableStateOf(store.activeListId) }
    if (filterListId.isBlank() && lists.isNotEmpty()) filterListId = lists.first().id

    val visibleTasks = store.tasksList
        .filter { !it.archived }
        .filter { filterListId.isBlank() || it.listId == filterListId }
        .filter { RepeatEngine.occursOn(it, today) }
        .sortedBy { it.sortOrder }

    val overridesByTask = store.overridesList.filter { it.date == dateKey }.associateBy { it.taskId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Today") },
                actions = {
                    IconButton(onClick = { onOpenDay(today) }) {
                        Icon(Icons.Default.Event, contentDescription = "Open calendar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val listId = if (filterListId.isNotBlank()) filterListId else store.activeListId
                onNewTask(listId, dateKey)
            }) {
                Icon(Icons.Default.Add, contentDescription = "New task")
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .padding(inner)
                .fillMaxSize()
        ) {
            if (lists.isNotEmpty()) {
                ListFilterRow(
                    lists = lists,
                    currentListId = filterListId,
                    onListSelected = { filterListId = it }
                )
            } else {
                SectionHeader("No lists yet")
                Text(
                    "Go to Lists to create your first list.",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(8.dp))

            if (visibleTasks.isEmpty()) {
                EmptyState(
                    title = "Nothing scheduled for today",
                    subtitle = "Tap + to create a task, or schedule a repeating routine."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(visibleTasks, key = { it.id }) { task ->
                        val ov: OccurrenceOverride? = overridesByTask[task.id]
                        Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            TaskOccurrenceCard(
                                task = task,
                                dateKey = dateKey,
                                override = ov,
                                onCompletionChange = { completed, note ->
                                    vm.setCompletion(task.id, dateKey, completed, note)
                                },
                                onSubtaskChange = { subId, completed, note ->
                                    vm.setSubtask(subId, task.id, dateKey, completed, note)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ListFilterRow(
    lists: List<RoutineList>,
    currentListId: String,
    onListSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val current = lists.firstOrNull { it.id == currentListId } ?: lists.first()

    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("List", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = current.name,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                lists.forEach { l ->
                    DropdownMenuItem(
                        text = { Text(l.name) },
                        onClick = {
                            onListSelected(l.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
