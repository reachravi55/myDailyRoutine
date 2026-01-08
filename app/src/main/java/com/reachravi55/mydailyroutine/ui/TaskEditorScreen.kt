package com.reachravi55.mydailyroutine.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.reachravi55.mydailyroutine.data.DateUtils
import com.reachravi55.mydailyroutine.data.Ids
import com.reachravi55.mydailyroutine.data.RoutineRepository
import com.reachravi55.mydailyroutine.proto.Reminder
import com.reachravi55.mydailyroutine.proto.RepeatRule
import com.reachravi55.mydailyroutine.proto.RoutineList
import com.reachravi55.mydailyroutine.proto.RoutineStore
import com.reachravi55.mydailyroutine.proto.Task
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorScreen(
    store: RoutineStore,
    initialTaskId: String?,
    initialListId: String,
    initialDateKey: String?,
    onDone: () -> Unit
) {
    val ctx = LocalContext.current
    val repo = RoutineRepository.get(ctx)
    val scope = rememberCoroutineScope()

    val existing = remember(store, initialTaskId) {
        initialTaskId?.let { id -> store.tasksList.firstOrNull { it.id == id } }
    }

    val lists = store.listsList.filterNot { it.archived }.sortedBy { it.sortOrder }

    var title by rememberSaveable { mutableStateOf(existing?.title ?: "") }
    var description by rememberSaveable { mutableStateOf(existing?.description ?: "") }
    var listId by rememberSaveable { mutableStateOf(existing?.listId ?: initialListId) }

    if (listId.isBlank() && lists.isNotEmpty()) listId = lists.first().id

    var startDate by rememberSaveable {
        mutableStateOf(existing?.startDate ?: (initialDateKey ?: DateUtils.todayKey()))
    }

    var notesEnabled by rememberSaveable { mutableStateOf(existing?.notesEnabled ?: true) }

    var freq by rememberSaveable { mutableStateOf(existing?.repeatRule?.frequency ?: RepeatRule.Frequency.DAILY) }
    var interval by rememberSaveable { mutableStateOf((existing?.repeatRule?.interval ?: 1).coerceAtLeast(1)) }

    val reminders = remember { mutableStateListOf<Reminder>() }.apply {
        if (isEmpty()) {
            val base = existing?.remindersList?.toList()
            if (!base.isNullOrEmpty()) addAll(base)
        }
    }

    // If creating brand-new and empty reminders, seed one
    LaunchedEffect(existing?.id) {
        if (existing == null && reminders.isEmpty()) {
            reminders.add(
                Reminder.newBuilder()
                    .setHour(9)
                    .setMinute(0)
                    .setEnabled(true)
                    .setLabel("Morning")
                    .build()
            )
        }
    }

    val canSave =
        title.trim().isNotEmpty() &&
            listId.isNotBlank() &&
            startDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))

    fun doSave() {
        if (!canSave) return
        scope.launch {
            val taskId = existing?.id ?: Ids.id()
            val rr = RepeatRule.newBuilder()
                .setFrequency(freq)
                .setInterval(interval)
                .build()

            val t = Task.newBuilder()
                .setId(taskId)
                .setListId(listId)
                .setTitle(title.trim())
                .setDescription(description.trim())
                .setStartDate(startDate)
                .setRepeatRule(rr)
                .setNotesEnabled(notesEnabled)
                .setSortOrder(existing?.sortOrder ?: (store.tasksCount + 1))
                .setArchived(false)
                .clearReminders()
                .apply { reminders.forEach { addReminders(it) } }
                .build()

            repo.upsertTask(t)
            onDone()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "New task" else "Edit task") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (existing != null) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    repo.deleteTask(existing.id)
                                    onDone()
                                }
                            }
                        ) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                    }
                    IconButton(onClick = { doSave() }, enabled = canSave) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                }
            )
        },
        bottomBar = {
            // Always-visible Save so user never feels “stuck”
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDone,
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel") }
                    Button(
                        onClick = { doSave() },
                        enabled = canSave,
                        modifier = Modifier.weight(1f)
                    ) { Text("Save") }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    reminders.add(
                        Reminder.newBuilder()
                            .setHour(12)
                            .setMinute(0)
                            .setEnabled(true)
                            .setLabel("Reminder")
                            .build()
                    )
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add reminder") }
            )
        }
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Title") },
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Description (optional)") },
                maxLines = 3
            )

            Spacer(Modifier.height(12.dp))

            ListPicker(
                lists = lists,
                currentListId = listId,
                onSelected = { listId = it }
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = startDate,
                onValueChange = { startDate = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Start date (YYYY-MM-DD)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            RepeatPicker(
                freq = freq,
                interval = interval,
                onFreq = { freq = it },
                onInterval = { interval = it.coerceAtLeast(1) }
            )

            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Notes on completion", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(2.dp))
                    Text("Show notes box after checking", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = notesEnabled, onCheckedChange = { notesEnabled = it })
            }

            Spacer(Modifier.height(16.dp))

            Text("Reminders", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            reminders.forEachIndexed { idx, r ->
                ReminderRow(
                    reminder = r,
                    onChange = { reminders[idx] = it },
                    onRemove = { reminders.removeAt(idx) }
                )
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(120.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListPicker(
    lists: List<RoutineList>,
    currentListId: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val current = lists.firstOrNull { it.id == currentListId } ?: lists.firstOrNull()

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = current?.name ?: "Select list",
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            label = { Text("List") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            lists.forEach { l ->
                DropdownMenuItem(
                    text = { Text(l.name) },
                    onClick = {
                        onSelected(l.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepeatPicker(
    freq: RepeatRule.Frequency,
    interval: Int,
    onFreq: (RepeatRule.Frequency) -> Unit,
    onInterval: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val label = when (freq) {
        RepeatRule.Frequency.NONE -> "One-time"
        RepeatRule.Frequency.DAILY -> "Daily"
        RepeatRule.Frequency.WEEKLY -> "Weekly"
        RepeatRule.Frequency.MONTHLY -> "Monthly"
        RepeatRule.Frequency.YEARLY -> "Yearly"
        else -> "Daily"
    }

    Text("Repeat", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = label,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                label = { Text("Frequency") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                listOf(
                    RepeatRule.Frequency.NONE,
                    RepeatRule.Frequency.DAILY,
                    RepeatRule.Frequency.WEEKLY,
                    RepeatRule.Frequency.MONTHLY,
                    RepeatRule.Frequency.YEARLY
                ).forEach { f ->
                    DropdownMenuItem(
                        text = { Text(f.name.lowercase().replaceFirstChar { it.titlecase() }) },
                        onClick = {
                            onFreq(f)
                            expanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = interval.toString(),
            onValueChange = { v -> onInterval(v.toIntOrNull() ?: 1) },
            label = { Text("Every") },
            modifier = Modifier.width(110.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }

    Spacer(Modifier.height(6.dp))
    Text(
        "Example: Every 2 weeks",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ReminderRow(
    reminder: Reminder,
    onChange: (Reminder) -> Unit,
    onRemove: () -> Unit
) {
    Card(elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(reminder.label.ifBlank { "Reminder" }, style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onRemove) { Text("Remove") }
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = reminder.hour.toString().padStart(2, '0'),
                    onValueChange = { v ->
                        val h = v.toIntOrNull()?.coerceIn(0, 23) ?: 0
                        onChange(reminder.toBuilder().setHour(h).build())
                    },
                    label = { Text("HH") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = reminder.minute.toString().padStart(2, '0'),
                    onValueChange = { v ->
                        val m = v.toIntOrNull()?.coerceIn(0, 59) ?: 0
                        onChange(reminder.toBuilder().setMinute(m).build())
                    },
                    label = { Text("MM") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Column(Modifier.weight(1f)) {
                    Text("Enabled", style = MaterialTheme.typography.labelLarge)
                    Switch(
                        checked = reminder.enabled,
                        onCheckedChange = { onChange(reminder.toBuilder().setEnabled(it).build()) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = reminder.label,
                onValueChange = { onChange(reminder.toBuilder().setLabel(it).build()) },
                label = { Text("Label") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
