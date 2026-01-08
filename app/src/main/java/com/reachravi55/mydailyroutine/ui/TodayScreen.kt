package com.reachravi55.mydailyroutine.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.reachravi55.mydailyroutine.alarms.AlarmScheduler
import com.reachravi55.mydailyroutine.data.RepeatEngine
import com.reachravi55.mydailyroutine.data.RoutineRepository
import com.reachravi55.mydailyroutine.data.key
import com.reachravi55.mydailyroutine.proto.RoutineStore
import com.reachravi55.mydailyroutine.proto.Task
import java.time.LocalDate

@Composable
fun TodayScreen(store: RoutineStore, date: LocalDate, contentPadding: PaddingValues) {
    val ctx = LocalContext.current
    val repo = RoutineRepository.get(ctx)
    val scope = rememberCoroutineScope()

    val tasks = store.tasksList.filter { !it.archived }
    val overrides = store.overridesList.filter { it.date == date.key() }.associateBy { it.taskId }

    val occ = tasks.flatMap { t ->
        val dates = RepeatEngine.occurrences(t, date, date)
        if (dates.isNotEmpty()) listOf(t) else emptyList()
    }.sortedBy { it.title.lowercase() }

    val shareText = remember(store, date) {
        buildShareText(store, date)
    }

    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Today", style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = {
                Share.shareText(ctx, shareText)
            }) {
                Icon(Icons.Default.Share, contentDescription = "Share")
            }
        }

        if (occ.isEmpty()) {
            EmptyStateCard(
                title = "No tasks scheduled",
                subtitle = "Tap New to create your first checklist or reminder."
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                items(occ, key = { it.id }) { task ->
                    val o = overrides[task.id]
                    TaskOccurrenceCard(
                        task = task,
                        date = date,
                        completed = o?.completed == true,
                        note = o?.note ?: "",
                        subtaskStates = o?.subtaskStatesList ?: emptyList(),
                        onToggleComplete = { checked ->
                            scope.launch { repo.setOccurrence(task.id, date.key(), checked, null) }
                        },
                        onNoteChange = { note ->
                            scope.launch { repo.setOccurrence(task.id, date.key(), o?.completed == true, note) }
                        },
                        onSubtaskToggle = { subId, checked ->
                            scope.launch { repo.setSubtaskState(task.id, date.key(), subId, checked, null) }
                        },
                        onSubtaskNoteChange = { subId, note ->
                            scope.launch { repo.setSubtaskState(task.id, date.key(), subId, true, note) }
                        },
                        onRescheduleReminders = {
                            AlarmScheduler.rescheduleTask(ctx, task)
                        }
                    )
                }
            }
        }
    }
}

private fun buildShareText(store: RoutineStore, date: LocalDate): String {
    val dateKey = date.key()
    val tasks = store.tasksList.filter { !it.archived }
    val overrides = store.overridesList.filter { it.date == dateKey }.associateBy { it.taskId }

    val occ = tasks.filter { RepeatEngine.occurrences(it, date, date).isNotEmpty() }
        .sortedBy { it.title.lowercase() }

    val sb = StringBuilder()
    sb.append("My Daily Routine — ").append(dateKey).append("\n\n")
    for (t in occ) {
        val o = overrides[t.id]
        val mark = if (o?.completed == true) "✅" else "⬜"
        sb.append(mark).append(" ").append(t.title).append("\n")
        val note = (o?.note ?: "").trim()
        if (note.isNotBlank()) sb.append("   • Note: ").append(note).append("\n")
        // subtasks
        if (t.subtasksCount > 0) {
            val subStates = (o?.subtaskStatesList ?: emptyList()).associateBy { it.subtaskId }
            t.subtasksList.sortedBy { it.sortOrder }.forEach { s ->
                val st = subStates[s.id]
                val smark = if (st?.completed == true) "✅" else "⬜"
                sb.append("   ").append(smark).append(" ").append(s.title).append("\n")
                val sn = (st?.note ?: "").trim()
                if (sn.isNotBlank()) sb.append("      • ").append(sn).append("\n")
            }
        }
        sb.append("\n")
    }
    return sb.toString().trim()
}

object Share {
    fun shareText(context: android.content.Context, text: String) {
        val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, text)
        }
        context.startActivity(android.content.Intent.createChooser(send, "Share"))
    }
}
