package com.reachravi55.mydailyroutine.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.reachravi55.mydailyroutine.proto.Subtask
import com.reachravi55.mydailyroutine.proto.SubtaskState
import com.reachravi55.mydailyroutine.proto.Task
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskOccurrenceCard(
    task: Task,
    date: LocalDate,
    completed: Boolean,
    note: String,
    subtaskStates: List<SubtaskState>,
    onToggleComplete: (Boolean) -> Unit,
    onNoteChange: (String) -> Unit,
    onSubtaskToggle: (String, Boolean) -> Unit,
    onSubtaskNoteChange: (String, String) -> Unit,
    onRescheduleReminders: () -> Unit
) {
    val subStateMap = remember(subtaskStates) { subtaskStates.associateBy { it.subtaskId } }

    var expanded by remember { mutableStateOf(false) }
    var localNote by remember(note) { mutableStateOf(TextFieldValue(note)) }
    var dirty by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        onClick = { expanded = !expanded }
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier.weight(1f)) {
                    Checkbox(
                        checked = completed,
                        onCheckedChange = {
                            onToggleComplete(it)
                            expanded = it // open when completed to show notes/progress
                        }
                    )
                    Column(Modifier.padding(top = 6.dp)) {
                        Text(task.title, style = MaterialTheme.typography.titleMedium)
                        val desc = task.description.trim()
                        if (desc.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                TextButton(onClick = onRescheduleReminders) { Text("Sync") }
            }

            if (expanded) {
                Spacer(Modifier.height(12.dp))
                // Task note
                OutlinedTextField(
                    value = localNote,
                    onValueChange = {
                        localNote = it
                        dirty = true
                    },
                    label = { Text("Progress notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        enabled = dirty,
                        onClick = {
                            onNoteChange(localNote.text)
                            dirty = false
                        }
                    ) { Text("Save notes") }
                }

                if (task.subtasksCount > 0) {
                    Spacer(Modifier.height(6.dp))
                    Text("Checklist", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    task.subtasksList.sortedBy { it.sortOrder }.forEach { sub ->
                        SubtaskRow(
                            sub = sub,
                            state = subStateMap[sub.id],
                            onToggle = { checked -> onSubtaskToggle(sub.id, checked) },
                            onSaveNote = { n -> onSubtaskNoteChange(sub.id, n) }
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SubtaskRow(
    sub: Subtask,
    state: SubtaskState?,
    onToggle: (Boolean) -> Unit,
    onSaveNote: (String) -> Unit
) {
    var localNote by remember(state?.note) { mutableStateOf(TextFieldValue(state?.note ?: "")) }
    var dirty by remember { mutableStateOf(false) }

    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.weight(1f)) {
                Checkbox(
                    checked = state?.completed == true,
                    onCheckedChange = {
                        onToggle(it)
                    }
                )
                Text(sub.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 12.dp))
            }
        }
        if (sub.notesEnabled) {
            OutlinedTextField(
                value = localNote,
                onValueChange = {
                    localNote = it
                    dirty = true
                },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 1
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    enabled = dirty,
                    onClick = {
                        onSaveNote(localNote.text)
                        dirty = false
                    }
                ) { Text("Save") }
            }
        }
    }
}
