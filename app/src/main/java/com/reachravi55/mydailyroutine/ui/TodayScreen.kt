package com.reachravi55.mydailyroutine.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reachravi55.mydailyroutine.data.RoutineRepository
import com.reachravi55.mydailyroutine.proto.OccurrenceOverride
import com.reachravi55.mydailyroutine.proto.RoutineStore
import java.time.LocalDate

@Composable
fun TodayScreen(
    store: RoutineStore,
    repo: RoutineRepository,
    contentPadding: PaddingValues,
    onOpenDay: (LocalDate) -> Unit,
    onNewTask: (String?) -> Unit,
    onEditTask: (String) -> Unit,
) {
    val today = remember { LocalDate.now() }

    // Pick an active list (or first list) so the user can immediately create tasks.
    val activeListId = remember(store.activeListId, store.listsCount) {
        when {
            store.activeListId.isNotBlank() -> store.activeListId
            store.listsCount > 0 -> store.listsList.first().id
            else -> ""
        }
    }

    val todaysOccurrences = remember(store.tasksList, store.overridesList, today) {
        val iso = today.toString()
        val overridesByTaskId = store.overridesList.associateBy { it.taskId }
        // Very simple: show tasks from the active list that are not archived.
        store.tasksList
            .filter { it.listId == activeListId && !it.archived }
            .sortedBy { it.sortOrder }
            .map { t ->
                t to (overridesByTaskId[t.id]?.takeIf { it.dateIso == iso })
            }
    }

    Scaffold(
        modifier = Modifier.padding(contentPadding),
        topBar = {
            TopAppBar(
                title = { Text("Today") },
                actions = {
                    TextButton(onClick = { onOpenDay(today) }) {
                        Text("Open Day")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNewTask(activeListId.ifBlank { null }) },
                enabled = store.listsCount > 0,
            ) {
                Text("+")
            }
        }
    ) { inner ->
        if (store.listsCount == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "No lists yet",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Create a list in the Lists tab first, then you can add tasks.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            return@Scaffold
        }

        if (todaysOccurrences.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "No tasks for today",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tap + to create one.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(todaysOccurrences, key = { it.first.id }) { (task, override) ->
                TaskOccurrenceCard(
                    task = task,
                    override = override,
                    onToggleCompleted = { checked ->
                        repo.setTaskCompletedForDate(taskId = task.id, date = today, completed = checked)
                    },
                    onToggleSubtask = { subtaskId, checked ->
                        repo.setSubtaskCompletedForDate(
                            taskId = task.id,
                            date = today,
                            subtaskId = subtaskId,
                            completed = checked,
                        )
                    },
                    onEdit = { onEditTask(task.id) },
                )
            }
        }
    }
}
