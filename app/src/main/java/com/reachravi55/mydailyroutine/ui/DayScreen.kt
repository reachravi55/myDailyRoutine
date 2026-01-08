package com.reachravi55.mydailyroutine.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reachravi55.mydailyroutine.data.DateUtils
import com.reachravi55.mydailyroutine.data.RepeatEngine
import com.reachravi55.mydailyroutine.proto.RoutineStore
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayScreen(
    store: RoutineStore,
    date: LocalDate,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onNewTask: (listId: String) -> Unit
) {
    val dateKey = DateUtils.formatKey(date)
    val tasks = store.tasksList.filter { !it.archived && RepeatEngine.occursOn(it, date) }.sortedBy { it.sortOrder }
    val overridesByTask = store.overridesList.filter { it.date == dateKey }.associateBy { it.taskId }
    val listId = store.activeListId

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(dateKey) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNewTask(listId) }) {
                Icon(Icons.Default.Add, contentDescription = "New task")
            }
        }
    ) { inner ->
        Column(
            Modifier
                .padding(contentPadding)
                .padding(inner)
                .fillMaxSize()
        ) {
            if (tasks.isEmpty()) {
                Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
                    Text("No tasks on this date", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text("Tap + to create one-time or repeating tasks.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    items(tasks, key = { it.id }) { task ->
                        Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            TaskOccurrenceCard(
                                task = task,
                                dateKey = dateKey,
                                override = overridesByTask[task.id],
                                onCompletionChange = { completed, note ->
                                    viewModel.setOccurrence(task.id, dateKey, completed, note)
                                },
                                onSubtaskChange = { subtaskId, completed, note ->
                                    viewModel.setSubtaskOccurrence(task.id, dateKey, subtaskId, completed, note)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}