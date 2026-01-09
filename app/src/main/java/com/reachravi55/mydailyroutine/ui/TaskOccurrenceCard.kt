package com.reachravi55.mydailyroutine.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.reachravi55.mydailyroutine.data.RoutineRepository
import com.reachravi55.mydailyroutine.proto.OccurrenceOverride
import com.reachravi55.mydailyroutine.proto.Task
import kotlinx.coroutines.launch

@Composable
fun TaskOccurrenceCard(
    task: Task,
    dateKey: String,
    override: OccurrenceOverride?,
) {
    val ctx = LocalContext.current
    val repo = RoutineRepository.get(ctx)
    val scope = rememberCoroutineScope()

    val completed = override?.completed ?: false
    var note by remember(task.id, dateKey) { mutableStateOf(override?.note ?: "") }

    val subtaskStateMap = remember(override, task.id, dateKey) {
        (override?.subtaskStatesList ?: emptyList()).associateBy { it.subtaskId }
    }

    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = completed,
                    onCheckedChange = { checked ->
                        scope.launch {
                            repo.setOccurrence(
                                taskId = task.id,
                                dateKey = dateKey,
                                completed = checked,
                                note = if (task.notesEnabled) note else null
                            )
                        }
                    }
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(task.title, style = MaterialTheme.typography.titleMedium)
                    if (task.description.isNotBlank()) {
                        Text(task.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (task.subtasksList.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Subtasks", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                task.subtasksList
                    .sortedBy { it.sortOrder }
                    .forEach { sub ->
                        val subCompleted = subtaskStateMap[sub.id]?.completed ?: false
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 4.dp, top = 2.dp, bottom = 2.dp)
                        ) {
                            Checkbox(
                                checked = subCompleted,
                                onCheckedChange = { checked ->
                                    scope.launch {
                                        repo.setSubtaskOccurrence(task.id, dateKey, sub.id, checked, null)
                                    }
                                }
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(sub.title, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
            }

            if (task.notesEnabled && completed) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { new ->
                        note = new
                        scope.launch {
                            repo.setOccurrence(task.id, dateKey, completed, new)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Notes") },
                    maxLines = 3
                )
            }
        }
    }
}
