package com.reachravi55.mydailyroutine.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reachravi55.mydailyroutine.proto.OccurrenceOverride
import com.reachravi55.mydailyroutine.proto.Subtask
import com.reachravi55.mydailyroutine.proto.Task

@Composable
fun TaskOccurrenceCard(
    task: Task,
    dateKey: String,
    override: OccurrenceOverride?,
    onCompletionChange: (Boolean, String) -> Unit,
    onSubtaskChange: (subtaskId: String, completed: Boolean, note: String) -> Unit,
) {
    val done = override?.completed ?: false
    val note = override?.note ?: ""

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = done,
                    onCheckedChange = { onCompletionChange(it, if (!it) "" else note) }
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(task.title, fontWeight = FontWeight.SemiBold)
                    if (task.description.isNotBlank()) {
                        Text(task.description, style = MaterialTheme.typography.bodySmall)
                    }
                }

                if (task.notesEnabled) {
                    IconButton(onClick = { /* keep reserved for future editor */ }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                }
            }

            // Subtasks
            if (task.subtasksCount > 0) {
                Divider()
                Text("Subtasks", style = MaterialTheme.typography.labelLarge)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    task.subtasksList.forEach { st: Subtask ->
                        val ov = override?.subtasksMap?.get(st.id)
                        val stDone = ov?.completed ?: false
                        val stNote = ov?.note ?: ""

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = stDone,
                                    onCheckedChange = { checked ->
                                        onSubtaskChange(st.id, checked, if (!checked) "" else stNote)
                                    }
                                )
                                Text(st.title, modifier = Modifier.weight(1f))
                            }

                            if (task.notesEnabled && stDone) {
                                OutlinedTextField(
                                    value = stNote,
                                    onValueChange = { onSubtaskChange(st.id, true, it) },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Notes") },
                                    placeholder = { Text("What did you do? Any details?") },
                                    singleLine = false,
                                    maxLines = 3,
                                )
                            }
                        }
                    }
                }
            }

            // Notes for main task
            if (task.notesEnabled && done) {
                Divider()
                OutlinedTextField(
                    value = note,
                    onValueChange = { onCompletionChange(done, it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Notes") },
                    placeholder = { Text("Progress notes…") },
                    singleLine = false,
                    maxLines = 4,
                )
            }
        }
    }
}
