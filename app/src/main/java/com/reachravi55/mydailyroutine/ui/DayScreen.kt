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
import com.reachravi55.mydailyroutine.proto.RoutineStore
import java.time.LocalDate

@Composable
fun DayScreen(
    date: LocalDate,
    store: RoutineStore,
    repo: RoutineRepository,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onEditTask: (String) -> Unit,
) {
    val iso = remember(date) { date.toString() }
    val overridesByTaskId = remember(store.overridesList, iso) {
        store.overridesList
            .filter { it.dateIso == iso }
            .associateBy { it.taskId }
    }

    val tasks = remember(store.tasksList, store.activeListId) {
        val listId = store.activeListId.ifBlank { store.listsList.firstOrNull()?.id.orEmpty() }
        store.tasksList
            .filter { it.listId == listId && !it.archived }
            .sortedBy { it.sortOrder }
    }

    Scaffold(
        modifier = Modifier.padding(contentPadding),
        topBar = {
            TopAppBar(
                title = { Text(date.toString()) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("←") }
                },
            )
        }
    ) { inner ->
        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("No tasks")
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
            items(tasks, key = { it.id }) { task ->
                TaskOccurrenceCard(
                    task = task,
                    override = overridesByTaskId[task.id],
                    onToggleCompleted = { checked ->
                        repo.setTaskCompletedForDate(taskId = task.id, date = date, completed = checked)
                    },
                    onToggleSubtask = { subtaskId, checked ->
                        repo.setSubtaskCompletedForDate(
                            taskId = task.id,
                            date = date,
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
