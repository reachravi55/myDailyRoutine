package com.reachravi55.mydailyroutine.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.reachravi55.mydailyroutine.alarms.AlarmScheduler
import com.reachravi55.mydailyroutine.data.RoutineRepository
import com.reachravi55.mydailyroutine.data.newId
import com.reachravi55.mydailyroutine.proto.*
import java.time.LocalDate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorScreen(store: RoutineStore, taskId: String?, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val repo = RoutineRepository.get(ctx)
    val scope = rememberCoroutineScope()

    val existing = store.tasksList.firstOrNull { it.id == taskId }

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var desc by remember { mutableStateOf(existing?.description ?: "") }

    val lists = store.listsList.sortedBy { it.sortOrder }
    var listId by remember { mutableStateOf(existing?.listId ?: (store.settings.activeListId.takeIf { it.isNotBlank() } ?: lists.firstOrNull()?.id ?: "")) }

    // Scheduling
    var startDate by remember { mutableStateOf(existing?.startDate?.takeIf { it.isNotBlank() } ?: LocalDate.now().toString()) }

    var freq by remember { mutableStateOf(existing?.repeat?.frequency ?: RepeatRule.Frequency.DAILY) }
    var interval by remember { mutableStateOf((existing?.repeat?.interval ?: 1).coerceAtLeast(1).toString()) }

    // Weekly weekdays
    val weekDaysAll = listOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun")
    val selectedWeekdays = remember { mutableStateListOf<Int>() }
    LaunchedEffect(existing?.repeat?.weekdaysList) {
        selectedWeekdays.clear()
        existing?.repeat?.weekdaysList?.forEach { selectedWeekdays.add(it) }
        if (selectedWeekdays.isEmpty() && freq == RepeatRule.Frequency.WEEKLY) selectedWeekdays.add(LocalDate.parse(startDate).dayOfWeek.value)
    }

    // Reminders (time list)
    val reminders = remember { mutableStateListOf<Reminder>() }
    LaunchedEffect(existing?.remindersList) {
        reminders.clear()
        existing?.remindersList?.forEach { reminders.add(it) }
        if (reminders.isEmpty()) {
            reminders.add(Reminder.newBuilder().setHour(8).setMinute(0).setEnabled(true).build())
        }
    }
    var newHour by remember { mutableStateOf("8") }
    var newMin by remember { mutableStateOf("0") }

    // Subtasks
    val subtasks = remember { mutableStateListOf<Subtask>() }
    LaunchedEffect(existing?.subtasksList) {
        subtasks.clear()
        existing?.subtasksList?.forEach { subtasks.add(it) }
    }
    var newSub by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "New Task" else "Edit Task") },
                navigationIcon = { }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(Modifier.height(16.dp))
            }

            item {
                Text("List", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                if (lists.isEmpty()) {
                    Text("Create a list first (Lists tab).", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    ExposedDropdownMenuBox(expanded = false, onExpandedChange = {}) { /* placeholder */ }
                    // simple buttons instead of dropdown (keeps code compact)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(lists) { l ->
                            FilterChip(selected = listId == l.id, onClick = { listId = l.id }, label = { Text(l.name) })
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            item {
                Text("Schedule", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Start date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FrequencyDropdown(freq = freq, onChange = { freq = it }, modifier = Modifier.weight(1f))
                    OutlinedTextField(
                        value = interval,
                        onValueChange = { interval = it.filter { ch -> ch.isDigit() }.take(3) },
                        label = { Text("Every") },
                        singleLine = true,
                        modifier = Modifier.width(110.dp)
                    )
                }

                if (freq == RepeatRule.Frequency.WEEKLY) {
                    Spacer(Modifier.height(10.dp))
                    Text("Repeat on", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(weekDaysAll) { (v, label) ->
                            FilterChip(selected = selectedWeekdays.contains(v), onClick = {
                                if (selectedWeekdays.contains(v)) selectedWeekdays.remove(v) else selectedWeekdays.add(v)
                            }, label = { Text(label) })
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            item {
                Text("Reminders", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                reminders.forEachIndexed { idx, r ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(String.format("%02d:%02d", r.hour, r.minute))
                        Row {
                            Switch(checked = r.enabled, onCheckedChange = {
                                reminders[idx] = r.toBuilder().setEnabled(it).build()
                            })
                            TextButton(onClick = { reminders.removeAt(idx) }) { Text("Remove") }
                        }
                    }
                    Divider()
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = newHour, onValueChange = { newHour = it.filter { c -> c.isDigit() }.take(2) }, label = { Text("Hour") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = newMin, onValueChange = { newMin = it.filter { c -> c.isDigit() }.take(2) }, label = { Text("Min") }, singleLine = true, modifier = Modifier.weight(1f))
                    Button(onClick = {
                        val h = newHour.toIntOrNull()?.coerceIn(0,23) ?: 8
                        val m = newMin.toIntOrNull()?.coerceIn(0,59) ?: 0
                        reminders.add(Reminder.newBuilder().setHour(h).setMinute(m).setEnabled(true).build())
                    }) { Text("Add") }
                }
                Spacer(Modifier.height(16.dp))
            }

            item {
                Text("Checklist items", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                subtasks.sortedBy { it.sortOrder }.forEachIndexed { idx, s ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(s.title, style = MaterialTheme.typography.bodyLarge)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                    Text("Notes")
                                    Spacer(Modifier.width(8.dp))
                                    Switch(checked = s.notesEnabled, onCheckedChange = {
                                        subtasks[idx] = s.toBuilder().setNotesEnabled(it).build()
                                    })
                                }
                                TextButton(onClick = { subtasks.removeAt(idx) }) { Text("Delete") }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newSub,
                        onValueChange = { newSub = it },
                        label = { Text("New item") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(onClick = {
                        val text = newSub.trim()
                        if (text.isNotBlank()) {
                            subtasks.add(
                                Subtask.newBuilder()
                                    .setId(newId())
                                    .setTitle(text)
                                    .setNotesEnabled(true)
                                    .setSortOrder(subtasks.size)
                                    .build()
                            )
                            newSub = ""
                        }
                    }) { Text("Add") }
                }
                Spacer(Modifier.height(24.dp))
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onDone) { Text("Cancel") }
                    Button(onClick = {
                        val task = (existing?.toBuilder() ?: Task.newBuilder().setId(newId())).apply {
                            setTitle(title.trim().ifBlank { "Untitled task" })
                            setDescription(desc.trim())
                            setListId(listId)
                            setStartDate(startDate.trim())
                            setRepeat(
                                RepeatRule.newBuilder()
                                    .setFrequency(freq)
                                    .setInterval(interval.toIntOrNull()?.coerceAtLeast(1) ?: 1)
                                    .apply {
                                        if (freq == RepeatRule.Frequency.WEEKLY) {
                                            clearWeekdays()
                                            selectedWeekdays.distinct().sorted().forEach { addWeekdays(it) }
                                        }
                                    }
                                    .build()
                            )
                            clearReminders()
                            reminders.forEach { addReminders(it) }
                            clearSubtasks()
                            subtasks.sortedBy { it.sortOrder }.forEach { addSubtasks(it) }
                        }.build()

                        // Save + schedule
                        scope.launch {
                            repo.upsertTask(task)
                            AlarmScheduler.cancelTaskWindow(ctx, task, 30)
                            AlarmScheduler.rescheduleTask(ctx, task, 30)
                        }
                        onDone()
                    }) { Text("Save") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FrequencyDropdown(freq: RepeatRule.Frequency, onChange: (RepeatRule.Frequency) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        RepeatRule.Frequency.NONE to "One-time",
        RepeatRule.Frequency.DAILY to "Daily",
        RepeatRule.Frequency.WEEKLY to "Weekly",
        RepeatRule.Frequency.MONTHLY to "Monthly",
        RepeatRule.Frequency.YEARLY to "Yearly"
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = options.first { it.first == freq }.second,
            onValueChange = {},
            readOnly = true,
            label = { Text("Repeat") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onChange(value)
                        expanded = false
                    }
                )
            }
        }
    }
}